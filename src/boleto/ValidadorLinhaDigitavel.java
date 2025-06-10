package boleto;

public class ValidadorLinhaDigitavel {

    public static boolean validar(String linha) {
        if (linha == null || linha.length() != 47 || !linha.matches("\\d{47}")) {
            System.out.println("❌ Linha digitável inválida: Deve ter 47 dígitos numéricos.");
            return false;
        }

        System.out.println("--- 🧾 Resumo de Validação da Linha Digitável ---");

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

        // === Impressão do resumo ===
        System.out.printf("| Campo | DV Informado | DV Calculado | Válido |\n");
        System.out.printf("|-------|---------------|---------------|--------|\n");
        System.out.printf("|  1    | %-13d | %-13d | %s |\n", dv1, dv1Calc, dv1 == dv1Calc ? "✅" : "❌");
        System.out.printf("|  2    | %-13d | %-13d | %s |\n", dv2, dv2Calc, dv2 == dv2Calc ? "✅" : "❌");
        System.out.printf("|  3    | %-13d | %-13d | %s |\n", dv3, dv3Calc, dv3 == dv3Calc ? "✅" : "❌");
        System.out.printf("| Geral | %-13d | %-13d | %s |\n", dvGeral, dvGeralCalc, dvGeral == dvGeralCalc ? "✅" : "❌");

        // === Veredito final ===
        boolean todosValidos = dv1 == dv1Calc && dv2 == dv2Calc && dv3 == dv3Calc && dvGeral == dvGeralCalc;

        if (todosValidos) {
            System.out.println("\n✅ Todos os Dígitos Verificadores estão CORRETOS.");
        } else {
            System.out.println("\n❌ A linha digitável contém ERROS nos DVs.");
        }

        return todosValidos;
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
