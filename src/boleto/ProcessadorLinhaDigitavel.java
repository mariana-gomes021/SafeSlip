// Em boleto/ProcessadorLinhaDigitavel.java

package boleto;

import boleto.ValidadorLinhaDigitavel;
import verificacao.ConsultaCNPJ;
import verificacao.ConsultaBanco;
import usuario.Boleto;
import java.math.BigDecimal;
import java.util.Scanner;

public class ProcessadorLinhaDigitavel {

    private Scanner scanner;

    public ProcessadorLinhaDigitavel(Scanner scanner) {
        this.scanner = scanner;
    }

    public void processar() {
        System.out.println("\n--- Processamento de Boleto por Linha Digital ---");
        System.out.println("Por favor, digite a linha digital (código de barras, com ou sem pontos/espaços):");
        
        String linhaDigitalInput = scanner.nextLine();
        String linhaDigital = linhaDigitalInput.trim().replaceAll("[^0-9]", "");

        if (linhaDigital.length() != 47) {
            System.err.println("❌ Erro: A linha digital deve conter 47 dígitos numéricos. Você digitou " + linhaDigitalInput.length() + " caracteres (incluindo pontos/espaços) que resultaram em " + linhaDigital.length() + " dígitos numéricos.");
            System.err.println("Por favor, verifique a entrada. Ex: 00190000090362072700200439729179799850000222900");
            return;
        }

        System.out.println("✅ Linha digital capturada e limpa: " + linhaDigital);

        Boleto boleto = new Boleto();
        boleto.setCodigoBarras(linhaDigital);

        // Validação da estrutura da linha digital
        System.out.println("\n--- Realizando validação detalhada da Linha Digital ---");
        boolean linhaDigitalEstruturaValida = ValidadorLinhaDigitavel.validar(linhaDigital);
        
        if (!linhaDigitalEstruturaValida) {
            System.out.println("❌ Validação de estrutura da Linha Digital FALHOU.");
            boleto.setStatusValidacao("ERRO_ESTRUTURA_LD"); 
            System.out.println("⚠️ No entanto, continuaremos com outras verificações.");
        } else {
             System.out.println("✅ Validação de estrutura da Linha Digital OK.");
             boleto.setStatusValidacao("VALIDO_ESTRUTURA_LD");
        }

        System.out.println("\nEste boleto possui algum desconto? (sim/nao)");
        String temDescontoStr = scanner.nextLine().trim().toLowerCase();
        boolean temDesconto = "sim".equals(temDescontoStr);

        BigDecimal valorInformadoPeloUsuario = null;
        if (temDesconto) {
            System.out.println("Por favor, digite o valor *original* do boleto (sem descontos, formato 00.00):");
            while (valorInformadoPeloUsuario == null) {
                try {
                    String valorInput = scanner.nextLine().replace(",", ".");
                    valorInformadoPeloUsuario = new BigDecimal(valorInput);
                } catch (NumberFormatException e) {
                    System.err.println("❌ Formato de valor inválido. Digite novamente o valor original (ex: 123.45):");
                }
            }
        } else {
            System.out.println("Por favor, digite o valor do pagamento do boleto (formato 00.00):");
            while (valorInformadoPeloUsuario == null) {
                try {
                    String valorInput = scanner.nextLine().replace(",", ".");
                    valorInformadoPeloUsuario = new BigDecimal(valorInput);
                } catch (NumberFormatException e) {
                    System.err.println("❌ Formato de valor inválido. Digite novamente o valor (ex: 123.45):");
                }
            }
        }
        boleto.setValor(valorInformadoPeloUsuario);

        BigDecimal valorDoCodigoBarras = boleto.getValorDoCodigoBarras(); 

        System.out.println("\n--- Verificação do Valor Final ---");
        System.out.println("Valor informado pelo usuário (sem desconto): " + valorInformadoPeloUsuario);
        System.out.println("Valor extraído da linha digital: " + valorDoCodigoBarras);

        if (valorInformadoPeloUsuario.compareTo(valorDoCodigoBarras) == 0) {
            System.out.println("✅ O valor informado corresponde ao valor na linha digital. Verificação de valor OK.");
        } else {
            System.out.println("⚠️ ATENÇÃO: O valor informado NÃO corresponde ao valor na linha digital. Isso pode indicar um problema.");
        }

        System.out.println("\n--- Verificação do CNPJ ---");
        System.out.println("Por favor, informe o CNPJ do beneficiário (somente números):");
        String cnpjInformado = scanner.nextLine().trim().replaceAll("[^0-9]", "");
        boleto.setCnpjEmitente(cnpjInformado);

        if (cnpjInformado.length() == 14) {
            System.out.println("🌐 Consultando CNPJ na BrasilAPI...");
            ConsultaCNPJ consultaCnpj = new ConsultaCNPJ(boleto);
            String statusConsultaCnpj = consultaCnpj.validarDadosComApi();

            System.out.println("ℹ️ Status da validação do CNPJ com a API: " + statusConsultaCnpj);

            System.out.println("\n--- Dados do CNPJ " + cnpjInformado + " Retornados pela BrasilAPI ---");
            System.out.println("📝 Razão Social: " + boleto.getRazaoSocialApi());
            System.out.println("✨ Nome Fantasia: " + boleto.getNomeFantasiaApi());
            System.out.println("--------------------------------------------------");

            System.out.println("\nAs informações do CNPJ acima estão corretas? (sim/nao)");
            String confirmacaoDadosCnpj = scanner.nextLine().trim().toLowerCase();

            if ("sim".equals(confirmacaoDadosCnpj)) {
                System.out.println("👍 Confirmação dos dados do CNPJ registrada.");
                boleto.setInformacoesConfirmadasPeloUsuario(true);
                if (statusConsultaCnpj.equals("VALIDO_API")) {
                    boleto.setStatusValidacao("VALIDO_CNPJ_API_E_USUARIO");
                } else {
                    boleto.setStatusValidacao("CNPJ_CONFIRMADO_USUARIO_COM_ALERTA");
                }
            } else {
                System.out.println("🚫 Você indicou que os dados do CNPJ não estão corretos. Marcarei como 'CNPJ_NAO_CONFIRMADO_USUARIO'.");
                boleto.setInformacoesConfirmadasPeloUsuario(false);
                boleto.setStatusValidacao("CNPJ_NAO_CONFIRMADO_USUARIO");
            }
        } else {
            System.out.println("⚠️ CNPJ inválido. A consulta não será realizada. Marcarei como 'CNPJ_INVALIDO_FORMATO'.");
            boleto.setStatusValidacao("CNPJ_INVALIDO_FORMATO");
            boleto.setInformacoesConfirmadasPeloUsuario(false);
        }

        // --- INÍCIO DA LÓGICA PARA CONFIRMAÇÃO DETALHADA DO BANCO ---
        System.out.println("\n--- Verificação do Banco Emissor ---");
        String codigoBanco = linhaDigital.substring(0, 3);
        boleto.setBancoEmissor(codigoBanco);

        System.out.println("🏦 Código do banco extraído da linha digital: " + codigoBanco);

        ConsultaBanco consultaBanco = new ConsultaBanco(boleto);
        String statusConsultaBanco = consultaBanco.validarBancoComApi(); // Isso preenche os campos do boleto

        System.out.println("ℹ️ Status da validação do banco com a API: " + statusConsultaBanco);

        // Exibir os dados puxados da API
        // Exibir os dados puxados da API - **AQUI ESTÁ A CORREÇÃO**
        System.out.println("\n--- Dados do Banco " + codigoBanco + " Retornados pela BrasilAPI ---");
        // O código do banco que você quer exibir da API é o que está no boleto.getBancoEmissor()
        // OU, se você quisesse o código retornado *pela API* (que já foi comparado e deve ser igual)
        // você teria que ter um campo 'codigoBancoApi' no Boleto, mas como ele já é comparado e validado
        // contra o código extraído, pode usar o próprio codigoBanco.
        System.out.println("🏦 Código do Banco (API): " + codigoBanco); // O código extraído da linha digital
        System.out.println("📝 Nome do Banco (API): " + (boleto.getNomeBancoApi() != null ? boleto.getNomeBancoApi() : "Não disponível"));
        System.out.println("✨ Nome Completo do Banco (API): " + (boleto.getNomeCompletoBancoApi() != null ? boleto.getNomeCompletoBancoApi() : "Não disponível"));
        System.out.println("🔢 ISPB (API): " + (boleto.getIspbBancoApi() != null ? boleto.getIspbBancoApi() : "Não disponível"));
        System.out.println("--------------------------------------------------");

        // Pergunta de confirmação ao usuário
        System.out.println("\nAs informações do banco acima estão corretas? (sim/nao)");
        String confirmacaoDadosBanco = scanner.nextLine().trim().toLowerCase();

        if ("sim".equals(confirmacaoDadosBanco)) {
            System.out.println("👍 Confirmação dos dados do banco registrada.");
            // Atualiza o status do banco para indicar que foi confirmado pelo usuário
            if (statusConsultaBanco.equals("VALIDO_API")) {
                boleto.setStatusValidacaoBanco("VALIDO_BANCO_API_E_USUARIO");
            } else {
                // Se a API teve um problema, mas o usuário confirmou
                boleto.setStatusValidacaoBanco("BANCO_CONFIRMADO_USUARIO_COM_ALERTA"); 
            }
            boleto.setInformacoesConfirmadasPeloUsuario(true); // Opcional: considerar como confirmação geral
        } else {
            System.out.println("🚫 Você indicou que os dados do banco não estão corretos. Marcarei como 'BANCO_NAO_CONFIRMADO_USUARIO'.");
            boleto.setStatusValidacaoBanco("BANCO_NAO_CONFIRMADO_USUARIO");
            boleto.setInformacoesConfirmadasPeloUsuario(false); // Opcional: considerar como confirmação geral
        }
        // --- FIM DA LÓGICA PARA CONFIRMAÇÃO DETALHADA DO BANCO ---


        System.out.println("\n--- Resumo e Preparação para Salvamento (Simulado) ---");
        System.out.println("Linha Digital: " + boleto.getCodigoBarras());
        System.out.println("Valor Informado pelo Usuário: " + boleto.getValor());
        System.out.println("Valor Extraído da Linha Digital: " + boleto.getValorDoCodigoBarras());
        System.out.println("CNPJ Beneficiário Informado: " + boleto.getCnpjEmitente());
        System.out.println("Razão Social (API): " + boleto.getRazaoSocialApi());
        System.out.println("Nome Fantasia (API): " + boleto.getNomeFantasiaApi());
        System.out.println("Situação Cadastral (API): " + boleto.getSituacaoCadastralApi());
        System.out.println("Código Banco (Linha Digital): " + boleto.getBancoEmissor());
        System.out.println("Nome Banco (API): " + boleto.getNomeBancoApi());
        System.out.println("Nome Completo Banco (API): " + boleto.getNomeCompletoBancoApi());
        System.out.println("ISPB (API): " + boleto.getIspbBancoApi());
        System.out.println("Informações CNPJ e Banco Confirmadas pelo Usuário: " + (boleto.isInformacoesConfirmadasPeloUsuario() ? "Sim" : "Não"));
        System.out.println("Status de Validação Geral: " + boleto.getStatusValidacao());
        System.out.println("Status de Validação de Banco: " + boleto.getStatusValidacaoBanco());
        System.out.println("Simulando salvamento do boleto no banco de dados...");
        System.out.println("Simulação de salvamento concluída.");

        System.out.println("\nProcessamento da linha digital concluído.");
    }
}