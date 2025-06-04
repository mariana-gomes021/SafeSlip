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

    public static boolean validar(String codigo) {
        if (codigo == null || !codigo.matches("\\d{44}")) {
            System.out.println("❌ Código inválido: não possui 44 dígitos numéricos.");
            return false;
        }

        try {
            int dvInformado = Character.getNumericValue(codigo.charAt(4));
            String codigoSemDV = codigo.substring(0, 4) + codigo.substring(5);
            int dvCalculado = calcularDVMod11(codigoSemDV);

            System.out.println("DV informado:   " + dvInformado);
            System.out.println("DV calculado:   " + dvCalculado);

            return dvCalculado == dvInformado;
        } catch (Exception e) {
            System.out.println("❌ Erro ao validar: " + e.getMessage());
            return false;
        }
    }

    private static int calcularDVMod11(String numero) {
        int soma = 0;
        int peso = 2;

        for (int i = numero.length() - 1; i >= 0; i--) {
            int num = Character.getNumericValue(numero.charAt(i));
            soma += num * peso;
            peso = (peso == 9) ? 2 : peso + 1;
        }

        int resto = soma % 11;
        int dv = 11 - resto;

        if (dv == 0 || dv == 10 || dv == 11) return 1;
        return dv;
    }
}


