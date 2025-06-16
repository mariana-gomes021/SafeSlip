/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controlador.ProcessadoresGUI;

import controlador.ProcessadoresGUI.ProcessamentoCancelado;
import usuario.Boleto; // Importe sua classe Boleto
import usuario.Usuario; // Importe sua classe Usuario
import bancodedados.RepositorioBoleto;
import bancodedados.RepositorioUsuario;
import bancodedados.RepositorioCnpjReputacao;
import verificacao.ConsultaCNPJ;
import verificacao.ConsultaBanco;
import boleto.extracao.ExtracaoBoleto; // Ajuste o pacote se ExtracaoBoleto estiver em outro lugar
import boleto.ValidadorLinhaDigitavel; // Ajuste o pacote se ValidadorLinhaDigitavel estiver em outro lugar

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.sql.Date;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

// Esta classe conterá a lógica de processamento do boleto, separada da GUI.
// Ela não fará perguntas ao usuário, mas receberá as respostas como parâmetros.
public class ProcessadorBoletoGui {

    private ExtracaoBoleto extracaoBoleto;
    private RepositorioBoleto repositorioBoleto;
    private RepositorioUsuario repositorioUsuario;
    private RepositorioCnpjReputacao repositorioCnpjReputacao;

    private File arquivoTxtGerado; // Para gerenciar o arquivo TXT gerado pela extração
    private int verificacoesComFalha; // Contador para rastrear falhas sem interromper

    public ProcessadorBoletoGui() {
        this.extracaoBoleto = new ExtracaoBoleto();
        this.repositorioBoleto = new RepositorioBoleto();
        this.repositorioUsuario = new RepositorioUsuario();
        this.repositorioCnpjReputacao = new RepositorioCnpjReputacao();
        this.verificacoesComFalha = 0; // Inicializa o contador de falhas
    }

    /**
     * Processa um boleto a partir de um arquivo PDF, interagindo com o usuário para confirmações.
     * @param pdfFile O arquivo PDF do boleto.
     * @return O objeto Boleto processado com todos os seus status.
     * @throws IOException Se houver erro de leitura do PDF ou manipulação de arquivos.
     * @throws SQLException Se houver erro de acesso ao banco de dados.
     * @throws ProcessamentoCancelado Se o usuário cancelar alguma etapa.
     */
    public Boleto processarBoleto(File pdfFile) throws IOException, SQLException, ProcessamentoCancelado {
        verificacoesComFalha = 0; // Reseta o contador para cada novo processamento

        // 1. Extração do Boleto
        extracaoBoleto.setCaminhoToArquivo(pdfFile);
        this.arquivoTxtGerado = extracaoBoleto.getArquivoTxtGerado(pdfFile);
        Boleto boletoExtraido = extracaoBoleto.processarTxt();

        boolean extracaoMinimaBemSucedida =
            boletoExtraido != null &&
            boletoExtraido.getCodigoBarras() != null &&
            !boletoExtraido.getCodigoBarras().isEmpty() &&
            boletoExtraido.getCnpjEmitente() != null &&
            !boletoExtraido.getCnpjEmitente().isEmpty();

        if (!extracaoMinimaBemSucedida) {
            apagarArquivoTxtGerado();
            throw new IllegalArgumentException("Não foi possível extrair informações essenciais do boleto (código de barras ou CNPJ). Verifique o arquivo.");
        }

        return boletoExtraido;
    }

