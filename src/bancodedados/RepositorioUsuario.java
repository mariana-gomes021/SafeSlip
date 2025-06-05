package bancodedados;

import usuario.Usuario;
import java.sql.*;

public class RepositorioUsuario {

    /**
     * Insere um novo usuário anônimo no banco de dados.
     * O ID será gerado automaticamente pelo banco.
     *
     * @return O objeto Usuario com o ID gerado.
     * @throws SQLException Se ocorrer um erro ao acessar o banco de dados.
     */

    public Usuario criarUsuarioAnonimo() throws SQLException {
        // A SQL para criar usuário anônimo deve ser assim se a tabela Usuario tem só o
        // ID
        String sql = "INSERT INTO Usuario () VALUES ()"; // Inserção de uma linha vazia para auto-incremento
        Usuario novoUsuario = new Usuario(); // Cria um novo objeto Usuario

        try (Connection conexao = ConexaoBD.getConexao();
                // Usa Statement.RETURN_GENERATED_KEYS para obter o ID gerado pelo
                // auto_increment
                PreparedStatement stmt = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            int linhasAfetadas = stmt.executeUpdate();
            if (linhasAfetadas > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        novoUsuario.setId(rs.getInt(1)); // Define o ID gerado no objeto Usuario
                        System.out.println("Novo usuário anônimo criado com ID: " + novoUsuario.getId());
                    } else {
                        System.err.println("Falha ao obter o ID gerado para o novo usuário anônimo.");
                    }
                }
            } else {
                System.err.println("Falha ao criar novo usuário anônimo: nenhuma linha afetada.");
            }

        } catch (SQLException e) {
            System.err.println("Erro ao criar usuário anônimo: " + e.getMessage());
            throw e; // Lançar a exceção para ser tratada na Main
        }
        return novoUsuario;
    }

    /**
     * Busca um usuário pelo ID.
     * 
     * @param id O ID do usuário a ser buscado.
     * @return O objeto Usuario se encontrado, ou null caso contrário.
     * @throws SQLException Se ocorrer um erro ao acessar o banco de dados.
     */
    public Usuario buscarUsuarioPorId(int id) throws SQLException {
        String sql = "SELECT id FROM Usuario WHERE id = ?";
        try (Connection conexao = ConexaoBD.getConexao();
                PreparedStatement stmt = conexao.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Usuario usuario = new Usuario(rs.getInt("id"));
                    return usuario;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar usuário por ID: " + e.getMessage());
            throw e;
        }
        return null;
    }

    // Métodos de atualização ou deleção podem ser adicionados se houver necessidade
    // futura,
    // mas para um usuário anônimo que só serve para contabilizar, não são
    // essenciais.
}