package safeslip;

import Denuncia.Denuncia;
import java.sql.SQLException;
import usuario.Escolha; // Se ainda usa lógica de console, mantenha
import javax.swing.SwingUtilities;
import javax.swing.*;
import java.awt.*;
import controlador.TelaEnvioBoleto;
import controlador.TelaProcessadorPDF;
import controlador.TelaProcessadorLinha; // <--- Importe a TelaProcessadorLinha

public class SafeSlip {

    public static void main(String[] args) {

        // --- Configuração da Interface Gráfica (Swing) ---
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("SafeSlip - Verificador de Boletos");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600); // Tamanho inicial da janela
            frame.setLocationRelativeTo(null); // Centraliza a janela na tela

            CardLayout cardLayout = new CardLayout();
            JPanel mainPanel = new JPanel(cardLayout); // Painel principal que gerencia as telas

            // Instancia as telas passando o CardLayout e o mainPanel
            TelaEnvioBoleto telaEnvioBoleto = new TelaEnvioBoleto(cardLayout, mainPanel);
            TelaProcessadorPDF telaProcessadorPDF = new TelaProcessadorPDF(cardLayout, mainPanel);
            // Crie uma instância da TelaProcessadorLinha
            TelaProcessadorLinha telaProcessadorLinha = new TelaProcessadorLinha(cardLayout, mainPanel); // <--- Nova instância

            // Adiciona as telas ao mainPanel com chaves únicas
            mainPanel.add(telaEnvioBoleto, "telaEnvioBoleto");
            mainPanel.add(telaProcessadorPDF, "processarPdf");
            // Adicione a TelaProcessadorLinha com uma chave
            mainPanel.add(telaProcessadorLinha, "processarLinha"); // <--- Adicionada a nova tela

            frame.add(mainPanel); // Adiciona o painel principal ao frame

            // Mostra a tela inicial (telaEnvioBoleto)
            cardLayout.show(mainPanel, "telaEnvioBoleto");

            frame.setVisible(true);
        });

        // --- Lógica da Interface de Console ---
        // Se você não for mais usar a lógica de console, pode remover este bloco.
        // Se for manter, certifique-se de que não há interação que "prenda" o programa aqui
        // enquanto a GUI está rodando.
        /*Scanner scanner = new Scanner(System.in);

        System.out.println("Seja bem vindo ao SafeSlip, seu verificador de boletos suspeitos!");
        System.out.println("Digite sua escolha (pdf ou linha digitavel):");
        String entrada = scanner.nextLine();

        Escolha escolha = new Escolha();
        escolha.setResposta(entrada, scanner);
        try {
            escolha.escolha(); // <- ESSENCIAL
        } catch (SQLException e) {
            System.out.println("Erro SQL ao processar escolha: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close(); // É boa prática fechar o scanner
        }*/
        
        // A lógica de Denuncia.validarTotalSuspeitas(); continuará a ser executada no console
        // independente da interface gráfica.
        Denuncia denuncia = new Denuncia();
        denuncia.validarTotalSuspeitas();
    }
}