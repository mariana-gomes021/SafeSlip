package Denuncia;

import bancodedados.ConexaoBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class Denuncia {

    // A consulta agora seleciona a nova coluna 'ultimo_email_enviado_em_suspeitas'
    private String cnpjESuspeitasQuery = "SELECT cnpj, total_suspeitas, ultima_atualizacao, ultimo_email_enviado_em_suspeitas "
                                         + "FROM CNPJ_Reputacao "
                                         + "ORDER BY ultima_atualizacao DESC LIMIT 1;"; // Ajuste o LIMIT se você quer o CNPJ mais recente, não apenas um
                                                                                      // Se você quer verificar TODOS os CNPJs que possam ter suspeitas para enviar emails, a lógica precisará ser expandida para iterar sobre eles.
                                                                                      // Por enquanto, esta lógica funciona para o *último* CNPJ atualizado.

    private String cnpj;
    private int totalSuspeitas = 0;
    private int ultimoEmailEnviadoEmSuspeitas = 0; // Nova variável para armazenar o último marco de envio
    private Date ultimaAtualizacao;

    // Construtor adicionado para permitir a criação de objetos Denuncia sem obter dados do DB imediatamente
    public Denuncia() {
        // Opcional: pode chamar obterCnpjESuspeitas() aqui se a classe sempre começar com dados
    }

    private void obterCnpjESuspeitas() {
        try (Connection conexao = ConexaoBD.getConexao(); 
             PreparedStatement stmt = conexao.prepareStatement(cnpjESuspeitasQuery); 
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                this.cnpj = rs.getString("cnpj");
                this.totalSuspeitas = rs.getInt("total_suspeitas");
                this.ultimaAtualizacao = rs.getDate("ultima_atualizacao");
                this.ultimoEmailEnviadoEmSuspeitas = rs.getInt("ultimo_email_enviado_em_suspeitas");
            } else {
                // Caso não encontre nenhum registro, para evitar NullPointer para cnpj, etc.
                this.cnpj = null;
                this.totalSuspeitas = 0;
                this.ultimaAtualizacao = null;
                this.ultimoEmailEnviadoEmSuspeitas = 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Erro ao consultar o banco para obter CNPJ e suspeitas: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Novo método para atualizar a coluna 'ultimo_email_enviado_em_suspeitas'
    private void atualizarUltimoEmailEnviadoEmSuspeitas(String cnpj, int novoMarco) {
        String updateQuery = "UPDATE CNPJ_Reputacao SET ultimo_email_enviado_em_suspeitas = ? WHERE cnpj = ?";
        try (Connection conexao = ConexaoBD.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(updateQuery)) {
            
            stmt.setInt(1, novoMarco);
            stmt.setString(2, cnpj);
            stmt.executeUpdate();
            System.out.println("Ultimo marco de e-mail atualizado para o CNPJ " + cnpj + " para " + novoMarco + " suspeitas.");
            
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar o último marco de e-mail para o CNPJ " + cnpj + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void validarTotalSuspeitas() {
        obterCnpjESuspeitas(); // Obtém os dados mais recentes do CNPJ

        // Verifica se há um CNPJ para processar
        if (this.cnpj == null) {
            System.out.println("Nenhum CNPJ encontrado para validação de suspeitas.");
            return;
        }

        // Lógica para enviar e-mail a cada 5 suspeitas progressivamente
        // Exemplo: se totalSuspeitas = 7, ultimoEmailEnviadoEmSuspeitas = 0 -> envia para 5 e atualiza para 5
        // Se totalSuspeitas = 12, ultimoEmailEnviadoEmSuspeitas = 5 -> envia para 10 e atualiza para 10
        // Se totalSuspeitas = 12, ultimoEmailEnviadoEmSuspeitas = 10 -> não faz nada
        
        int proximoMarcoEnvio = (this.ultimoEmailEnviadoEmSuspeitas / 5 + 1) * 5;
        
        if (this.totalSuspeitas >= proximoMarcoEnvio) {
            // Garante que o e-mail não seja enviado para um marco já enviado
            if (proximoMarcoEnvio > this.ultimoEmailEnviadoEmSuspeitas) {
                
                EnviarEmail enviarEmail = new EnviarEmail();
                enviarEmail.enviar(this.cnpj, this.totalSuspeitas, this.ultimaAtualizacao);
                
                // Após o envio, atualiza o banco de dados com o novo marco
                atualizarUltimoEmailEnviadoEmSuspeitas(this.cnpj, proximoMarcoEnvio);
            } else {
                System.out.println("E-mail para " + this.cnpj + " no marco de " + proximoMarcoEnvio + " suspeitas já foi enviado.");
            }
        } else {
            System.out.println("CNPJ " + this.cnpj + " com " + this.totalSuspeitas + " suspeitas. Nenhum e-mail para o próximo marco (" + proximoMarcoEnvio + ") é necessário agora.");
        }
    }
}