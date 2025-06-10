package bancodedados;

import usuario.Boleto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Timestamp; // Importe Timestamp para data_extracao (se for DATETIME no banco)

public class RepositorioBoleto {

    public boolean inserirBoleto(Boleto boleto) throws SQLException {
        // SQL para verificar a existência do boleto e seus status atuais
        String checkSql = "SELECT status_validacao, status_validacao_banco, informacoes_confirmadas_usuario FROM Boleto WHERE codigo_barras = ?";
        
        // SQL para INSERIR um novo boleto, incluindo as novas colunas
        String insertSql = "INSERT INTO Boleto (" +
                           "valor, vencimento, cnpj_emitente, nome_beneficiario, " +
                           "banco_emissor, codigo_barras, data_extracao, status_validacao, " +
                           "status_validacao_banco, informacoes_confirmadas_usuario, usuario_id, " + // Novas colunas
                           "denunciado, nome_cnpj_receita" + // Colunas existentes que não estavam no seu insert original
                           ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"; // Total de 13 (?) parâmetros agora
        
        // SQL para ATUALIZAR um boleto existente (se necessário, com todos os novos status)
        String updateSql = "UPDATE Boleto SET " +
                           "valor = ?, vencimento = ?, cnpj_emitente = ?, nome_beneficiario = ?, " +
                           "banco_emissor = ?, data_extracao = ?, status_validacao = ?, " +
                           "status_validacao_banco = ?, informacoes_confirmadas_usuario = ?, usuario_id = ?, " + // Novas colunas
                           "denunciado = ?, nome_cnpj_receita = ? " + // Colunas existentes
                           "WHERE codigo_barras = ?"; // Condição de atualização

        try (Connection conexao = ConexaoBD.getConexao()) {
            // Primeiro, verifica se o boleto já existe
            try (PreparedStatement checkStmt = conexao.prepareStatement(checkSql)) {
                checkStmt.setString(1, boleto.getCodigoBarras());
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    // Boleto com este código de barras já existe
                    String statusValidacaoExistente = rs.getString("status_validacao");
                    String statusValidacaoBancoExistente = rs.getString("status_validacao_banco"); // Pega o status do banco existente
                    boolean infoConfirmadasExistente = rs.getBoolean("informacoes_confirmadas_usuario"); // Pega a confirmação existente

                    System.out.println("⚠️ Boleto com código de barras '" + boleto.getCodigoBarras() + "' já existe no banco de dados.");
                    System.out.println("   Status de Validação CNPJ atual: '" + statusValidacaoExistente + "'.");
                    System.out.println("   Status de Validação Banco atual: '" + statusValidacaoBancoExistente + "'.");
                    System.out.println("   Informações Confirmadas pelo Usuário (atual): " + infoConfirmadasExistente + ".");

                    // Decidir se precisa atualizar (se algum dos status mudou)
                     boolean precisaAtualizar = 
                        !statusValidacaoExistente.equals(boleto.getStatusValidacao()) ||
                        // Corrigindo a comparação para statusValidacaoBancoExistente
                        (statusValidacaoBancoExistente == null && boleto.getStatusValidacaoBanco() != null) || // Era null e agora não é
                        (statusValidacaoBancoExistente != null && !statusValidacaoBancoExistente.equals(boleto.getStatusValidacaoBanco())) || // Não era null e é diferente
                        infoConfirmadasExistente != boleto.isInformacoesConfirmadasPeloUsuario();

                    if (precisaAtualizar) {
                        System.out.println("   Atualizando informações do boleto existente.");
                        try (PreparedStatement updateStmt = conexao.prepareStatement(updateSql)) {
                            // Parâmetros para UPDATE
                            int i = 1;
                            updateStmt.setBigDecimal(i++, boleto.getValor());
                            updateStmt.setDate(i++, (boleto.getVencimento() != null) ? Date.valueOf(boleto.getVencimento()) : null);
                            updateStmt.setString(i++, boleto.getCnpjEmitente());
                            updateStmt.setString(i++, boleto.getNomeBeneficiario());
                            updateStmt.setString(i++, boleto.getBancoEmissor());
                            // data_extracao é DATETIME no seu SQL, então use Timestamp
                            updateStmt.setTimestamp(i++, (boleto.getDataExtracao() != null) ? Timestamp.valueOf(boleto.getDataExtracao()) : null);
                            updateStmt.setString(i++, boleto.getStatusValidacao() != null ? boleto.getStatusValidacao() : "PENDENTE");
                            updateStmt.setString(i++, boleto.getStatusValidacaoBanco() != null ? boleto.getStatusValidacaoBanco() : "PENDENTE"); // Novo
                            updateStmt.setBoolean(i++, boleto.isInformacoesConfirmadasPeloUsuario()); // Novo
                            
                            if (boleto.getUsuarioId() > 0) {
                                updateStmt.setInt(i++, boleto.getUsuarioId());
                            } else {
                                updateStmt.setNull(i++, java.sql.Types.INTEGER);
                            }
                            updateStmt.setBoolean(i++, boleto.isDenunciado()); // Campo existente
                            updateStmt.setString(i++, boleto.getNomeCnpjReceita()); // Campo existente

                            updateStmt.setString(i++, boleto.getCodigoBarras()); // WHERE clause

                            updateStmt.executeUpdate();
                            System.out.println("   Boleto existente atualizado com sucesso!");
                        }
                    } else {
                        System.out.println("   Nenhuma atualização necessária para o boleto existente.");
                    }
                    return true; // Boleto já está no banco (ou foi atualizado)
                }
            }

            // Se o boleto não existe, procede com a inserção
            try (PreparedStatement insertStmt = conexao.prepareStatement(insertSql)) {
                int i = 1; // Contador para os parâmetros
                insertStmt.setBigDecimal(i++, boleto.getValor());
                insertStmt.setDate(i++, (boleto.getVencimento() != null) ? Date.valueOf(boleto.getVencimento()) : null);
                insertStmt.setString(i++, boleto.getCnpjEmitente());
                insertStmt.setString(i++, boleto.getNomeBeneficiario());
                insertStmt.setString(i++, boleto.getBancoEmissor());
                insertStmt.setString(i++, boleto.getCodigoBarras());
                // data_extracao é DATETIME no seu SQL, então use Timestamp
                insertStmt.setTimestamp(i++, (boleto.getDataExtracao() != null) ? Timestamp.valueOf(boleto.getDataExtracao()) : null);
                insertStmt.setString(i++, boleto.getStatusValidacao() != null ? boleto.getStatusValidacao() : "PENDENTE"); // Usa o status do boleto, ou 'PENDENTE' como fallback
                insertStmt.setString(i++, boleto.getStatusValidacaoBanco() != null ? boleto.getStatusValidacaoBanco() : "PENDENTE"); // ** NOVO CAMPO **
                insertStmt.setBoolean(i++, boleto.isInformacoesConfirmadasPeloUsuario()); // ** NOVO CAMPO **
                
                if (boleto.getUsuarioId() > 0) {
                    insertStmt.setInt(i++, boleto.getUsuarioId());
                } else {
                    insertStmt.setNull(i++, java.sql.Types.INTEGER);
                }
                insertStmt.setBoolean(i++, boleto.isDenunciado()); // Adicionado
                insertStmt.setString(i++, boleto.getNomeCnpjReceita()); // Adicionado

                int linhasAfetadas = insertStmt.executeUpdate();
                return linhasAfetadas > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erro ao inserir ou verificar boleto no banco de dados: " + e.getMessage());
            e.printStackTrace(); // Para ver o stack trace completo do erro
            throw e; // Re-lança a exceção para que ela seja tratada em ProcessadorBoleto
        }
    }
}