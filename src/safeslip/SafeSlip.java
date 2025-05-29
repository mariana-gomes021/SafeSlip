/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package safeslip;
import extracao.EnvioBoleto;
import java.io.File;

/**
 *
 * @author DELL
 */
public class SafeSlip {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        //Teste envio do boleto
        //teste mariana - develop
        //teste create pr
          System.out.println("===== SafeSlip - Envio de Boleto =====");

        EnvioBoleto envio = new EnvioBoleto();
        File boletoSelecionado = envio.selecionarArquivoPDF();

        if (boletoSelecionado != null) {
            System.out.println("Boleto enviado com sucesso:");
            System.out.println("Nome do arquivo: " + boletoSelecionado.getName());
            System.out.println("Caminho completo: " + boletoSelecionado.getAbsolutePath());
        } else {
            System.out.println("Nenhum boleto foi enviado.");
        }

        System.out.println("======================================");
    }
    
}
