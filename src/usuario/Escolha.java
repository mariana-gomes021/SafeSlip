package usuario;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Scanner;

import boleto.ProcessadorBoleto;
import boleto.ProcessadorLinhaDigitavel; // Importe a nova classe

public class Escolha {

    private String resposta;
    private Scanner scanner;

    public void setResposta(String resposta, Scanner scanner) {
        this.resposta = resposta.trim().toLowerCase();
        this.scanner = scanner;
    }

    public void escolha() throws SQLException {
        switch (resposta.toLowerCase()) {
            case "pdf":
                ProcessadorBoleto processador = new ProcessadorBoleto(scanner);
                try {
                    processador.processarNovoBoleto();
                } catch (IOException e) {
                    System.err.println("Erro de I/O durante o processamento: " + e.getMessage());
                    e.printStackTrace();
                } catch (SQLException e) {
                    System.err.println("Erro de banco de dados durante o processamento: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    scanner.close();
                    System.out.println("\n Processamento do boleto concluido. SafeSlip encerrado.");
                }
                break;
            case "linha digitavel":
                ProcessadorLinhaDigitavel processadorLinhaDigitavel = new ProcessadorLinhaDigitavel(scanner);
                processadorLinhaDigitavel.processar();
                scanner.close(); // Fechar o scanner também para esta opção
                System.out.println("\n Processamento da linha digital concluido. SafeSlip encerrado.");
                break;
            default:
                System.out.println("Opcao invalida. Por favor, digite 'pdf' ou 'linha digital'.");
                scanner.close(); // Fechar o scanner para opção inválida também
                break;
        }
    }
}