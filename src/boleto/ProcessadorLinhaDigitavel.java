// Em boleto/ProcessadorLinhaDigital.java

package boleto;

import boleto.ValidadorLinhaDigitavel; // Importar a classe com o novo nome
import verificacao.ConsultaCNPJ;
import verificacao.ConsultaBanco;
import usuario.Boleto; // Se você usa a classe Boleto para guardar os dados
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

        // Validação da estrutura da linha digital
        System.out.println("\n--- Realizando validação detalhada da Linha Digital ---");
        boolean linhaDigitalEstruturaValida = ValidadorLinhaDigitavel.validar(linhaDigital);
        
        if (!linhaDigitalEstruturaValida) {
            System.out.println("❌ Validação de estrutura da Linha Digital FALHOU.");
            return; // Interrompe o processo se a linha for inválida
        } else {
             System.out.println("✅ Validação de estrutura da Linha Digital OK.");
        }

        // Perguntar sobre desconto e valor
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

        BigDecimal valorDoCodigoBarras = extrairValorDoCodigoBarras(linhaDigital);

        System.out.println("\n--- Verificação do Valor Final ---");
        System.out.println("Valor informado pelo usuário (sem desconto): " + valorInformadoPeloUsuario);
        System.out.println("Valor extraído do código de barras: " + valorDoCodigoBarras);

        if (valorInformadoPeloUsuario.compareTo(valorDoCodigoBarras) == 0) {
            System.out.println("✅ O valor informado corresponde ao valor no código de barras. Verificação de valor OK.");
        } else {
            System.out.println("⚠️ ATENÇÃO: O valor informado NÃO corresponde ao valor no código de barras. Isso pode indicar um problema.");
        }

        // NOVO PASSO: Pedir CNPJ e realizar consulta
        System.out.println("\n--- Verificação do CNPJ ---");
        System.out.println("Por favor, informe o CNPJ do beneficiário (somente números):");
        String cnpjInformado = scanner.nextLine().trim().replaceAll("[^0-9]", "");

        if (cnpjInformado.length() == 14) {
            System.out.println("🌐 Consultando CNPJ na BrasilAPI...");
            // Criar um objeto Boleto temporário para a consulta CNPJ
            Boleto boletoTemp = new Boleto(); 
            boletoTemp.setCnpjEmitente(cnpjInformado);
            ConsultaCNPJ consultaCnpj = new ConsultaCNPJ(boletoTemp);
            String statusConsultaCnpj = consultaCnpj.validarDadosComApi();
            System.out.println("ℹ️ Status da validação do CNPJ: " + statusConsultaCnpj);
        } else {
            System.out.println("⚠️ CNPJ inválido. A consulta não será realizada.");
        }

        // NOVO PASSO: Extrair 3 primeiros dígitos e realizar consulta de banco
        System.out.println("\n--- Verificação do Banco Emissor ---");
        String codigoBanco = linhaDigital.substring(0, 3); // Extrai os 3 primeiros dígitos
        System.out.println("🏦 Código do banco extraído da linha digital: " + codigoBanco);

        // Criar um objeto Boleto temporário para a consulta Banco
        Boleto boletoTempBanco = new Boleto();
        boletoTempBanco.setCodigoBarras(linhaDigital); // O ConsultaBanco pode precisar da linha toda
        ConsultaBanco consultaBanco = new ConsultaBanco(boletoTempBanco);
        String statusConsultaBanco = consultaBanco.validarBancoComApi();
        System.out.println("ℹ️ Status da validação do banco: " + statusConsultaBanco);

        System.out.println("\nProcessamento da linha digital concluído.");
    }

    private BigDecimal extrairValorDoCodigoBarras(String codigoBarras) {
        if (codigoBarras == null || codigoBarras.length() < 10) {
            return BigDecimal.ZERO;
        }
        String valorStr = codigoBarras.substring(codigoBarras.length() - 10);
        try {
            String valorFormatado = valorStr.substring(0, 8) + "." + valorStr.substring(8, 10);
            return new BigDecimal(valorFormatado);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}