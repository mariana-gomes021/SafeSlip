/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package verificacao;

import bancodedados.ConexaoBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Classe responsável por calcular a reputação de um CNPJ com base
 * nos boletos emitidos e nas denúncias registradas.
 * @author Luara Lima
 */
public class CalculadoraReputacao {
    //Calculadora de reputação do Boleto
    private final ConexaoBD conexao;

    public CalculadoraReputacao(ConexaoBD conexao) {
        this.conexao = conexao;
    }

    /**
     * Calcula a reputação do CNPJ com base em:
     * - total de boletos emitidos
     * - total de denúncias recebidas
     * Também salva ou atualiza os dados na tabela `CNPJ_Reputacao`.
     *
     * @param cnpj CNPJ do emitente
     */
    public void calcularReputacao(String cnpj) {
        String sql = """
                   select count(DISTINCT b.id) as total_boletos,
                   count(DISTINCT b.id) as total_denuncias
                   from Boleto b
                   left join Denuncia d on b.id = d.boleto_id
                   where b.cnpj_emitente = ?
                """;

        try (Connection conn = ConexaoBD.getConexao(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, cnpj);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int totalBoletos = rs.getInt("total_boletos");
                int totalDenuncias = rs.getInt("total_denuncias");

                double reputacao = totalBoletos == 0
                        ? 100.0
                        : (1 - ((double) totalDenuncias / totalBoletos)) * 100.0;

                String tipoReputacao;
                if (reputacao == 0) {
                    tipoReputacao = "Reincidente";
                    System.out.println("⚠ Este CNPJ possui muitas denúncias anteriores. Risco elevado.");
                } else if (reputacao < 50) {
                    tipoReputacao = "Problemático";
                    System.out.println("⚠ Este CNPJ possui muitas denúncias anteriores. Risco elevado.");
                } else if (reputacao <= 80) {
                    tipoReputacao = "Risco Moderado";
                } else {
                    tipoReputacao = "Confiável";
                }


                System.out.println("Total de Boletos: " + totalBoletos);
                System.out.println("Total de Denúncias: " + totalDenuncias);
                System.out.printf("Score de Reputação: %.2f%%\n", reputacao);
                System.out.println("Classificação: " + tipoReputacao);
                System.out.println("Cálculo de reputação concluído para o CNPJ: " + cnpj);


                salvarOuAtualizarReputacao(conn, cnpj, reputacao, totalBoletos, totalDenuncias);
            } else {
                System.out.println("Nenhum boleto encontrado para o CNPJ: " + cnpj);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Salva ou atualiza a reputação do CNPJ na tabela `CNPJ_Reputacao`.
     *
     * @param conn           Conexão ativa com o banco de dados
     * @param cnpj           CNPJ do emitente
     * @param reputacao      Score de reputação
     * @param totalBoletos   Total de boletos emitidos
     * @param totalDenuncias Total de denúncias
     * @throws SQLException se ocorrer erro na execução do SQL
     */
    private void salvarOuAtualizarReputacao(Connection conn, String cnpj, double reputacao, int totalBoletos, int totalDenuncias) throws SQLException {
        String updateSql = """
                    INSERT INTO CNPJ_Reputacao (cnpj, score_reputacao, total_boletos, total_denuncias)
                    VALUES (?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        score_reputacao = VALUES(score_reputacao),
                        total_boletos = VALUES(total_boletos),
                        total_denuncias = VALUES(total_denuncias),
                        ultima_atualizacao = CURRENT_TIMESTAMP
                """;

        try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setString(1, cnpj);
            stmt.setDouble(2, reputacao);
            stmt.setInt(3, totalBoletos);
            stmt.setInt(4, totalDenuncias);
            stmt.executeUpdate();
        }
    }


}
