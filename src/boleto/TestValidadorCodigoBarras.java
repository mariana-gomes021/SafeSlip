/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package boleto;
import boleto.ValidadorCodigoBarras;
/**
 *
 * @author Casa
 */



public class TestValidadorCodigoBarras {
    public static void main(String[] args) {
        String codigoValido = "12345678901234567890123456789012345678901234"; // código de barras real
        String codigoInvalido = "23793381286007781382396000052803975840000002000";

        System.out.println("🔍 Testando código válido:");
        boolean resultadoValido = ValidadorCodigoBarras.validar(codigoValido);
        System.out.println("Código: " + codigoValido);
        System.out.println("Resultado: " + (resultadoValido ? "✅ VÁLIDO" : "❌ INVÁLIDO"));

        System.out.println("\n🔍 Testando código inválido:");
        boolean resultadoInvalido = ValidadorCodigoBarras.validar(codigoInvalido);
        System.out.println("Código: " + codigoInvalido);
        System.out.println("Resultado: " + (resultadoInvalido ? "✅ VÁLIDO" : "❌ INVÁLIDO"));
    }
}

