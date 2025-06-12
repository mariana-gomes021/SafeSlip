package bancodedados;

import usuario.Boleto;
import usuario.Usuario; // Assumindo que a classe Usuario existe

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.math.BigDecimal; // Importar BigDecimal
import java.time.LocalDate; // Importar LocalDate

public class RepositorioBoleto {

    public boolean inserirBoleto(Boleto boleto) throws SQLException {
        // SQL para verificar a existência do boleto e seus status atuais
        // Atualizado: 'suspeito' no lugar de 'denunciado', adicionado 'total_atualizacoes'
        String checkSql = "SELECT status_validacao, status_validacao_banco, informacoes_confirmadas_pelo_usuario, " +
                          "valor, vencimento, cnpj_emitente, nome_beneficiario, banco_emissor, " +
                          "suspeito, usuario_id, " + // 'suspeito' aqui
                          "nome_banco_api, nome_completo_banco_api, ispb_banco_api, razao_social_api, nome_fantasia_api, " +
                          "total_atualizacoes " + // 'total_atualizacoes' aqui
                          "FROM Boleto WHERE codigo_barras = ?";
        
        // SQL para INSERIR um novo boleto, incluindo as novas colunas da API
        // Atualizado: 'suspeito' no lugar de 'denunciado', adicionado 'total_atualizacoes'
        String insertSql = "INSERT INTO Boleto (" +
                           "valor, vencimento, cnpj_emitente, nome_beneficiario, " +
                           "banco_emissor, codigo_barras, data_extracao, status_validacao, " +
                           "status_validacao_banco, informacoes_confirmadas_pelo_usuario, usuario_id, " +
                           "suspeito, " + // 'suspeito' aqui
                           "nome_banco_api, nome_completo_banco_api, ispb_banco_api, razao_social_api, nome_fantasia_api, total_atualizacoes" + // 'total_atualizacoes' aqui
                           ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"; // Total de 18 parâmetros

        // SQL para ATUALIZAR um boleto existente (com todos os novos status e dados de API)
        // Atualizado: 'suspeito' no lugar de 'denunciado', incrementa 'total_atualizacoes'
        String updateSql = "UPDATE Boleto SET " +
                           "valor = ?, vencimento = ?, cnpj_emitente = ?, nome_beneficiario = ?, " +
                           "banco_emissor = ?, data_extracao = ?, status_validacao = ?, " +
                           "status_validacao_banco = ?, informacoes_confirmadas_pelo_usuario = ?, usuario_id = ?, " +
                           "suspeito = ?, " + // 'suspeito' aqui
                           "nome_banco_api = ?, nome_completo_banco_api = ?, ispb_banco_api = ?, razao_social_api = ?, nome_fantasia_api = ?, " +
                           "total_atualizacoes = total_atualizacoes + 1 " + // NOVO: Incrementa o contador
                           "WHERE codigo_barras = ?";

        try (Connection conexao = ConexaoBD.getConexao()) {
            // Primeiro, verifica se o boleto já existe
            try (PreparedStatement checkStmt = conexao.prepareStatement(checkSql)) {
                checkStmt.setString(1, boleto.getCodigoBarras());
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    // Boleto com este código de barras já existe
                    String statusValidacaoExistente = rs.getString("status_validacao");
                    String statusValidacaoBancoExistente = rs.getString("status_validacao_banco");
                    boolean infoConfirmadasExistente = rs.getBoolean("informacoes_confirmadas_pelo_usuario");
                    
                    // Dados adicionais para comparação de atualização
                    BigDecimal valorExistente = rs.getBigDecimal("valor");
                    LocalDate vencimentoExistente = rs.getDate("vencimento") != null ? rs.getDate("vencimento").toLocalDate() : null;
                    String cnpjEmitenteExistente = rs.getString("cnpj_emitente");
                    String nomeBeneficiarioExistente = rs.getString("nome_beneficiario");
                    String bancoEmissorExistente = rs.getString("banco_emissor");
                    String nomeBancoApiExistente = rs.getString("nome_banco_api");
                    String nomeCompletoBancoApiExistente = rs.getString("nome_completo_banco_api");
                    String ispbBancoApiExistente = rs.getString("ispb_banco_api");
                    String razaoSocialApiExistente = rs.getString("razao_social_api");
                    String nomeFantasiaApiExistente = rs.getString("nome_fantasia_api");
                    boolean suspeitoExistente = rs.getBoolean("suspeito"); // NOVO: Obtém o status 'suspeito' existente
                    int totalAtualizacoesExistente = rs.getInt("total_atualizacoes"); // NOVO: Pega o valor atual

                    // Define o totalAtualizacoes no objeto boleto para que possa ser usado na reputação
                    boleto.setTotalAtualizacoes(totalAtualizacoesExistente); 

                    System.out.println("⚠️ Boleto com código de barras '" + boleto.getCodigoBarras() + "' já existe no banco de dados.");
                    System.out.println("   Status de Validação CNPJ atual: '" + statusValidacaoExistente + "'.");
                    System.out.println("   Status de Validação Banco atual: '" + statusValidacaoBancoExistente + "'.");
                    System.out.println("   Informações Confirmadas pelo Usuário (atual): " + infoConfirmadasExistente + ".");
                    System.out.println("   Status Suspeito (atual): " + suspeitoExistente + "."); // NOVO: Exibe o status suspeito
                    System.out.println("   Total de Atualizações: " + totalAtualizacoesExistente + "."); // NOVO: Exibe o contador

                    // Decidir se precisa atualizar (se algum dos status ou informações mudou)
                    boolean precisaAtualizar =
                        !statusValidacaoExistente.equals(boleto.getStatusValidacao()) ||
                        (statusValidacaoBancoExistente == null && boleto.getStatusValidacaoBanco() != null) ||
                        (statusValidacaoBancoExistente != null && !statusValidacaoBancoExistente.equals(boleto.getStatusValidacaoBanco())) ||
                        infoConfirmadasExistente != boleto.isInformacoesConfirmadasPeloUsuario() ||
                        (boleto.getValorAsBigDecimal() != null && (valorExistente == null || boleto.getValorAsBigDecimal().compareTo(valorExistente) != 0)) || // Usando getValorAsBigDecimal
                        (boleto.getVencimento() != null && (vencimentoExistente == null || !boleto.getVencimento().equals(vencimentoExistente))) ||
                        (boleto.getCnpjEmitente() != null && (cnpjEmitenteExistente == null || !boleto.getCnpjEmitente().equals(cnpjEmitenteExistente))) ||
                        (boleto.getNomeBeneficiario() != null && (nomeBeneficiarioExistente == null || !boleto.getNomeBeneficiario().equals(nomeBeneficiarioExistente))) ||
                        (boleto.getBancoEmissor() != null && (bancoEmissorExistente == null || !boleto.getBancoEmissor().equals(bancoEmissorExistente))) ||
                        (boleto.getNomeBancoApi() != null && (nomeBancoApiExistente == null || !boleto.getNomeBancoApi().equals(nomeBancoApiExistente))) ||
                        (boleto.getNomeCompletoBancoApi() != null && (nomeCompletoBancoApiExistente == null || !boleto.getNomeCompletoBancoApi().equals(nomeCompletoBancoApiExistente))) ||
                        (boleto.getIspbBancoApi() != null && (ispbBancoApiExistente == null || !boleto.getIspbBancoApi().equals(ispbBancoApiExistente))) ||
                        (boleto.getRazaoSocialApi() != null && (razaoSocialApiExistente == null || !boleto.getRazaoSocialApi().equals(razaoSocialApiExistente))) ||
                        (boleto.getNomeFantasiaApi() != null && (nomeFantasiaApiExistente == null || !boleto.getNomeFantasiaApi().equals(nomeFantasiaApiExistente))) ||
                        suspeitoExistente != boleto.isSuspeito(); // NOVO: Compara o status suspeito

                    if (precisaAtualizar) {
                        System.out.println("   Atualizando informações do boleto existente.");
                        try (PreparedStatement updateStmt = conexao.prepareStatement(updateSql)) {
                            int i = 1;
                            updateStmt.setBigDecimal(i++, boleto.getValorAsBigDecimal()); // Usando o getter para BigDecimal
                            updateStmt.setDate(i++, (boleto.getVencimento() != null) ? Date.valueOf(boleto.getVencimento()) : null);
                            updateStmt.setString(i++, boleto.getCnpjEmitente());
                            updateStmt.setString(i++, boleto.getNomeBeneficiario());
                            updateStmt.setString(i++, boleto.getBancoEmissor());
                            updateStmt.setTimestamp(i++, (boleto.getDataExtracao() != null) ? Timestamp.valueOf(boleto.getDataExtracao()) : null);
                            updateStmt.setString(i++, boleto.getStatusValidacao() != null ? boleto.getStatusValidacao() : "PENDENTE");
                            updateStmt.setString(i++, boleto.getStatusValidacaoBanco() != null ? boleto.getStatusValidacaoBanco() : "PENDENTE");
                            updateStmt.setBoolean(i++, boleto.isInformacoesConfirmadasPeloUsuario());
                            
                            if (boleto.getUsuarioId() > 0) {
                                updateStmt.setInt(i++, boleto.getUsuarioId());
                            } else {
                                updateStmt.setNull(i++, java.sql.Types.INTEGER);
                            }
                            updateStmt.setBoolean(i++, boleto.isSuspeito()); // Atualizado: .isSuspeito()

                            // Campos de API
                            updateStmt.setString(i++, boleto.getNomeBancoApi());
                            updateStmt.setString(i++, boleto.getNomeCompletoBancoApi());
                            updateStmt.setString(i++, boleto.getIspbBancoApi());
                            updateStmt.setString(i++, boleto.getRazaoSocialApi());
                            updateStmt.setString(i++, boleto.getNomeFantasiaApi());

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
                insertStmt.setBigDecimal(i++, boleto.getValorAsBigDecimal()); // Usando o getter para BigDecimal
                insertStmt.setDate(i++, (boleto.getVencimento() != null) ? Date.valueOf(boleto.getVencimento()) : null);
                insertStmt.setString(i++, boleto.getCnpjEmitente());
                insertStmt.setString(i++, boleto.getNomeBeneficiario());
                insertStmt.setString(i++, boleto.getBancoEmissor());
                insertStmt.setString(i++, boleto.getCodigoBarras());
                insertStmt.setTimestamp(i++, (boleto.getDataExtracao() != null) ? Timestamp.valueOf(boleto.getDataExtracao()) : null);
                insertStmt.setString(i++, boleto.getStatusValidacao() != null ? boleto.getStatusValidacao() : "PENDENTE");
                insertStmt.setString(i++, boleto.getStatusValidacaoBanco() != null ? boleto.getStatusValidacaoBanco() : "PENDENTE");
                insertStmt.setBoolean(i++, boleto.isInformacoesConfirmadasPeloUsuario());
                
                if (boleto.getUsuarioId() > 0) {
                    insertStmt.setInt(i++, boleto.getUsuarioId());
                } else {
                    insertStmt.setNull(i++, java.sql.Types.INTEGER);
                }
                insertStmt.setBoolean(i++, boleto.isSuspeito()); // Atualizado: .isSuspeito()

                // Novos campos de API
                insertStmt.setString(i++, boleto.getNomeBancoApi());
                insertStmt.setString(i++, boleto.getNomeCompletoBancoApi());
                insertStmt.setString(i++, boleto.getIspbBancoApi());
                insertStmt.setString(i++, boleto.getRazaoSocialApi());
                insertStmt.setString(i++, boleto.getNomeFantasiaApi());
                insertStmt.setInt(i++, 0); // NOVO: total_atualizacoes = 0 na inserção

                int linhasAfetadas = insertStmt.executeUpdate();
                return linhasAfetadas > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erro ao inserir ou verificar boleto no banco de dados: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-lança a exceção para que ela seja tratada em ProcessadorBoleto
        }
    }
}
