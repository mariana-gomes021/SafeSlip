package verificacao;

import bancodedados.ConexaoBD;

public class TesteCalculadoraReputacao {
    public static void main(String[] args) {
        // CNPJ que você quer testar. Certifique-se de que ele exista nas tabelas.
        String cnpjTeste = "22334455000166";

        // Instancia a classe CalculadoraReputacao
        CalculadoraReputacao calculadora = new CalculadoraReputacao(new ConexaoBD());

        // Chama o método para calcular a reputação
        calculadora.calcularReputacao(cnpjTeste);

    }
}