    /**
     * Continua o processamento do boleto após a confirmação inicial do usuário e entrada de valor.
     * Todas as validações são executadas e seus resultados são registrados no objeto Boleto.
     * @param boletoExtraido O objeto Boleto com os dados extraídos e a flag de confirmação do usuário.
     * @param valorSemDescontoInformado O valor que o usuário informou como original (sem descontos).
     * @throws SQLException Se houver erro de acesso ao banco de dados.
     */
    public void continuarProcessamento(Boleto boletoExtraido, BigDecimal valorSemDescontoInformado) throws SQLException {

        // 3. Verificação do Valor
        String valorVerificacaoStatus = verificarValorBoleto(boletoExtraido.getCodigoBarras(), valorSemDescontoInformado);
        if ("VALOR_DIVERGENTE".equals(valorVerificacaoStatus)) {
            // boletoExtraido.setMensagemAlerta("ALERTA: Valor informado difere do valor no código de barras."); // Remove esta linha
            boletoExtraido.addDetalheFalha("ALERTA: O valor informado (" + valorSemDescontoInformado + ") difere do valor detectado no código de barras."); // Adiciona aqui
            verificacoesComFalha++;
        } else if ("ERRO_EXTRACAO_VALOR_CB".equals(valorVerificacaoStatus)) {
            boletoExtraido.addDetalheFalha("ALERTA: Não foi possível extrair ou verificar o valor do código de barras.");
            verificacoesComFalha++;
        } else if ("DADOS_INCOMPLETOS".equals(valorVerificacaoStatus)) {
            boletoExtraido.addDetalheFalha("ALERTA: Dados incompletos para verificação de valor do código de barras.");
            verificacoesComFalha++;
        }


        // 4. Validação Detalhada do Código de Barras
        boolean codigoBarrasEstruturaValida = ValidadorLinhaDigitavel.validar(boletoExtraido.getCodigoBarras());
        if (!codigoBarrasEstruturaValida) {
            boletoExtraido.setStatusValidacao("INVALIDO_ESTRUTURA_CB");
            boletoExtraido.addDetalheFalha("Falha: Estrutura do Código de Barras inválida.");
            verificacoesComFalha++;
        } else {
            // Se já não houver um status de invalidez pior, define como válido para seguir
            if (!"INVALIDO_ESTRUTURA_CB".equals(boletoExtraido.getStatusValidacao()) &&
                !"INVALIDO".equals(boletoExtraido.getStatusValidacao())) {
                boletoExtraido.setStatusValidacao("VALIDO_INICIAL"); // Estado inicial OK
            }
        }

        // 5. Inserir ou Atualizar CNPJ Emitente
        String cnpjEmitente = boletoExtraido.getCnpjEmitente();
        if (cnpjEmitente != null && !cnpjEmitente.isEmpty()) {
            try {
                inserirOuAtualizarCnpjEmitente(cnpjEmitente, boletoExtraido.getNomeBeneficiario());
            } catch (SQLException e) {
                boletoExtraido.addDetalheFalha("Falha: Erro ao inserir/atualizar CNPJ Emitente no BD: " + e.getMessage());
                verificacoesComFalha++;
            }
        } else {
            boletoExtraido.addDetalheFalha("Alerta: CNPJ do emitente não extraído ou inválido do PDF.");
            if (!"INVALIDO".equals(boletoExtraido.getStatusValidacao())) { // Não sobrescreve erro de estrutura grave
                 boletoExtraido.setStatusValidacao("CNPJ_NAO_EXTRAIDO");
            }
            verificacoesComFalha++;
        }

        // 6. Verificação de CNPJ com API
        ConsultaCNPJ consultaCnpj = new ConsultaCNPJ(boletoExtraido);
        String statusValidacaoCNPJAPI = consultaCnpj.validarDadosComApi();
        if (statusValidacaoCNPJAPI.startsWith("ALERTA") || statusValidacaoCNPJAPI.startsWith("INVALIDO")) {
            if (!"INVALIDO_ESTRUTURA_CB".equals(boletoExtraido.getStatusValidacao()) &&
                !"INVALIDO".equals(boletoExtraido.getStatusValidacao())) { // Só atualiza se o status atual não for pior
                boletoExtraido.setStatusValidacao(statusValidacaoCNPJAPI);
            }
            boletoExtraido.addDetalheFalha("Alerta/Falha na validação CNPJ API: " + statusValidacaoCNPJAPI);
            verificacoesComFalha++;
        }

        // 7. Comparação de Nomes (PDF vs. API)
        String nomePdf = boletoExtraido.getNomeBeneficiario();
        String razaoApi = boletoExtraido.getRazaoSocialApi();

        if (nomePdf != null && !nomePdf.isEmpty() && razaoApi != null && !razaoApi.isEmpty()) {
            String nomePdfLimpo = nomePdf.toLowerCase().replaceAll("\\s+", "");
            String razaoApiLimpa = razaoApi.toLowerCase().replaceAll("\\s+", "");

            if (!nomePdfLimpo.equals(razaoApiLimpa) && !nomePdfLimpo.contains(razaoApiLimpa)
                    && !razaoApiLimpa.contains(nomePdfLimpo)) {
                boletoExtraido.setStatusValidacao("ALERTA_FRAUDE_NOME_CNPJ_DIVERGENTE");
                boletoExtraido.addDetalheFalha("ALERTA DE FRAUDE POTENCIAL: Nome do beneficiário no PDF ('" + boletoExtraido.getNomeBeneficiario() +
                        "') DIVERGE da Razão Social da API ('" + boletoExtraido.getRazaoSocialApi() + "').");
                verificacoesComFalha++;
                System.out.println("**ALERTA DE FRAUDE POTENCIAL:** Nome do beneficiario no PDF ('" + boletoExtraido.getNomeBeneficiario() +
                                   "') DIVERGE da Razão Social da API ('" + boletoExtraido.getRazaoSocialApi() + "') para este CNPJ.");
            } else {
                System.out.println("O nome do beneficiario no PDF ('" + boletoExtraido.getNomeBeneficiario() +
                                   "') BATE com a Razao Social ('" + boletoExtraido.getRazaoSocialApi() + "').");
            }
        } else {
            boletoExtraido.addDetalheFalha("Alerta: Não foi possível comparar nome do beneficiário do PDF com a Razão Social da API (dados ausentes ou não disponíveis).");
            System.out.println("Nao foi possivel comparar o nome do beneficiario do PDF com a Razao Social(dados ausentes ou não disponíveis).");
        }

        // 8. Verificação de Banco com API
        ConsultaBanco consultaBanco = new ConsultaBanco(boletoExtraido);
        String statusValidacaoBancoAPI = consultaBanco.validarBancoComApi();
        boletoExtraido.setStatusValidacaoBanco(statusValidacaoBancoAPI);
        if (statusValidacaoBancoAPI.startsWith("ALERTA") || statusValidacaoBancoAPI.startsWith("INVALIDO")) {
            boletoExtraido.addDetalheFalha("Alerta/Falha na validação do Banco API: " + statusValidacaoBancoAPI);
            verificacoesComFalha++;
        }

        // 9. Atualizar Reputação do CNPJ
        boolean isBoletoFalhoParaReputacao = (verificacoesComFalha > 0); // Considera todas as falhas acumuladas

        try {
            if (boletoExtraido.getCnpjEmitente() != null && !boletoExtraido.getCnpjEmitente().isEmpty()) {
                repositorioCnpjReputacao.atualizarReputacaoCnpj(
                        boletoExtraido.getCnpjEmitente(),
                        isBoletoFalhoParaReputacao,
                        boletoExtraido.getTotalAtualizacoes()
                );

                Object[] reputacaoAtual = repositorioCnpjReputacao.buscarReputacaoCnpj(boletoExtraido.getCnpjEmitente());
                if (reputacaoAtual != null) {
                    BigDecimal score = (BigDecimal) reputacaoAtual[0];
                    int totalBoletosCnpj = (int) reputacaoAtual[1];
                    int totalDenunciasCnpj = (int) reputacaoAtual[2];

                    boletoExtraido.setScoreReputacaoCnpj(score);
                    boletoExtraido.setTotalBoletosCnpj(totalBoletosCnpj);
                    boletoExtraido.setTotalDenunciasCnpj(totalDenunciasCnpj);

                    String classificacao;
                    if (totalBoletosCnpj < 5) {
                        classificacao = "Insuficiente";
                    } else if (score.compareTo(new BigDecimal("80.00")) > 0) {
                        classificacao = "Confiável";
                    } else if (score.compareTo(new BigDecimal("50.00")) >= 0) {
                        classificacao = "Risco Moderado";
                    } else if (score.compareTo(BigDecimal.ZERO) == 0) {
                        classificacao = "Reincidente";
                    } else {
                        classificacao = "Problemático";
                    }

                    if ((classificacao.equals("Reincidente") || classificacao.equals("Problemático"))
                            && totalDenunciasCnpj >= 5) {
                        boletoExtraido.setSuspeito(true);
                        // Se for suspeito e não for um status de invalidez mais forte
                        if (!"INVALIDO".equals(boletoExtraido.getStatusValidacao()) && !"INVALIDO_ESTRUTURA_CB".equals(boletoExtraido.getStatusValidacao())) {
                            boletoExtraido.setStatusValidacao("ALERTA_GERAL_NAO_CONFORMIDADE");
                        }
                        boletoExtraido.addDetalheFalha("Alerta: Boleto de CNPJ classificado como '" + classificacao + "' e com "
                                + totalDenunciasCnpj + " denúncias. Marcado como SUSPEITO!");
                        verificacoesComFalha++;
                    }
                } else {
                    boletoExtraido.addDetalheFalha("Alerta: Não foi possível buscar a reputação do CNPJ.");
                }
            } else {
                boletoExtraido.addDetalheFalha("Alerta: Não foi possível calcular reputação: CNPJ Emitente não extraído ou vazio.");
            }
        } catch (SQLException ex) {
            boletoExtraido.addDetalheFalha("Falha: Erro ao processar reputação do CNPJ: " + ex.getMessage());
            verificacoesComFalha++;
            throw ex; // Ainda lança a exceção de SQL para tratamento da camada superior
        }

        // 10. Criação de Usuário Anônimo e Salvamento
        Usuario usuarioAnonimo = repositorioUsuario.criarUsuarioAnonimo();
        if (usuarioAnonimo == null || usuarioAnonimo.getId() == 0) {
            apagarArquivoTxtGerado(); // Tenta apagar o TXT antes de falhar
            throw new SQLException("Falha crítica: Não foi possível criar um usuário anônimo. O boleto não será salvo.");
        }
        boletoExtraido.setUsuarioId(usuarioAnonimo.getId());

        if (!repositorioBoleto.inserirBoleto(boletoExtraido)) {
            apagarArquivoTxtGerado(); // Tenta apagar o TXT antes de falhar
            throw new SQLException("Falha desconhecida ao salvar o boleto.");
        }

        // Se o processamento chegou até aqui e não houve exceções críticas, apagar o TXT
        apagarArquivoTxtGerado();

        // Finalmente, define o status geral do boleto baseado nas verificações com falha
        if (verificacoesComFalha > 0) {
            // Se já não houver um status de invalidez mais grave, ou se já não for um alerta mais específico
            if (!boletoExtraido.getStatusValidacao().startsWith("INVALIDO") &&
                !boletoExtraido.getStatusValidacao().startsWith("ALERTA_FRAUDE")) {
                boletoExtraido.setStatusValidacao("VALIDO_COM_ALERTAS");
            }
        } else {
            if (!boletoExtraido.getStatusValidacao().startsWith("INVALIDO") &&
                !boletoExtraido.getStatusValidacao().startsWith("ALERTA")) {
                boletoExtraido.setStatusValidacao("VALIDO_COMPLETO");
            }
        }
    }


