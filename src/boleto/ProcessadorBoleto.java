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

public class ProcessadorBoleto {

    private ExtracaoBoleto extracaoBoleto;
    private RepositorioBoleto repositorioBoleto;
    private RepositorioUsuario repositorioUsuario;
    private Scanner scanner;
    private EnvioBoleto envioBoleto; // Certifique-se de que EnvioBoleto está no pacote correto e acessível
    private File arquivoTxtParaApagar;

    public ProcessadorBoleto(Scanner scanner) {
        this.extracaoBoleto = new ExtracaoBoleto();
        this.repositorioBoleto = new RepositorioBoleto();
        this.repositorioUsuario = new RepositorioUsuario();
        this.scanner = scanner;
        this.envioBoleto = new EnvioBoleto();
    }

    public void processarNovoBoleto() throws IOException, SQLException {
        System.out.println("\n📂 Abrindo a janela de seleção de arquivo. Por favor, selecione o boleto em PDF.");
        
        File pdfSelecionado = envioBoleto.selecionarArquivoPDF();
        if (pdfSelecionado == null) {
            System.err.println("❌ Processamento cancelado: Nenhum arquivo PDF foi selecionado.");
            return;
        }

        extracaoBoleto.setCaminhoToArquivo(pdfSelecionado);
        // Correção aqui: era 'extracaoBaoletos', agora é 'extracaoBoleto'
        this.arquivoTxtParaApagar = extracaoBoleto.getArquivoTxtGerado(pdfSelecionado);

        System.out.println("\n⏳ Iniciando extração do boleto...");
        Boleto boletoExtraido = extracaoBoleto.processarTxt();

        boolean extracaoMinimaBemSucedida =
                boletoExtraido != null &&
                boletoExtraido.getCodigoBarras() != null &&
                !boletoExtraido.getCodigoBarras().isEmpty() &&
                boletoExtraido.getCnpjEmitente() != null &&
                !boletoExtraido.getCnpjEmitente().isEmpty();

        if (extracaoMinimaBemSucedida) {
            System.out.println("\n--- Detalhes do Boleto Extraído para Confirmação ---");
            System.out.println("💰 Valor: " + (boletoExtraido.getValor() != null ? boletoExtraido.getValor() : "Não encontrado"));
            System.out.println("🗓️ Data de Vencimento: " + (boletoExtraido.getVencimento() != null ? boletoExtraido.getVencimento().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Não encontrado"));
            System.out.println("🏢 CNPJ do Beneficiário: " + (boletoExtraido.getCnpjEmitente() != null ? boletoExtraido.getCnpjEmitente() : "Não encontrado"));
            System.out.println("📝 Nome do Beneficiário: " + (boletoExtraido.getNomeBeneficiario() != null ? boletoExtraido.getNomeBeneficiario() : "Não encontrado"));
            System.out.println("🏦 Banco Emissor (extraído do PDF): " + (boletoExtraido.getBancoEmissor() != null ? boletoExtraido.getBancoEmissor() : "Não encontrado"));
            System.out.println("🔢 Número do Código de Barras: " + (boletoExtraido.getCodigoBarras() != null ? boletoExtraido.getCodigoBarras() : "Não encontrado"));
            System.out.println("--------------------------------------------------");

            // PRIMEIRA PERGUNTA: Confirmação dos dados extraídos
            System.out.println("\nAs informações do boleto estão corretas? (sim/nao)");
            String confirmacao = scanner.nextLine();

            boolean usuarioConfirmou = "sim".equalsIgnoreCase(confirmacao);
            boletoExtraido.setInformacoesConfirmadasPeloUsuario(usuarioConfirmou);

            if (!usuarioConfirmou && !"nao".equalsIgnoreCase(confirmacao)) {
                 System.out.println("❓ Resposta inválida. O boleto será salvo, mas marcado como não confirmado pelo usuário.");
            } else if (!usuarioConfirmou) {
                System.out.println("🚫 Usuário indicou que as informações não estão corretas. O boleto será salvo para análise, mas marcado como não confirmado.");
            } else {
                System.out.println("👍 Confirmação registrada. Prosseguindo com o salvamento e verificação.");
            }
            
            // SEGUNDA PERGUNTA: Informações sobre desconto e valor original/sem desconto
            System.out.println("\nEste boleto possui algum desconto? (sim/nao)");
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
                        System.err.println("❌ Formato de valor inválido. Digite novamente o valor original (ex: 123.45):");
                    }
                }
            } else {
                valorSemDescontoInformado = boletoExtraido.getValor();
                if (valorSemDescontoInformado == null) {
                    System.out.println("Valor não extraído do PDF. Por favor, digite o valor do pagamento do boleto (formato 00.00):");
                     while (valorSemDescontoInformado == null) {
                        try {
                            String valorInput = scanner.nextLine().replace(",", ".");
                            valorSemDescontoInformado = new BigDecimal(valorInput);
                        } catch (NumberFormatException e) {
                            System.err.println("❌ Formato de valor inválido. Digite novamente o valor (ex: 123.45):");
                        }
                    }
                }
            }
            // Realiza a verificação do valor contra o código de barras
            verificarValorBoleto(boletoExtraido.getCodigoBarras(), valorSemDescontoInformado);
            
            // INÍCIO DA VALIDAÇÃO DETALHADA DO CÓDIGO DE BARRAS
            System.out.println("\n--- Realizando validação detalhada do Código de Barras ---");
            boolean codigoBarrasEstruturaValida = ValidadorLinhaDigitavel.validar(boletoExtraido.getCodigoBarras());
            
            if (!codigoBarrasEstruturaValida) {
                System.out.println("❌ Validação de estrutura do Código de Barras FALHOU. Marcarei como 'INVALIDO'.");
                boletoExtraido.setStatusValidacao("INVALIDO"); // Ou um status mais específico como "ERRO_ESTRUTURA_CB"
            } else {
                 System.out.println("✅ Validação de estrutura do Código de Barras OK.");
                 // Se o status já estiver 'VALIDO' e esta validação OK, mantém 'VALIDO'.
                 // Se estava 'ERRO' por outro motivo (ex: CNPJ inválido), e esta OK, não muda de 'ERRO'.
                 // Esta parte não sobrescreve um erro anterior, apenas complementa.
            }
            // FIM DA VALIDAÇÃO DETALHADA DO CÓDIGO DE BARRAS


            String cnpjEmitente = boletoExtraido.getCnpjEmitente();
            if (cnpjEmitente != null && !cnpjEmitente.isEmpty()) {
                inserirOuAtualizarCnpjEmitente(cnpjEmitente, boletoExtraido.getNomeBeneficiario());
            } else {
                System.err.println("⚠️ CNPJ do emitente não extraído ou inválido. O boleto será salvo, mas com um aviso.");
            }

            System.out.println("\n🌐 Verificando dados do CNPJ do boleto com a BrasilAPI...");
            ConsultaCNPJ consultaCnpj = new ConsultaCNPJ(boletoExtraido);
            String statusValidacaoCNPJAPI = consultaCnpj.validarDadosComApi();
            // Só atualiza se o status não foi marcado como INVÁLIDO pela validação de estrutura do CB
            if (!"INVALIDO".equals(boletoExtraido.getStatusValidacao())) {
                boletoExtraido.setStatusValidacao(statusValidacaoCNPJAPI);
            }
            System.out.println("ℹ️ Status da validação do CNPJ: " + statusValidacaoCNPJAPI);

            System.out.println("\n🏦 Verificando dados do banco do boleto com a BrasilAPI...");
            ConsultaBanco consultaBanco = new ConsultaBanco(boletoExtraido);
            String statusValidacaoBancoAPI = consultaBanco.validarBancoComApi();
            boletoExtraido.setStatusValidacaoBanco(statusValidacaoBancoAPI);
            System.out.println("ℹ️ Status da validação do banco: " + statusValidacaoBancoAPI);

            Usuario usuarioAnonimo = repositorioUsuario.criarUsuarioAnonimo();
            if (usuarioAnonimo == null || usuarioAnonimo.getId() == 0) {
                System.err.println("❌ Falha crítica: Não foi possível criar um usuário anônimo. O boleto não será salvo.");
                return;
            }
            System.out.println("🔗 Novo usuário anônimo criado com ID: " + usuarioAnonimo.getId());

            boletoExtraido.setUsuarioId(usuarioAnonimo.getId());

            System.out.println("\n💾 Tentando salvar boleto no banco de dados...");
            System.out.println("   Status Validação CNPJ: " + boletoExtraido.getStatusValidacao());
            System.out.println("   Status Validação Banco: " + boletoExtraido.getStatusValidacaoBanco());
            System.out.println("   Informações Confirmadas Pelo Usuário: " + boletoExtraido.isInformacoesConfirmadasPeloUsuario());

            if (repositorioBoleto.inserirBoleto(boletoExtraido)) {
                System.out.println("🎉 Boleto salvo no banco de dados com sucesso!");
                apagarArquivoTxtGerado();
            } else {
                System.err.println("❌ Falha desconhecida ao salvar o boleto.");
            }

        } else {
            System.out.println("\n🚫 Não foi possível extrair informações essenciais do boleto (código de barras ou CNPJ). Verifique o arquivo.");
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
        String insertSql = "INSERT INTO CNPJ_Emitente (cnpj, nome_razao_social, situacao_cadastral, data_abertura) VALUES (?, ?, ?, ?)";

        try (Connection conexao = ConexaoBD.getConexao()) {
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
                insertStmt.setString(3, "VERIFICAR API");
                insertStmt.setDate(4, Date.valueOf(LocalDate.now()));

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