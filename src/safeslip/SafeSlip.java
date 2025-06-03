package safeslip;

import bancodedados.ConexaoBD;
import boleto.*;
import boleto.extracao.ExtracaoBoleto;
import java.io.File;
import java.sql.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
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

        try (Connection conexao = ConexaoBD.getConexao()) {
            if (conexao != null) {
                System.out.println("Conexão bem-sucedida!");
            }
        } catch (SQLException e) {
            System.err.println("Erro na conexão: " + e.getMessage());
        }

    }

}
