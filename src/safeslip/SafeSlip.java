package safeslip; 

import boleto.ProcessadorBoleto;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Scanner;

public class SafeSlip { 

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        Scanner scanner = new Scanner(System.in); // Cria o Scanner uma vez

        try {
            // Instancia o processador de boleto, passando o scanner
            ProcessadorBoleto processador = new ProcessadorBoleto(scanner);
            
            // Chama o método que contém toda a lógica
            processador.processarNovoBoleto();

        } catch (IOException e) {
            System.err.println("🚨 Erro de E/S: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("🚨 Erro ao interagir com o banco de dados: " + e.getMessage());
            e.printStackTrace(); // Para depuração
        } finally {
            if (scanner != null) {
                scanner.close(); // Fecha o scanner no final
            }
            long endTime = System.currentTimeMillis();
            long totalTimeSeconds = (endTime - startTime) / 1000;
            System.out.println("\n--- Construção concluída ---");
            System.out.println("Total de tempo: " + totalTimeSeconds + " segundos");
        }
    }
}