    // --- Métodos Auxiliares de Processamento (mantidos aqui) ---
    // Este método agora retorna uma String de status em vez de lançar exceção
    private String verificarValorBoleto(String codigoBarras, BigDecimal valorInformado) {
        if (codigoBarras == null || codigoBarras.length() < 10 || valorInformado == null) {
            return "DADOS_INCOMPLETOS";
        }

        BigDecimal valorDoCodigoBarras = BigDecimal.ZERO;
        try {
            String valorStr = codigoBarras.substring(codigoBarras.length() - 10);
            String valorFormatado = valorStr.substring(0, 8) + "." + valorStr.substring(8, 10);
            valorDoCodigoBarras = new BigDecimal(valorFormatado);
        } catch (NumberFormatException e) {
            System.err.println("Erro ao extrair valor do código de barras para verificação: " + e.getMessage());
            return "ERRO_EXTRACAO_VALOR_CB";
        }

        if (valorInformado.compareTo(valorDoCodigoBarras) != 0) {
            return "VALOR_DIVERGENTE";
        }
        return "OK";
    }

    private void apagarArquivoTxtGerado() {
        if (arquivoTxtGerado != null && arquivoTxtGerado.exists()) {
            if (arquivoTxtGerado.delete()) {
                System.out.println("Arquivo TXT '" + arquivoTxtGerado.getName() + "' apagado com sucesso.");
            } else {
                System.err.println("Não foi possível apagar o arquivo TXT '" + arquivoTxtGerado.getName() + "'.");
            }
        } else {
            System.out.println("Nenhum arquivo TXT para apagar ou arquivo não existe.");
        }
    }

