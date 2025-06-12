/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bancodedados;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class RepositorioCnpjReputacao {

    // Limite de atualizações para um boleto antes de aplicar penalidade na reputação do CNPJ
    private static final int LIMITE_ATUALIZACOES_PARA_PENALIDADE = 3;

    /**
     * Atualiza a reputação de um CNPJ na tabela CNPJ_Reputacao.
     * Incrementa o total de boletos processados para o CNPJ e, se for uma falha,
     * incrementa o total de denúncias. Recalcula o score de reputação.
     * Também penaliza o score se o boleto tiver muitas atualizações.
     *
     * @param cnpj O CNPJ para o qual a reputação será atualizada.
     * @param isBoletoFalho Indica se o boleto associado a este CNPJ resultou em uma falha de validação.
     * @param boletoTotalAtualizacoes O número total de vezes que este boleto específico foi atualizado.
     * @throws SQLException Se ocorrer um erro no acesso ao banco de dados.
     */
    public void atualizarReputacaoCnpj(String cnpj, boolean isBoletoFalho, int boletoTotalAtualizacoes) throws SQLException {
        String checkSql = "SELECT score_reputacao, total_boletos, total_suspeitas FROM CNPJ_Reputacao WHERE cnpj = ?";
        String insertSql = "INSERT INTO CNPJ_Reputacao (cnpj, score_reputacao, total_boletos, total_suspeitas ultima_atualizacao) VALUES (?, ?, ?, ?, ?)";
        String updateSql = "UPDATE CNPJ_Reputacao SET score_reputacao = ?, total_boletos = ?, total_suspeitas= ?, ultima_atualizacao = ? WHERE cnpj = ?";

        try (Connection conexao = ConexaoBD.getConexao()) {
            int totalBoletos = 0;
            int totalDenuncias = 0;
            BigDecimal scoreReputacao = new BigDecimal("100.00"); // Padrão inicial

            // 1. Verificar se o CNPJ já existe na tabela de reputação
            try (PreparedStatement checkStmt = conexao.prepareStatement(checkSql)) {
                checkStmt.setString(1, cnpj);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    scoreReputacao = rs.getBigDecimal("score_reputacao"); // Pega o score existente para ajustar
                    totalBoletos = rs.getInt("total_boletos");
                    totalDenuncias = rs.getInt("total_suspeitas");
                }
            }

            // 2. Atualizar contadores
            totalBoletos++; // Sempre incrementa o total de boletos processados
            if (isBoletoFalho) {
                totalDenuncias++; // Incrementa denúncias se o boleto foi considerado falho
            }

            // NOVO: Lógica de penalidade baseada no número de atualizações do boleto
            if (boletoTotalAtualizacoes >= LIMITE_ATUALIZACOES_PARA_PENALIDADE) {
                System.out.println("🚨 Boleto com CNPJ '" + cnpj + "' foi atualizado " + boletoTotalAtualizacoes + " vezes. Penalizando reputação!");
                // Penalidade: Reduz o score em X pontos por cada atualização acima do limite.
                // Ou, uma penalidade fixa, ou adiciona às denúncias.
                // Exemplo: penalidade de 5 pontos no score.
                scoreReputacao = scoreReputacao.subtract(new BigDecimal("5.00"));
                if (scoreReputacao.compareTo(BigDecimal.ZERO) < 0) {
                    scoreReputacao = BigDecimal.ZERO; // Garante que o score não seja negativo
                }
            }


            // 3. Recalcular score de reputação (se houve denúncias ou penalidade)
            // A lógica de penalidade acima já ajusta o score diretamente.
            // A lógica original de (corretas / total) * 100 ainda faz sentido
            // se 'totalDenuncias' reflete as falhas e as penalidades por atualização.
            if (totalBoletos > 0) {
                // Cálculo: (Total de Verificações Corretas / Total de Boletos) * 100
                // Verificações Corretas = totalBoletos - totalDenuncias
                BigDecimal verificacoesCorretas = new BigDecimal(totalBoletos - totalDenuncias);
                BigDecimal totalBoletosBd = new BigDecimal(totalBoletos);
                if (totalBoletosBd.compareTo(BigDecimal.ZERO) > 0) { // Evita divisão por zero
                    scoreReputacao = verificacoesCorretas.divide(totalBoletosBd, 2, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100.00"));
                } else {
                    scoreReputacao = new BigDecimal("100.00");
                }
            } else {
                scoreReputacao = new BigDecimal("100.00"); // Se não há boletos, reputação é 100%
            }

            // 4. Inserir ou atualizar no banco de dados
            // Consideramos 'rowsAffected == 0' no update como um sinal para inserir
            int rowsAffected = 0;
            try (PreparedStatement updateStmt = conexao.prepareStatement(updateSql)) {
                updateStmt.setBigDecimal(1, scoreReputacao);
                updateStmt.setInt(2, totalBoletos);
                updateStmt.setInt(3, totalDenuncias);
                updateStmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                updateStmt.setString(5, cnpj);
                rowsAffected = updateStmt.executeUpdate();
            }

            if (rowsAffected == 0) { // Se não encontrou para atualizar, insere
                try (PreparedStatement insertStmt = conexao.prepareStatement(insertSql)) {
                    insertStmt.setString(1, cnpj);
                    insertStmt.setBigDecimal(2, scoreReputacao);
                    insertStmt.setInt(3, totalBoletos);
                    insertStmt.setInt(4, totalDenuncias);
                    insertStmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
                    insertStmt.executeUpdate();
                    System.out.println("✅ Reputação para CNPJ '" + cnpj + "' inserida.");
                }
            } else {
                System.out.println("🔄 Reputação para CNPJ '" + cnpj + "' atualizada.");
            }
        }
    }

    /**
     * Busca os dados de reputação de um CNPJ.
     *
     * @param cnpj O CNPJ a ser buscado.
     * @return Um array de Object contendo [score_reputacao (BigDecimal), total_boletos (int), total_suspeitas (int)]
     * ou null se o CNPJ não for encontrado.
     * @throws SQLException Se ocorrer um erro no acesso ao banco de dados.
     */
    public Object[] buscarReputacaoCnpj(String cnpj) throws SQLException {
        String sql = "SELECT score_reputacao, total_boletos, total_suspeitas FROM CNPJ_Reputacao WHERE cnpj = ?";
        try (Connection conexao = ConexaoBD.getConexao();
             PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, cnpj);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                BigDecimal score = rs.getBigDecimal("score_reputacao");
                int totalBoletos = rs.getInt("total_boletos");
                int totalDenuncias = rs.getInt("total_suspeitas");
                return new Object[]{score, totalBoletos, totalDenuncias};
            }
        }
        return null;
    }
}

