package usuario;

// Removidas as importações desnecessárias
// Removidas as dependências de EnvioBoleto e ExtracaoBoleto

public class Usuario {

    private int id;

    // Construtor padrão
    public Usuario() {
    }

    // Construtor para definir o ID, se necessário (ex: ao buscar do BD)
    public Usuario(int id) {
        this.id = id;
    }

    // Getter e Setter para o ID
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    // Métodos de envio e visualização de dados removidos,
    // pois a responsabilidade é do fluxo da aplicação (Main)
    // e não do modelo de dados Usuario.
}