    private void inserirOuAtualizarCnpjEmitente(String cnpj, String nomeRazaoSocial) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM CNPJ_Emitente WHERE cnpj = ?";
        String insertSql = "INSERT INTO CNPJ_Emitente (cnpj, nome_razao_social, data_abertura) VALUES (?, ?, ?)";

        try (Connection conexao = bancodedados.ConexaoBD.getConexao()) {
            try (PreparedStatement checkStmt = conexao.prepareStatement(checkSql)) {
                checkStmt.setString(1, cnpj);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("CNPJ Emitente '" + cnpj + "' já existe na tabela CNPJ_Emitente.");
                    return;
                }
            }

            try (PreparedStatement insertStmt = conexao.prepareStatement(insertSql)) {
                insertStmt.setString(1, cnpj);
                insertStmt.setString(2, nomeRazaoSocial != null && !nomeRazaoSocial.isEmpty() ? nomeRazaoSocial
                        : "Desconhecido (Extraído do PDF)");
                insertStmt.setDate(3, Date.valueOf(LocalDate.now()));

                int linhasAfetadas = insertStmt.executeUpdate();
                if (linhasAfetadas > 0) {
                    System.out.println("CNPJ Emitente '" + cnpj + "' inserido na tabela CNPJ_Emitente.");
                } else {
                    System.err.println("Falha ao inserir CNPJ Emitente '" + cnpj + "' na tabela CNPJ_Emitente.");
                }
            }
        }
    }
}