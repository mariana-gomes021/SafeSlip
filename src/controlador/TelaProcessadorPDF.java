package controlador;

import controlador.ProcessadoresGUI.ProcessadorBoletoGui;
import controlador.ProcessadoresGUI.ProcessamentoCancelado;
import usuario.Boleto;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class TelaProcessadorPDF extends JPanel {

    private JTextArea resultArea;
    private JButton selectPdfButton;
    private JButton homeButton; // Novo botão "Página Inicial"

    private ProcessadorBoletoGui processador;

    // Construtor agora recebe o CardLayout e o container pai
    private CardLayout cardLayout;
    private JPanel parentContainer;

    // Construtor atualizado para aceitar CardLayout e JPanel pai
    public TelaProcessadorPDF(CardLayout cardLayout, JPanel parentContainer) {
        this.cardLayout = cardLayout;
        this.parentContainer = parentContainer;
        this.processador = new ProcessadorBoletoGui();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Processamento de Boletos via PDF", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        add(titleLabel, BorderLayout.NORTH);

        // Painel para os botões na parte inferior
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0)); // Centraliza os botões

        homeButton = new JButton("Página Inicial"); // Inicializa o novo botão
        buttonPanel.add(homeButton); // Adiciona o botão "Página Inicial"
        
        selectPdfButton = new JButton("Selecionar Arquivo PDF");
        buttonPanel.add(selectPdfButton); // Adiciona o botão "Selecionar PDF"

        add(buttonPanel, BorderLayout.SOUTH); // Adiciona o painel de botões à parte inferior

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        add(scrollPane, BorderLayout.CENTER);

        // Ação para o botão "Página Inicial"
        homeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Assume que "telaEnvioBoleto" é o nome da sua TelaEnvioBoleto no CardLayout
                cardLayout.show(parentContainer, "telaEnvioBoleto"); 
                resultArea.setText(""); // Limpa a área de texto ao voltar para a página inicial
            }
        });

        selectPdfButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Selecione o arquivo PDF do boleto");
                int userSelection = fileChooser.showOpenDialog(TelaProcessadorPDF.this);

                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File pdfSelecionado = fileChooser.getSelectedFile();
                    resultArea.setText("Processando arquivo: " + pdfSelecionado.getName() + "\n");
                    Boleto boletoProcessado = null;

                    try {
                        boletoProcessado = processador.processarBoleto(pdfSelecionado);

                        // --- Diálogo de Confirmação dos Dados Extraídos ---
                        StringBuilder details = new StringBuilder();
                        details.append("--- Detalhes do Boleto Extraído para Confirmação ---\n");
                        details.append("Valor: ").append(boletoProcessado.getValorAsBigDecimal() != null ? boletoProcessado.getValorAsBigDecimal() : "Não encontrado").append("\n");
                        details.append("Data de Vencimento: ").append(boletoProcessado.getVencimento() != null ? boletoProcessado.getVencimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Não encontrado").append("\n");
                        details.append("CNPJ do Beneficiário: ").append(boletoProcessado.getCnpjEmitente() != null ? boletoProcessado.getCnpjEmitente() : "Não encontrado").append("\n");
                        details.append("Nome do Beneficiário: ").append(boletoProcessado.getNomeBeneficiario() != null ? boletoProcessado.getNomeBeneficiario() : "Não encontrado").append("\n");
                        details.append("Banco Emissor (extraído do PDF): ").append(boletoProcessado.getBancoEmissor() != null ? boletoProcessado.getBancoEmissor() : "Não encontrado").append("\n");
                        details.append("Número do Código de Barras: ").append(boletoProcessado.getCodigoBarras() != null ? boletoProcessado.getCodigoBarras() : "Não encontrado").append("\n");
                        details.append("--------------------------------------------------\n\n");
                        details.append("As informações do boleto estão corretas?");

                        String[] optionsConfirmacao = {"Sim", "Não"};
                        int confirmResult = JOptionPane.showOptionDialog(
                                TelaProcessadorPDF.this,
                                details.toString(),
                                "Confirmar Dados do Boleto",
                                JOptionPane.YES_NO_OPTION,          // optionType
                                JOptionPane.QUESTION_MESSAGE,       // messageType
                                null,                               // icon (null para ícone padrão)
                                optionsConfirmacao,                 // options
                                optionsConfirmacao[0]               // initialValue (botão padrão)
                        );

                        boolean usuarioConfirmou = (confirmResult == JOptionPane.YES_OPTION);
                        boletoProcessado.setInformacoesConfirmadasPeloUsuario(usuarioConfirmou);

                        if (!usuarioConfirmou) {
                            resultArea.append("Usuário indicou que as informações NÃO estão corretas. Prosseguindo com alerta.\n");
                            boletoProcessado.addDetalheFalha("Usuário não confirmou as informações extraídas do PDF.");
                        } else {
                            resultArea.append("Confirmação registrada. Prosseguindo...\n");
                        }

                        // --- Pergunta sobre Desconto e Valor Original ---
                        BigDecimal valorSemDescontoInformado = null;
                        
                        String[] optionsDesconto = {"Sim", "Não"};
                        int temDescontoOption = JOptionPane.showOptionDialog(
                                TelaProcessadorPDF.this,
                                "Este boleto possui algum desconto?",
                                "Boleto com Desconto?",
                                JOptionPane.YES_NO_OPTION,          // optionType
                                JOptionPane.QUESTION_MESSAGE,       // messageType
                                null,                               // icon (null para ícone padrão)
                                optionsDesconto,                    // options
                                optionsDesconto[0]                  // initialValue (botão padrão)
                        );

                        boolean temDesconto = (temDescontoOption == JOptionPane.YES_OPTION);

                        if (temDesconto) {
                            String valorInput;
                            do {
                                valorInput = JOptionPane.showInputDialog(TelaProcessadorPDF.this,
                                        "Por favor, digite o valor *original* do boleto (sem descontos, formato 00.00):",
                                        "Valor Original", JOptionPane.QUESTION_MESSAGE);
                                if (valorInput == null) {
                                    resultArea.append("Entrada de valor cancelada pelo usuário.\n");
                                    return;
                                }
                                try {
                                    valorSemDescontoInformado = new BigDecimal(valorInput.replace(",", "."));
                                } catch (NumberFormatException ex) {
                                    JOptionPane.showMessageDialog(TelaProcessadorPDF.this,
                                            "Formato de valor inválido. Digite novamente o valor original (ex: 123.45):",
                                            "Erro de Formato", JOptionPane.ERROR_MESSAGE);
                                    valorSemDescontoInformado = null;
                                }
                            } while (valorSemDescontoInformado == null);
                        } else {
                            valorSemDescontoInformado = boletoProcessado.getValorAsBigDecimal();
                            if (valorSemDescontoInformado == null) {
                                String valorInput;
                                do {
                                    valorInput = JOptionPane.showInputDialog(TelaProcessadorPDF.this,
                                            "Valor não extraído do PDF. Por favor, digite o valor do pagamento do boleto (formato 00.00):",
                                            "Valor do Boleto", JOptionPane.QUESTION_MESSAGE);
                                    if (valorInput == null) {
                                        resultArea.append("Entrada de valor cancelada pelo usuário.\n");
                                        return;
                                    }
                                    try {
                                        valorSemDescontoInformado = new BigDecimal(valorInput.replace(",", "."));
                                    } catch (NumberFormatException ex) {
                                        JOptionPane.showMessageDialog(TelaProcessadorPDF.this,
                                                "Formato de valor inválido. Digite novamente o valor (ex: 123.45):",
                                                "Erro de Formato", JOptionPane.ERROR_MESSAGE);
                                        valorSemDescontoInformado = null;
                                    }
                                } while (valorSemDescontoInformado == null);
                            }
                        }

                        processador.continuarProcessamento(boletoProcessado, valorSemDescontoInformado);

                        // --- Exibe os resultados finais após o processamento completo ---
                        resultArea.append("\n--- RESULTADO FINAL DO PROCESSAMENTO ---\n");
                        resultArea.append("Nome Completo do Banco: " + (boletoProcessado.getNomeCompletoBancoApi() != null && !boletoProcessado.getNomeCompletoBancoApi().isEmpty() ? boletoProcessado.getNomeCompletoBancoApi() : "Dado indisponível") + "\n");
                        resultArea.append("Código do Banco: " + (boletoProcessado.getCodigoBancoApi() != null && !boletoProcessado.getCodigoBancoApi().isEmpty() ? boletoProcessado.getCodigoBancoApi() : "Dado indisponível") + "\n");
                        
                        String validadeLinhaDigitavel = "Válida";
                        if ("INVALIDO_ESTRUTURA_CB".equals(boletoProcessado.getStatusValidacao())) {
                            validadeLinhaDigitavel = "Inválida (estrutura)";
                        } else if (boletoProcessado.getCodigoBarras() == null || boletoProcessado.getCodigoBarras().isEmpty()) {
                             validadeLinhaDigitavel = "Não avaliada (código de barras ausente)";
                        }
                        resultArea.append("Validade da Linha Digitável: " + validadeLinhaDigitavel + "\n");
                        
                        resultArea.append("CNPJ Emitente: " + boletoProcessado.getCnpjEmitente() + "\n");
                        resultArea.append("Nome Beneficiário (PDF): " + boletoProcessado.getNomeBeneficiario() + "\n");
                        resultArea.append("Razão Social (CNPJ): " + (boletoProcessado.getRazaoSocialApi() != null && !boletoProcessado.getRazaoSocialApi().isEmpty() && !boletoProcessado.getRazaoSocialApi().equals("Dado indisponível") ? boletoProcessado.getRazaoSocialApi() : "Dado indisponível") + "\n");
                        resultArea.append("Nome Fantasia (CNPJ): " + (boletoProcessado.getNomeFantasiaApi() != null && !boletoProcessado.getNomeFantasiaApi().isEmpty() && !boletoProcessado.getNomeFantasiaApi().equals("Dado indisponível") ? boletoProcessado.getNomeFantasiaApi() : "Dado indisponível") + "\n");
                        resultArea.append("Valor do Boleto: " + boletoProcessado.getValorAsBigDecimal() + "\n");
                        resultArea.append("Data de Vencimento: " + (boletoProcessado.getVencimento() != null ? boletoProcessado.getVencimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A") + "\n");
                        resultArea.append(String.format("Score de Reputação (CNPJ): %.2f%%\n", boletoProcessado.getScoreReputacaoCnpj()));
                        resultArea.append("Total de Boletos (CNPJ): " + boletoProcessado.getTotalBoletosCnpj() + "\n");
                        resultArea.append("Total de Suspeitas (CNPJ): " + boletoProcessado.getTotalDenunciasCnpj() + "\n");
                        resultArea.append("Boleto Suspeito: " + (boletoProcessado.isSuspeito() ? "SIM" : "NÃO") + "\n");

                        if (!boletoProcessado.getDetalhesFalha().isEmpty()) {
                            resultArea.append("\n--- DETALHES DE ALERTAS/FALHAS ENCONTRADAS ---\n");
                            for (String detalhe : boletoProcessado.getDetalhesFalha()) {
                                resultArea.append("- " + detalhe + "\n");
                            }
                        } else {
                            resultArea.append("\nNenhum alerta ou falha significativa encontrada nas verificações.\n");
                        }


                        JOptionPane.showMessageDialog(TelaProcessadorPDF.this,
                                "Boleto processado e salvo com sucesso! Verifique os detalhes na área de resultados.",
                                "Processamento Concluído", JOptionPane.INFORMATION_MESSAGE);

                    } catch (ProcessamentoCancelado ex) {
                        resultArea.append("Processamento cancelado: " + ex.getMessage() + "\n");
                        JOptionPane.showMessageDialog(TelaProcessadorPDF.this,
                                "Processamento cancelado: " + ex.getMessage(),
                                "Processo Interrompido", JOptionPane.WARNING_MESSAGE);
                    } catch (IllegalArgumentException ex) {
                        resultArea.append("Erro na etapa inicial: " + ex.getMessage() + "\n");
                        JOptionPane.showMessageDialog(TelaProcessadorPDF.this,
                                "Erro na etapa inicial de extração: " + ex.getMessage(),
                                "Erro de Extração", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    } catch (IOException | SQLException ex) {
                        resultArea.append("Erro durante o processamento: " + ex.getMessage() + "\n");
                        JOptionPane.showMessageDialog(TelaProcessadorPDF.this,
                                "Ocorreu um erro durante o processamento do boleto: " + ex.getMessage(),
                                "Erro de Processamento", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    } catch (Exception ex) {
                        resultArea.append("Um erro inesperado ocorreu: " + ex.getMessage() + "\n");
                        JOptionPane.showMessageDialog(TelaProcessadorPDF.this,
                                "Um erro inesperado ocorreu: " + ex.getMessage(),
                                "Erro Inesperado", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }
                } else {
                    resultArea.setText("Seleção de arquivo cancelada.");
                }
            }
        });
    }
}