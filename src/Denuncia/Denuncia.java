package Denuncia;

import bancodedados.ConexaoBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class Denuncia {

    private String cnpjESuspeitas = "SELECT cnpj, total_suspeitas, ultima_atualizacao FROM CNPJ_Reputacao ORDER BY "
                                    + "ultima_atualizacao DESC LIMIT 1;";
    private String cnpj;
    private int totalSuspeitas = 0;
    private Date ultimaAtualizacao;

    private void obterCnpjESuspeitas() {

        try (Connection conexao = ConexaoBD.getConexao(); 
                PreparedStatement stmt = conexao.prepareStatement(cnpjESuspeitas); 
                ResultSet rs = stmt.executeQuery()
                ) {

            if (rs.next()) {
                cnpj = rs.getString("cnpj");
                totalSuspeitas = rs.getInt("total_suspeitas");
                ultimaAtualizacao = rs.getDate("ultima_atualizacao");
            }
            
        } catch (SQLException e) {
            System.err.println("Erro ao consultar o banco: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void validarTotalSuspeitas (){
        obterCnpjESuspeitas();
        
        if(totalSuspeitas > 0){
            EnviarEmail enviarEmail = new EnviarEmail();
            enviarEmail.enviar(cnpj, totalSuspeitas, ultimaAtualizacao);
        }
    }
}
