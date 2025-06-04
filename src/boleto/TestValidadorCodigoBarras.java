/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package boleto;

/**
 *
 * @author Casa
 */
public class TestValidadorCodigoBarras {
    public static void main(String[] args) {
        String linhaDigitavel = "40390000071193607601429450367015910480000350817";

        System.out.println("🔍 Testando linha digitável:");
        boolean resultado = ValidadorCodigoBarras.validar(linhaDigitavel);
        System.out.println("Resultado: " + (resultado ? "✅ VÁLIDO" : "❌ INVÁLIDO"));
    }
}
