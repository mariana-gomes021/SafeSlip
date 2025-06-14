package boleto;

import java.math.BigDecimal;
import usuario.Boleto; // Importar Boleto se for usar (neste caso, é usado no validarValor)

public class ValidadorLinhaDigitavel {

    /**
     * Valida a estrutura básica da linha digitável (47 dígitos e apenas números)
     * e também os dígitos verificadores (DVs) dos campos e o DV geral.
     * Esta é a Verificação 1.
     *
     * @param linha A linha digitável limpa (somente números).
     * @return true se o formato básico e todos os DVs estiverem corretos, false caso contrário.
     */
    public static boolean validar(String linha) {
        if (linha == null || linha.length() != 47 || !linha.matches("\\d{47}")) {
            System.err.println("Erro: A linha digitavel deve conter 47 digitos numericos.");
            return false;
        }

        System.out.println("--- Resumo de Validacao dos Digitos Verificadores da Linha Digitavel ---");

        // === Campos ===
        String campo1 = linha.substring(0, 9);
        int dv1 = Character.getNumericValue(linha.charAt(9));
        int dv1Calc = calcularModulo10(campo1);

        String campo2 = linha.substring(10, 20);
        int dv2 = Character.getNumericValue(linha.charAt(20));
        int dv2Calc = calcularModulo10(campo2);

        String campo3 = linha.substring(21, 31);
        int dv3 = Character.getNumericValue(linha.charAt(31));
        int dv3Calc = calcularModulo10(campo3);

        int dvGeral = Character.getNumericValue(linha.charAt(32));
        int dvGeralCalc = calcularDvGeral(linha);

        // === Veredito final ===
        boolean todosValidos = dv1 == dv1Calc && dv2 == dv2Calc && dv3 == dv3Calc && dvGeral == dvGeralCalc;

        if (todosValidos) {
            System.out.println("Todos os Digitos Verificadores estao CORRETOS.");
        } else {
            System.out.println("A linha digitavel contem ERROS nos Digitos Verificadores.");
            // System.out.printf("| Campo | DV Informado | DV Calculado | Válido |\n");
            // System.out.printf("|-------|---------------|---------------|--------|\n");
            // System.out.printf("|   1   | %-13d | %-13d | %s |\n", dv1, dv1Calc, dv1 == dv1Calc ? "✅" : "❌");
            // System.out.printf("|   2   | %-13d | %-13d | %s |\n", dv2, dv2Calc, dv2 == dv2Calc ? "✅" : "❌");
            // System.out.printf("|   3   | %-13d | %-13d | %s |\n", dv3, dv3Calc, dv3 == dv3Calc ? "✅" : "❌");
            // System.out.printf("| Geral | %-13d | %-13d | %s |\n", dvGeral, dvGeralCalc, dvGeral == dvGeralCalc ? "✅" : "❌");
        }

        return todosValidos;
    }

    /**
     * Compara o valor informado pelo usuário com o valor extraído da linha digitável.
     * Esta é a Verificação 2.
     *
     * @param valorInformadoPeloUsuario O valor do boleto informado pelo usuário.
     * @param linhaDigital A linha digitável limpa (somente números).
     * @return true se os valores coincidirem, false caso contrário.
     */
    public static boolean validarValor(BigDecimal valorInformadoPeloUsuario, String linhaDigital) {
        BigDecimal valorDoCodigoBarras = BigDecimal.ZERO; // Valor padrão em caso de erro

        if (linhaDigital == null || linhaDigital.length() < 10) {
            System.err.println("Erro: Linha digitavel muito curta para extrair o valor.");
            return false;
        }

        try {
            // Os últimos 10 dígitos da linha digitável representam o valor (8 inteiros + 2 decimais)
            String valorStr = linhaDigital.substring(linhaDigital.length() - 10);
            String valorFormatado = valorStr.substring(0, 8) + "." + valorStr.substring(8, 10);
            valorDoCodigoBarras = new BigDecimal(valorFormatado);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            System.err.println("Erro ao extrair valor do codigo de barras da linha digitavel: " + e.getMessage());
            return false;
        }

        System.out.println("\n--- Verificacao do Valor Final ---");
        System.out.println("Valor informado pelo usuario: " + valorInformadoPeloUsuario);
        System.out.println("Valor extraido da linha digitavel: " + valorDoCodigoBarras);

        // Compara os valores
        if (valorInformadoPeloUsuario.compareTo(valorDoCodigoBarras) == 0) {
            System.out.println(" O valor informado corresponde ao valor na linha digitavel. Verificacao de valor OK.");
            return true;
        } else {
            System.out.println("ATENCAO: O valor informado NAO corresponde ao valor na linha digitavel. Isso pode indicar um problema.");
            return false;
        }
    }

    private static int calcularModulo10(String num) {
        int soma = 0;
        int peso = 2;
        for (int i = num.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(num.charAt(i)) * peso;
            if (n > 9) n = (n / 10) + (n % 10);
            soma += n;
            peso = (peso == 2) ? 1 : 2;
        }
        int resto = soma % 10;
        return (resto == 0) ? 0 : (10 - resto);
    }

    private static int calcularDvGeral(String linha) {
        String banco = linha.substring(0, 3);
        String moeda = linha.substring(3, 4);
        String fatorVencimento = linha.substring(33, 37);
        String valor = linha.substring(37, 47);
        String campoLivre = linha.substring(4, 9) + linha.substring(10, 20) + linha.substring(21, 31);

        String codigoSemDvGeral = banco + moeda + fatorVencimento + valor + campoLivre;

        return calcularModulo11(codigoSemDvGeral);
    }

    private static int calcularModulo11(String num) {
        int soma = 0;
        int peso = 2;
        for (int i = num.length() - 1; i >= 0; i--) {
            int dig = Character.getNumericValue(num.charAt(i));
            soma += dig * peso;
            peso++;
            if (peso > 9) peso = 2;
        }
        int resto = soma % 11;
        if (resto == 0 || resto == 1 || resto == 10) {
            return 1;
        } else {
            return 11 - resto;
        }
    }
}