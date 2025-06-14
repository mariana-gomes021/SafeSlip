package bancodedados; // Coloque no pacote bancodedados

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class RepositorioCnpjEmitente {

    /**
     * Insere ou atualiza um CNPJ na tabela CNPJ_Emitente.
     * Se o CNPJ já existe, apenas informa. Caso contrário, insere.
     * @param cnpj O CNPJ a ser inserido/verificado.
     * @param nomeRazaoSocial O nome/razão social associado ao CNPJ.
     * @throws SQLException Se ocorrer um erro no acesso ao banco de dados.
     */
    public void inserirOuAtualizarCnpjEmitente(String cnpj, String nomeRazaoSocial) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM CNPJ_Emitente WHERE cnpj = ?";
        String insertSql = "INSERT INTO CNPJ_Emitente (cnpj, nome_razao_social, data_abertura) VALUES (?, ?, ?)";

        try (Connection conexao = ConexaoBD.getConexao()) {
            // Verifica se o CNPJ já existe
            try (PreparedStatement checkStmt = conexao.prepareStatement(checkSql)) {
                checkStmt.setString(1, cnpj);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("CNPJ Emitente '" + cnpj + "' já existe na tabela CNPJ_Emitente.");
                    return; // Sai do método se o CNPJ já existe
                }
            }

            // Se não existe, insere o novo CNPJ
            try (PreparedStatement insertStmt = conexao.prepareStatement(insertSql)) {
                insertStmt.setString(1, cnpj);
                insertStmt.setString(2, nomeRazaoSocial != null && !nomeRazaoSocial.isEmpty() ? nomeRazaoSocial : "Desconhecido");
                insertStmt.setDate(3, Date.valueOf(LocalDate.now())); // Usa a data atual como data de abertura

                int linhasAfetadas = insertStmt.executeUpdate();
                if (linhasAfetadas > 0) {
                    System.out.println("CNPJ Emitente '" + cnpj + "' inserido na tabela CNPJ_Emitente.");
                } else {
                    System.err.println("Falha ao inserir CNPJ Emitente '" + cnpj + "' na tabela CNPJ_Emitente.");
                }
            }
        }
    }
}