package boleto;

import boleto.extracao.ExtracaoBoleto;
import usuario.Boleto;
import usuario.Usuario;
import bancodedados.RepositorioBoleto;
import bancodedados.RepositorioUsuario;

import bancodedados.ConexaoBD;
import verificacao.ConsultaCNPJ;
import verificacao.ConsultaBanco;
import boleto.ValidadorLinhaDigitavel; // Importe a classe ValidadorCodigoBarras

import java.time.format.DateTimeFormatter; 
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.math.BigDecimal;
import bancodedados.RepositorioCnpjReputacao; // Importar RepositorioCnpjReputacao

public class ProcessadorBoleto {

    private ExtracaoBoleto extracaoBoleto;
    private RepositorioBoleto repositorioBoleto;
    private RepositorioUsuario repositorioUsuario;
    private RepositorioCnpjReputacao repositorioCnpjReputacao; // Instância do repositório de reputação
    private Scanner scanner;
    private EnvioBoleto envioBoleto;
    private File arquivoTxtParaApagar; // Para gerenciar o arquivo TXT gerado

    public ProcessadorBoleto(Scanner scanner) {
        this.extracaoBoleto = new ExtracaoBoleto();
        this.repositorioBoleto = new RepositorioBoleto();
        this.repositorioUsuario = new RepositorioUsuario();
        this.repositorioCnpjReputacao = new RepositorioCnpjReputacao(); // Inicialização
        this.scanner = scanner;
        this.envioBoleto = new EnvioBoleto();
    }

