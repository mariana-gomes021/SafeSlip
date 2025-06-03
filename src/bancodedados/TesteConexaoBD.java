    /*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bancodedados;

import java.sql.*;

/**
 *
 * @author luara
 */
public class TesteConexaoBD {

    public static void main(String[] args) {
        
        try (Connection conexao = ConexaoBD.getConexao()) {
            if (conexao != null) {
                System.out.println("Conexão bem-sucedida!");
            }
        } catch (SQLException e) {
            System.err.println("Erro na conexão: " + e.getMessage());
        }
    }
}
