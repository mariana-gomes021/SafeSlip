package safeslip;

import boleto.*;
import boleto.extracao.ExtracaoBoleto;
import java.io.File;
import java.io.IOException;
import usuario.Usuario;

public class SafeSlip {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        //TODO code application logic here
        // Teste envio do boleto
        // teste
        System.out.println("===== SafeSlip - Envio de Boleto =====");

        Usuario usuario = new Usuario();
        usuario.enviarBoleto();
        usuario.visualizarEConfirmarDadosPdf();

        System.out.println("======================================");;
//        ExtracaoBoleto extracaoBoleto = new ExtracaoBoleto();
//        extracaoBoleto.processarTxt();
    }

}
