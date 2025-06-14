package verificacao;

import usuario.Boleto;
import java.util.Scanner;
import java.sql.SQLException; // Para lidar com exceções de DB, se o repositório lançar
import bancodedados.RepositorioCnpjEmitente;

public class ValidadorEmitente {

    private Boleto boleto;
    private Scanner scanner;
    private RepositorioCnpjEmitente repositorioCnpjEmitente; // Novo repositório para o CNPJ

    public ValidadorEmitente(Boleto boleto, Scanner scanner) {
        this.boleto = boleto;
        this.scanner = scanner;
        this.repositorioCnpjEmitente = new RepositorioCnpjEmitente(); // Inicializa o repositório
    }

    /**
     * Realiza a validação do CNPJ do beneficiário usando a BrasilAPI e confirma com o usuário.
     * Esta é a Verificação 3.
     * @return String indicando o status da validação do CNPJ (VALIDO_CNPJ_API_E_USUARIO, CNPJ_NAO_CONFIRMADO_USUARIO, etc.)
     */
    public String validarCnpjBeneficiario() {
        System.out.println("\n--- Verificacao do CNPJ do Beneficiario ---");
        System.out.println("Por favor, informe o CNPJ do beneficiario (somente numeros):");
        String cnpjInformado = scanner.nextLine().trim().replaceAll("[^0-9]", "");
        boleto.setCnpjEmitente(cnpjInformado);

        if (cnpjInformado.length() == 14) {
            System.out.println("Consultando CNPJ...");
            ConsultaCNPJ consultaCnpj = new ConsultaCNPJ(boleto);
            String statusConsultaCnpj = consultaCnpj.validarDadosComApi();

            System.out.println("Status da validacao do CNPJ pela API: " + statusConsultaCnpj);

            try {
                // Inserir/atualizar CNPJ na tabela CNPJ_Emitente
                repositorioCnpjEmitente.inserirOuAtualizarCnpjEmitente(cnpjInformado, boleto.getRazaoSocialApi());
            } catch (SQLException e) {
                System.err.println("Erro ao tentar inserir/atualizar CNPJ emitente no banco de dados: " + e.getMessage());
                e.printStackTrace();
                // Pode retornar um status de erro específico se a falha no DB for crítica
            }

            System.out.println("\n--- Dados do CNPJ " + cnpjInformado + " Retornados pela BrasilAPI ---");
            System.out.println("Razão Social: " + (boleto.getRazaoSocialApi() != null ? boleto.getRazaoSocialApi() : "Nao disponivel"));
            System.out.println("Nome Fantasia: " + (boleto.getNomeFantasiaApi() != null ? boleto.getNomeFantasiaApi() : "Nao disponivel"));
            System.out.println("--------------------------------------------------");

            scanner.nextLine();
            System.out.println("\nAs informacoes do CNPJ acima estao corretas? (sim/nao)");
            String confirmacaoDadosCnpj = scanner.nextLine().trim().toLowerCase();

            if ("sim".equals(confirmacaoDadosCnpj)) {
                System.out.println("Confirmacao dos dados do CNPJ registrada.");
                boleto.setInformacoesConfirmadasPeloUsuario(true);
                if ("VALIDO".equals(statusConsultaCnpj)) { // Usar "VALIDO" que vem da ConsultaCNPJ.validarDadosComApi()
                    return "VALIDO_CNPJ_API_E_USUARIO";
                } else if ("ERRO_API".equals(statusConsultaCnpj)) {
                     // Se a API deu erro, mas o usuário confirmou, ainda é um alerta
                    return "CNPJ_CONFIRMADO_USUARIO_COM_ALERTA_API_OFFLINE";
                } else { // INVALIDO ou ERRO
                    return "CNPJ_CONFIRMADO_USUARIO_COM_ALERTA";
                }
            } else {
                System.out.println("Voce indicou que os dados do CNPJ nao estao corretos. Marcarei como 'CNPJ_NAO_CONFIRMADO_USUARIO'.");
                boleto.setInformacoesConfirmadasPeloUsuario(false);
                return "CNPJ_NAO_CONFIRMADO_USUARIO";
            }
        } else {
            System.out.println("CNPJ invalido. A consulta nao sera realizada. Marcarei como 'CNPJ_INVALIDO_FORMATO'.");
            boleto.setInformacoesConfirmadasPeloUsuario(false);
            return "CNPJ_INVALIDO_FORMATO";
        }
    }

    /**
     * Realiza a validação dos dados bancários usando a BrasilAPI e confirma com o usuário.
     * Esta é a Verificação 4.
     * @return String indicando o status da validação do banco (VALIDO_BANCO_API_E_USUARIO, BANCO_NAO_CONFIRMADO_USUARIO, etc.)
     */
    public String validarDadosBancarios() {
        System.out.println("\n--- Verificacao do Banco Emissor ---");
        String codigoBanco = boleto.getCodigoBarras().substring(0, 3);
        boleto.setBancoEmissor(codigoBanco); // Define o código do banco extraído da linha digitável

        System.out.println("Codigo do banco extraido da linha digitavel: " + codigoBanco);

        ConsultaBanco consultaBanco = new ConsultaBanco(boleto);
        String statusConsultaBanco = consultaBanco.validarBancoComApi();
        // O método validarBancoComApi da ConsultaBanco já preenche os campos nomeBancoApi, nomeCompletoBancoApi, ispbBancoApi no objeto boleto

        System.out.println("Status da validacao do banco pela API: " + statusConsultaBanco);

        System.out.println("\n--- Dados do Banco " + codigoBanco + " Retornados pela BrasilAPI ---");
        System.out.println("Codigo do Banco (API): " + (boleto.getCodigoBancoApi() != null ? boleto.getCodigoBancoApi() : "Nao disponivel"));
        System.out.println("Nome do Banco (API): " + (boleto.getNomeBancoApi() != null ? boleto.getNomeBancoApi() : "Nao disponivel"));
        System.out.println("Nome Completo do Banco (API): " + (boleto.getNomeCompletoBancoApi() != null ? boleto.getNomeCompletoBancoApi() : "Nao disponivel"));
        System.out.println("ISPB (API): " + (boleto.getIspbBancoApi() != null ? boleto.getIspbBancoApi() : "Nao disponivel"));
        System.out.println("--------------------------------------------------");

        System.out.println("\nAs informacoes acima estao corretas? (sim/nao)");
        String confirmacaoDadosBanco = scanner.nextLine().trim().toLowerCase();

        if ("sim".equals(confirmacaoDadosBanco)) {
            System.out.println("Confirmacao dos dados do banco registrada.");
            if ("VALIDO".equals(statusConsultaBanco)) { // Usar "VALIDO" que vem da ConsultaBanco.validarBancoComApi()
                return "VALIDO_BANCO_API_E_USUARIO";
            } else if ("ERRO_API".equals(statusConsultaBanco)) {
                // Se a API deu erro, mas o usuário confirmou, ainda é um alerta
                return "BANCO_CONFIRMADO_USUARIO_COM_ALERTA_API_OFFLINE";
            }
            else { // INVALIDO ou ERRO
                return "BANCO_CONFIRMADO_USUARIO_COM_ALERTA";
            }
        } else {
            System.out.println("Voce indicou que os dados do banco nao estao corretos. Marcarei como 'BANCO_NAO_CONFIRMADO_USUARIO'.");
            return "BANCO_NAO_CONFIRMADO_USUARIO";
        }
    }
}