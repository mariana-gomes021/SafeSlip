package safeslip;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Scanner;
import boleto.ProcessadorBoleto;

public class SafeSlip {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in); // Cria o scanner para leitura de entrada
        ProcessadorBoleto processador = new ProcessadorBoleto(scanner); // Instancia o processador de boleto

        System.out.println("🚀 Iniciando o SafeSlip - Processador de Boletos.");

        try {
            // Chama o método principal de processamento de boletos
            processador.processarNovoBoleto(); 
        } catch (IOException e) {
            System.err.println("❌ Erro de I/O durante o processamento: " + e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("❌ Erro de banco de dados durante o processamento: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close(); // Sempre fechar o scanner para evitar vazamento de recursos
            System.out.println("\n✅ Processamento do boleto concluído. SafeSlip encerrado.");
        }
    }
}