    public void processarNovoBoleto() throws IOException, SQLException {
        System.out.println("\n Abrindo a janela de seleaoo de arquivo. Por favor, selecione o boleto em PDF.");
        
        File pdfSelecionado = envioBoleto.selecionarArquivoPDF();
        if (pdfSelecionado == null) {
            System.err.println("Processamento cancelado: Nenhum arquivo PDF foi selecionado.");
            return;
        }

        extracaoBoleto.setCaminhoToArquivo(pdfSelecionado);
        this.arquivoTxtParaApagar = extracaoBoleto.getArquivoTxtGerado(pdfSelecionado);

        System.out.println("\n Iniciando extracao do boleto...");
        Boleto boletoExtraido = extracaoBoleto.processarTxt();

        boolean extracaoMinimaBemSucedida =
                boletoExtraido != null &&
                boletoExtraido.getCodigoBarras() != null &&
                !boletoExtraido.getCodigoBarras().isEmpty() &&
                boletoExtraido.getCnpjEmitente() != null &&
                !boletoExtraido.getCnpjEmitente().isEmpty();

        if (extracaoMinimaBemSucedida) {
            System.out.println("\n--- Detalhes do Boleto Extraido para Confirmacao ---");
            // Usando getValorAsBigDecimal() para exibição, mas verificando se não é nulo antes
            System.out.println("Valor: " + (boletoExtraido.getValorAsBigDecimal() != null ? boletoExtraido.getValorAsBigDecimal() : "Nao encontrado"));
            System.out.println("Data de Vencimento: " + (boletoExtraido.getVencimento() != null ? boletoExtraido.getVencimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Nao encontrado"));
            System.out.println("CNPJ do Beneficiario: " + (boletoExtraido.getCnpjEmitente() != null ? boletoExtraido.getCnpjEmitente() : "Nao encontrado"));
            System.out.println("Nome do Beneficiario: " + (boletoExtraido.getNomeBeneficiario() != null ? boletoExtraido.getNomeBeneficiario() : "Nao encontrado"));
            System.out.println("Banco Emissor (extraido do PDF): " + (boletoExtraido.getBancoEmissor() != null ? boletoExtraido.getBancoEmissor() : "Nao encontrado"));
            System.out.println("Numero do Codigo de Barras: " + (boletoExtraido.getCodigoBarras() != null ? boletoExtraido.getCodigoBarras() : "Nao encontrado"));
            System.out.println("--------------------------------------------------");

            // PRIMEIRA PERGUNTA: Confirmação dos dados extraídos
            System.out.println("\n As informacoes do boleto estao corretas? (sim/nao)");
            String confirmacao = scanner.nextLine();

            boolean usuarioConfirmou = "sim".equalsIgnoreCase(confirmacao);
            boletoExtraido.setInformacoesConfirmadasPeloUsuario(usuarioConfirmou);

            if (!usuarioConfirmou && !"nao".equalsIgnoreCase(confirmacao)) {
                System.out.println("Resposta inválida. O boleto sera salvo, mas marcado como nao confirmado pelo usuario.");
            } else if (!usuarioConfirmou) {
                System.out.println("Usuario indicou que as informacoes nao estao corretas. O boleto sera salvo para analise, mas marcado como nao confirmado.");
            } else {
                System.out.println("Confirmacao registrada. Prosseguindo com o salvamento e verificacao.");
            }
            
            // SEGUNDA PERGUNTA: Informações sobre desconto e valor original/sem desconto
            System.out.println("\n Este boleto possui algum desconto? (sim/nao)");
            String temDescontoStr = scanner.nextLine().trim().toLowerCase();
            boolean temDesconto = "sim".equals(temDescontoStr);

            BigDecimal valorSemDescontoInformado = null;
            if (temDesconto) {
                System.out.println("Por favor, digite o valor *original* do boleto (sem descontos, formato 00.00):");
                while (valorSemDescontoInformado == null) {
                    try {
                        String valorInput = scanner.nextLine().replace(",", "."); // Troca vírgula por ponto
                        valorSemDescontoInformado = new BigDecimal(valorInput);
                    } catch (NumberFormatException e) {
                        System.err.println("Formato de valor invalido. Digite novamente o valor original (ex: 123.45):");
                    }
                }
            } else {
                valorSemDescontoInformado = boletoExtraido.getValorAsBigDecimal(); // Usar getter para BigDecimal
                if (valorSemDescontoInformado == null) {
                    System.out.println("Valor nao extraido do PDF. Por favor, digite o valor do pagamento do boleto (formato 00.00):");
                     while (valorSemDescontoInformado == null) {
                        try {
                            String valorInput = scanner.nextLine().replace(",", ".");
                            valorSemDescontoInformado = new BigDecimal(valorInput);
                        } catch (NumberFormatException e) {
                            System.err.println("Formato de valor invalido. Digite novamente o valor (ex: 123.45):");
                        }
                    }
                }
            }
            // Realiza a verificação do valor contra o código de barras
            verificarValorBoleto(boletoExtraido.getCodigoBarras(), valorSemDescontoInformado);
            
            // INÍCIO DA VALIDAÇÃO DETALHADA DO CÓDIGO DE BARRAS
            System.out.println("\n--- Realizando validação detalhada do Codigo de Barras ---");
            boolean codigoBarrasEstruturaValida = ValidadorLinhaDigitavel.validar(boletoExtraido.getCodigoBarras());
            
            if (!codigoBarrasEstruturaValida) {
                System.out.println("Validacao de estrutura do Codigo de Barras FALHOU. Marcarei como 'INVALIDO'.");
                boletoExtraido.setStatusValidacao("INVALIDO"); // Ou um status mais específico como "ERRO_ESTRUTURA_CB"
            } else {
                System.out.println("Validacao de estrutura do Codigo de Barras OK.");
            }
            // FIM DA VALIDAÇÃO DETALHADA DO CÓDIGO DE BARRAS

            String cnpjEmitente = boletoExtraido.getCnpjEmitente();
            if (cnpjEmitente != null && !cnpjEmitente.isEmpty()) {
                inserirOuAtualizarCnpjEmitente(cnpjEmitente, boletoExtraido.getNomeBeneficiario());
            } else {
                System.err.println("CNPJ do emitente nao extraido ou invalido. O boleto sera salvo, mas com um aviso.");
            }

            System.out.println("\n Verificando dados do CNPJ do boleto...");
            ConsultaCNPJ consultaCnpj = new ConsultaCNPJ(boletoExtraido);
            String statusValidacaoCNPJAPI = consultaCnpj.validarDadosComApi();
            // Só atualiza se o status não foi marcado como INVÁLIDO pela validação de estrutura do CB
            if (!"INVALIDO".equals(boletoExtraido.getStatusValidacao())) {
                boletoExtraido.setStatusValidacao(statusValidacaoCNPJAPI);
            }
            System.out.println("Status da validacao do CNPJ: " + statusValidacaoCNPJAPI);

            // Início da nova lógica de comparação de nomes
            int verificacoesComFalha = 0; // Contador para verificações que falharem
            
            String nomePdf = boletoExtraido.getNomeBeneficiario();
            String razaoApi = boletoExtraido.getRazaoSocialApi();

            if (nomePdf != null && !nomePdf.isEmpty() && razaoApi != null && !razaoApi.isEmpty()) {
                String nomePdfLimpo = nomePdf.toLowerCase().replaceAll("\\s+", ""); // Remove espaços e converte para minúsculas
                String razaoApiLimpa = razaoApi.toLowerCase().replaceAll("\\s+", ""); // Remove espaços e converte para minúsculas

                if (!nomePdfLimpo.equals(razaoApiLimpa) && !nomePdfLimpo.contains(razaoApiLimpa)
                        && !razaoApiLimpa.contains(nomePdfLimpo)) {
                    System.out.println("**ALERTA DE FRAUDE POTENCIAL:** Nome do beneficiario no PDF ('"
                            + boletoExtraido.getNomeBeneficiario() +
                            "') DIVERGE da Razão Social da API ('" + boletoExtraido.getRazaoSocialApi()
                            + "') para este CNPJ.");
                    boletoExtraido.setStatusValidacao("ALERTA_FRAUDE_NOME_CNPJ_DIVERGENTE");
                    verificacoesComFalha++;
                } else {
                    System.out.println("O nome do beneficiario no PDF ('" + boletoExtraido.getNomeBeneficiario() +
                            "') BATE com a Razao Social ('" + boletoExtraido.getRazaoSocialApi() + "').");
                }
            } else {
                System.out.println(
                        "Nao foi possivel comparar o nome do beneficiario do PDF com a Razao Social(dados ausentes ou não disponíveis).");
            }
            // Fim da nova lógica de comparação de nomes

            System.out.println("\n Verificando dados do banco do boleto...");
            ConsultaBanco consultaBanco = new ConsultaBanco(boletoExtraido);
            String statusValidacaoBancoAPI = consultaBanco.validarBancoComApi();
            boletoExtraido.setStatusValidacaoBanco(statusValidacaoBancoAPI);
            System.out.println("Status da validacao do banco: " + statusValidacaoBancoAPI);


            // Determinar se o boleto é "falho" para a reputação do CNPJ
            boolean isBoletoFalhoParaReputacao = false;
            if ("INVALIDO".equals(boletoExtraido.getStatusValidacao()) ||
                "ALERTA_FRAUDE_NOME_CNPJ_DIVERGENTE".equals(boletoExtraido.getStatusValidacao()) ||
                "CNPJ_INVALIDO_FORMATO".equals(boletoExtraido.getStatusValidacao()) ||
                "CNPJ_NAO_CONFIRMADO_USUARIO".equals(boletoExtraido.getStatusValidacao()) ||
                "BANCO_NAO_CONFIRMADO_USUARIO".equals(boletoExtraido.getStatusValidacaoBanco()) ||
                "ALERTA_GERAL_NAO_CONFORMIDADE".equals(boletoExtraido.getStatusValidacao())) {
                isBoletoFalhoParaReputacao = true;
            }

            // NOVO: Atualizar reputação do CNPJ
            try {
                // Antes de chamar RepositorioCnpjReputacao.atualizarReputacaoCnpj, o boletoExtraido.totalAtualizacoes
                // precisa estar com o valor correto do banco de dados (se o boleto já existia).
                // Isso é feito no RepositorioBoleto.inserirBoleto (na parte de 'if (rs.next())')
                // No entanto, para o ProcessadorBoleto, se o boleto *não existia* antes de ser extraído,
                // totalAtualizacoes será 0, o que é o valor padrão. Se existia e foi atualizado,
                // RepositorioBoleto já terá definido o valor em boletoExtraido.setTotalAtualizacoes().
                repositorioCnpjReputacao.atualizarReputacaoCnpj(boletoExtraido.getCnpjEmitente(), isBoletoFalhoParaReputacao, boletoExtraido.getTotalAtualizacoes());

                // Busca a reputação atualizada para exibir ao usuário e salvar no boleto
                Object[] reputacaoAtual = repositorioCnpjReputacao
                        .buscarReputacaoCnpj(boletoExtraido.getCnpjEmitente());
                if (reputacaoAtual != null) {
                    BigDecimal score = (BigDecimal) reputacaoAtual[0];
                    int totalBoletosCnpj = (int) reputacaoAtual[1];
                    int totalDenunciasCnpj = (int) reputacaoAtual[2];

                    // Define os valores de reputação no objeto Boleto
                    boletoExtraido.setScoreReputacaoCnpj(score);
                    boletoExtraido.setTotalBoletosCnpj(totalBoletosCnpj);
                    boletoExtraido.setTotalDenunciasCnpj(totalDenunciasCnpj);

                    String classificacao;
                    if (score.compareTo(new BigDecimal("80.00")) > 0) {
                        classificacao = "Confiável";
                    } else if (score.compareTo(new BigDecimal("50.00")) >= 0
                            && score.compareTo(new BigDecimal("80.00")) <= 0) {
                        classificacao = "Risco Moderado";
                    } else {
                        if (score.compareTo(BigDecimal.ZERO) == 0) {
                            classificacao = "Reincidente";
                        } else {
                            classificacao = "Problemático";
                        }
                        System.out.println("\n⚠ Este CNPJ possui muitas denúncias anteriores. Risco elevado.");
                    }

                    System.out.println("Total de Boletos (CNPJ): " + totalBoletosCnpj);
                    System.out.println("Total de Denuncias (CNPJ): " + totalDenunciasCnpj);
                    System.out.printf("Score de Reputacao (CNPJ): %.2f%%\n", score);
                    System.out.println("Classificacao (CNPJ): " + classificacao);
                    System.out.println("Calculo de reputacao concluido para o CNPJ: " + boletoExtraido.getCnpjEmitente());

                    if ((classificacao.equals("Reincidente") || classificacao.equals("Problemático"))
                            && totalDenunciasCnpj >= 10) {
                        boletoExtraido.setSuspeito(true); // ATUALIZADO: setSuspeito(true)
                        System.out.println("Boleto de CNPJ classificado como '" + classificacao + "' e com "
                                + totalDenunciasCnpj + " denuncias. Marcado como SUSPEITO automaticamente!"); // ATUALIZADO: SUSPEITO
                    }
                }
            } catch (SQLException e) {
                System.err.println("Erro ao processar reputacao do CNPJ: " + e.getMessage());
                // Não relança a exceção aqui para não impedir o salvamento do boleto
            }
            // FIM NOVO: Atualizar reputação do CNPJ


            Usuario usuarioAnonimo = repositorioUsuario.criarUsuarioAnonimo();
            if (usuarioAnonimo == null || usuarioAnonimo.getId() == 0) {
                System.err.println("Falha critica: Nao foi possivel criar um usuario anonimo. O boleto nao sera salvo.");
                return;
            }
            System.out.println("Novo usuario anonimo criado com ID: " + usuarioAnonimo.getId());

            boletoExtraido.setUsuarioId(usuarioAnonimo.getId());

            System.out.println("\n Tentando salvar boleto no banco de dados...");
            System.out.println("Status Validacao CNPJ: " + boletoExtraido.getStatusValidacao());
            System.out.println("Status Validacao Banco: " + boletoExtraido.getStatusValidacaoBanco());
            System.out.println("Informacoes Confirmadas Pelo Usuario: " + boletoExtraido.isInformacoesConfirmadasPeloUsuario());
            

            if (repositorioBoleto.inserirBoleto(boletoExtraido)) {
                System.out.println("Boleto salvo no banco de dados com sucesso!");
                apagarArquivoTxtGerado();
            } else {
                System.err.println("Falha desconhecida ao salvar o boleto.");
            }

        } else {
            System.out.println("\n Nao foi possível extrair informações essenciais do boleto (código de barras ou CNPJ). Verifique o arquivo.");
        }
    }

    private void verificarValorBoleto(String codigoBarras, BigDecimal valorInformado) {
        if (codigoBarras == null || codigoBarras.length() < 10 || valorInformado == null) {
            System.out.println("⚠️ Não foi possível realizar a verificação do valor do boleto (dados incompletos).");
            return;
        }

        BigDecimal valorDoCodigoBarras = BigDecimal.ZERO;
        try {
            String valorStr = codigoBarras.substring(codigoBarras.length() - 10);
            String valorFormatado = valorStr.substring(0, 8) + "." + valorStr.substring(8, 10);
            valorDoCodigoBarras = new BigDecimal(valorFormatado);
        } catch (NumberFormatException e) {
            System.err.println("Erro ao extrair valor do código de barras para verificação: " + e.getMessage());
        }

        System.out.println("\n--- Verificação do Valor Final ---");
        System.out.println("Valor informado pelo usuário (sem desconto): " + valorInformado);
        System.out.println("Valor extraído do código de barras: " + valorDoCodigoBarras);

        if (valorInformado.compareTo(valorDoCodigoBarras) == 0) {
            System.out.println("✅ O valor informado corresponde ao valor no código de barras. Verificação de valor OK.");
        } else {
            System.out.println("⚠️ ATENÇÃO: O valor informado NÃO corresponde ao valor no código de barras. Isso pode indicar um problema.");
        }
    }

    private void apagarArquivoTxtGerado() {
        if (arquivoTxtParaApagar != null && arquivoTxtParaApagar.exists()) {
            if (arquivoTxtParaApagar.delete()) {
                System.out.println("🗑️ Arquivo TXT '" + arquivoTxtParaApagar.getName() + "' apagado com sucesso.");
            } else {
                System.err.println("❌ Não foi possível apagar o arquivo TXT '" + arquivoTxtParaApagar.getName() + "'.");
            }
        } else {
            System.out.println("ℹ️ Nenhum arquivo TXT para apagar ou arquivo não existe.");
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
                    System.out.println("✅ CNPJ Emitente '" + cnpj + "' já existe na tabela CNPJ_Emitente.");
                    return;
                }
            }

            try (PreparedStatement insertStmt = conexao.prepareStatement(insertSql)) {
                insertStmt.setString(1, cnpj);
                insertStmt.setString(2, nomeRazaoSocial != null && !nomeRazaoSocial.isEmpty() ? nomeRazaoSocial : "Desconhecido (Extraído do PDF)");
                insertStmt.setDate(3, Date.valueOf(LocalDate.now()));

                int linhasAfetadas = insertStmt.executeUpdate();
                if (linhasAfetadas > 0) {
                    System.out.println("➕ CNPJ Emitente '" + cnpj + "' inserido na tabela CNPJ_Emitente.");
                } else {
                    System.err.println("❌ Falha ao inserir CNPJ Emitente '" + cnpj + "' na tabela CNPJ_Emitente.");
                }
            }
        }
    }
}
