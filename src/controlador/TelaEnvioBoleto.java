package controlador; // Verifique se este é o pacote correto

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TelaEnvioBoleto extends JPanel { 

    // Componentes da UI (mantidos do seu initComponents)
    private javax.swing.JButton jButton1; // Botão "Linha Digitável"
    private javax.swing.JButton jButton2; // Botão "PDF"
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;

    // Adicione CardLayout e JPanel para navegação
    private CardLayout cardLayout;
    private JPanel parentContainer;

    // Construtor agora aceita CardLayout e JPanel
    public TelaEnvioBoleto(CardLayout cardLayout, JPanel parentContainer) {
        this.cardLayout = cardLayout;
        this.parentContainer = parentContainer;

        initComponents();

        // Adicionando os ActionListeners aos botões
        // jButton2 é o botão "PDF"
        jButton2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Altera a tela visível no CardLayout para a tela do processador de PDF
                cardLayout.show(parentContainer, "processarPdf"); 
            }
        });

        // jButton1 é o botão "Linha Digitável"
        jButton1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Altera a tela visível no CardLayout para a tela do processador de Linha Digitável
                cardLayout.show(parentContainer, "processarLinha"); // <--- Esta linha é a chave!
            }
        });
    }

    /**
     * Este método é chamado de dentro do construtor para inicializar o formulário.
     * CUIDADO: Não modifique este código. O conteúdo deste método é sempre
     * regenerado pelo Editor de Formulários (como o do NetBeans).
     * Ele agora apenas configura os componentes deste JPanel.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton(); // Botão para Linha Digitável
        jButton2 = new javax.swing.JButton(); // Botão para PDF

        setBackground(new java.awt.Color(242, 235, 235));
        
        // Configuração do layout para este JPanel
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout); 
        
        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setText("Seja Bem-Vindo ao SafeSlip");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel3.setText("Aqui você pode verificar rapidamente a autenticidade de um boleto ");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel4.setText("bancário, seja por arquivo PDF ou digitando a linha digitável.");

        jLabel5.setFont(new java.awt.Font("Segoe UI Black", 1, 14)); // NOI18N
        jLabel5.setText("Escolha o método de verificação abaixo para começar.");

        jButton1.setText("Linha Digitável");

        jButton2.setText("PDF");

        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(48, 48, 48)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 247, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel5)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(33, 33, 33)
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(48, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap(70, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addGap(26, 26, 26)
                .addComponent(jLabel5)
                .addGap(35, 35, 35)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton2)
                    .addComponent(jButton1))
                .addGap(81, 81, 81))
        );
    }// </editor-fold>
}