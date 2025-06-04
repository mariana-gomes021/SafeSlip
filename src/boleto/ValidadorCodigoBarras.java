/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package boleto;

/**
 *
 * @author DELL
 */

public class ValidadorCodigoBarras {

    public static boolean validar(String linha) {
        if (linha == null || linha.length() != 47 || !linha.matches("\\d{47}")) {
            return false; // tem que ter 47 dígitos
        }

        // Campos e DVs
        String campo1 = linha.substring(0, 9);
        int dvCampo1Informado = Character.getNumericValue(linha.charAt(9));

        String campo2 = linha.substring(10, 20);
        int dvCampo2Informado = Character.getNumericValue(linha.charAt(20));

        String campo3 = linha.substring(21, 31);
        int dvCampo3Informado = Character.getNumericValue(linha.charAt(31));

        boolean campo1Valido = calcularModulo10(campo1) == dvCampo1Informado;
        boolean campo2Valido = calcularModulo10(campo2) == dvCampo2Informado;
        boolean campo3Valido = calcularModulo10(campo3) == dvCampo3Informado;

        // Se quiser ignorar o DV geral, só validar esses 3:
        return campo1Valido && campo2Valido && campo3Valido;
    }

    private static int calcularModulo10(String num) {
        int soma = 0;
        int peso = 2;
        for (int i = num.length() - 1; i >= 0; i--) {
            int valor = Character.getNumericValue(num.charAt(i)) * peso;
            if (valor > 9) {
                valor = (valor / 10) + (valor % 10);
            }
            soma += valor;
            peso = (peso == 2) ? 1 : 2;
        }
        int resto = soma % 10;
        return (resto == 0) ? 0 : (10 - resto);
    }

}
