package bancodedados;

import usuario.Boleto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date; 

public class RepositorioBoleto {

    public boolean inserirBoleto(Boleto boleto) throws SQLException {
        String checkSql = "SELECT status_validacao FROM boleto WHERE codigo_barras = ?";
        String insertSql = "INSERT INTO boleto (valor, vencimento, cnpj_emitente, nome_beneficiario, " +
                           "banco_emissor, codigo_barras, data_extracao, status_validacao, usuario_id) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        String updateStatusSql = "UPDATE boleto SET status_validacao = ? WHERE codigo_barras = ?"; // Para atualizar status de boletos existentes

        try (Connection conexao = ConexaoBD.getConexao()) {
            // Primeiro, verifica se o boleto já existe
            try (PreparedStatement checkStmt = conexao.prepareStatement(checkSql)) {
                checkStmt.setString(1, boleto.getCodigoBarras());
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    // Boleto com este código de barras já existe
                    String statusExistente = rs.getString("status_validacao");
                    System.out.println("⚠️ Boleto com código de barras '" + boleto.getCodigoBarras() + "' já existe no banco de dados.");
                    System.out.println("   Status de validação atual: '" + statusExistente + "'.");
                    
                    // Opcional: Se o boleto já existe, e o novo status de validação é diferente do existente, você pode atualizá-lo.
                    // Isso é útil se você quiser que uma re-validação sobrescreva o status anterior.
                    if (!statusExistente.equals(boleto.getStatusValidacao())) {
                        System.out.println("   Atualizando status de validação para: '" + boleto.getStatusValidacao() + "'.");
                        try (PreparedStatement updateStmt = conexao.prepareStatement(updateStatusSql)) {
                            updateStmt.setString(1, boleto.getStatusValidacao());
                            updateStmt.setString(2, boleto.getCodigoBarras());
                            updateStmt.executeUpdate();
                        }
                    }
                    return true; // Considera como "sucesso" porque o objetivo de estar no banco foi atingido
                }
            }

            // Se não existe, procede com a inserção
            try (PreparedStatement insertStmt = conexao.prepareStatement(insertSql)) {
                Date sqlVencimento = (boleto.getVencimento() != null) ? Date.valueOf(boleto.getVencimento()) : null;
                Date sqlDataExtracao = (boleto.getDataExtracao() != null) ? Date.valueOf(boleto.getDataExtracao().toLocalDate()) : null;


                insertStmt.setBigDecimal(1, boleto.getValor());
                insertStmt.setDate(2, sqlVencimento);
                insertStmt.setString(3, boleto.getCnpjEmitente());
                insertStmt.setString(4, boleto.getNomeBeneficiario());
                insertStmt.setString(5, boleto.getBancoEmissor());
                insertStmt.setString(6, boleto.getCodigoBarras());
                insertStmt.setDate(7, sqlDataExtracao);
                insertStmt.setString(8, boleto.getStatusValidacao() != null ? boleto.getStatusValidacao() : "PENDENTE"); // Usa o status do boleto, ou 'PENDENTE' como fallback
                
                if (boleto.getUsuarioId() > 0) {
                    insertStmt.setInt(9, boleto.getUsuarioId());
                } else {
                    insertStmt.setNull(9, java.sql.Types.INTEGER); 
                }

                int linhasAfetadas = insertStmt.executeUpdate();
                return linhasAfetadas > 0;
            }
        }
    }
}