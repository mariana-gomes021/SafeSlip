/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bancodedados;

import java.sql.*;

/**
 *
 * @author DELL
 */
public class ConexaoBD {

    // private - encapsulamento, evitando que outras classes acessem ou modifiquem
    // static - pertence à classe, unica
    // final  constante, não pode ser altearada
    private static final String URL = "jdbc:mysql://localhost:3306/SAFESLIP";
    private static final String USUARIO = "root";
    private static final String SENHA = "12345678"; // colocar sua senha do root

    public static Connection getConexao() throws SQLException {
        try {   
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USUARIO, SENHA);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver JDBC não encontrado.", e);
        }
    }

}
