package controlador;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter; // Importar para formatar a data
import controlador.ProcessadoresGUI.ProcessadorLinhaGui; // Importe o ProcessadorLinhaGui
import usuario.Boleto; // Importe a classe Boleto

public class TelaProcessadorLinha extends JPanel {

    private CardLayout cardLayout;
    private JPanel parentContainer;

    // Componentes da GUI
    private JTextField lineInputField;
    private JTextField valueInputField;
    private JTextField dueDateField;
    private JTextField cnpjInputField; 
    // private JCheckBox discountCheckBox; // <--- REMOVIDO: Não é mais necessário declarar aqui
    private JTextArea resultArea;
    private JButton processButton;
    private JButton backButton; 

    // O objeto Boleto será retornado pelo primeiro processamento e usado para o segundo
    private Boleto boletoEmProcessamento;

    // Construtor com CardLayout e JPanel para navegação
    public TelaProcessadorLinha(CardLayout cardLayout, JPanel parentContainer) {
        this.cardLayout = cardLayout;
        this.parentContainer = parentContainer;

        initComponents(); 
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Processamento de Boletos via Linha Digitável", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);

        // Painel para os campos de entrada (Linha Digitável, Valor, Vencimento, CNPJ)
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); 
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Linha Digitável
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
        inputPanel.add(new JLabel("Linha Digitável:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        lineInputField = new JTextField(30);
        inputPanel.add(lineInputField, gbc);

        // Valor ORIGINAL do Pagamento (com o novo texto)
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST;
        inputPanel.add(new JLabel("Valor *original* do boleto (sem descontos, formato 00.00):"), gbc); // <--- TEXTO ATUALIZADO
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        valueInputField = new JTextField(15);
        inputPanel.add(valueInputField, gbc);

        // Data de Vencimento
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        inputPanel.add(new JLabel("Vencimento (DD/MM/AAAA):"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        dueDateField = new JTextField(10);
        inputPanel.add(dueDateField, gbc);
        
        // CNPJ do Beneficiário
        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST;
        inputPanel.add(new JLabel("CNPJ Beneficiário:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0;
        cnpjInputField = new JTextField(18); 
        inputPanel.add(cnpjInputField, gbc);

        // O JCheckBox 'discountCheckBox' e sua adição ao painel foram REMOVIDOS aqui.
        // gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.WEST;
        // discountCheckBox = new JCheckBox("Este boleto possui desconto?");
        // inputPanel.add(discountCheckBox, gbc);

        // Botão Processar (ajustado o gridy para a próxima posição disponível)
        gbc.gridx = 0; gbc.gridy = 4; // <--- Ajustado o gridy após a remoção do checkbox
        gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        processButton = new JButton("Processar Linha Digitável");
        inputPanel.add(processButton, gbc);

        add(inputPanel, BorderLayout.CENTER); 

        // Área de resultados (inicialmente vazia, preenchida após o processamento)
        resultArea = new JTextArea(15, 40);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        
        // Painel de botões no rodapé
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backButton = new JButton("Voltar à Página Inicial");
        buttonPanel.add(backButton);

        // Crie um novo painel para os resultados e o botão de voltar
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(scrollPane, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        // Listener para o botão de processamento
        processButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                processarBoletoLinhaDigitavel();
            }
        });

        // Listener para o botão de voltar
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(parentContainer, "telaEnvioBoleto");
                clearFields(); // Limpa os campos ao voltar
            }
        });
    }

    private void processarBoletoLinhaDigitavel() {
        final String linhaDigitavel = lineInputField.getText().trim();
        final String valorStr = valueInputField.getText().trim();
        final String dataVencimentoStr = dueDateField.getText().trim();
        final String cnpjInput = cnpjInputField.getText().trim();
        // final boolean temDesconto = discountCheckBox.isSelected(); // <--- REMOVIDO: Não é mais usada

        if (linhaDigitavel.isEmpty() || valorStr.isEmpty() || dataVencimentoStr.isEmpty() || cnpjInput.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, preencha todos os campos obrigatórios.", "Campos Vazios", JOptionPane.WARNING_MESSAGE);
            return;
        }

        resultArea.setText("Iniciando processamento da linha digitável...\n");
        // Desabilita o botão para evitar cliques múltiplos enquanto processa
        processButton.setEnabled(false);

        SwingWorker<Boleto, Void> worker = new SwingWorker<Boleto, Void>() {
            @Override
            protected Boleto doInBackground() throws Exception {
                // Primeira chamada ao ProcessadorLinhaGui para pegar os dados da API
                ProcessadorLinhaGui initialProcessor = new ProcessadorLinhaGui();
                // Passa 'false' para as confirmações de usuário na primeira vez
                return initialProcessor.processarLinhaDigitavel(
                    linhaDigitavel, valorStr, dataVencimentoStr, 
                    false, // <--- Sempre passa 'false' para 'temDesconto'
                    false, false, // Sem confirmação de usuário ainda
                    cnpjInput
                );
            }

            @Override
            protected void done() {
                try {
                    boletoEmProcessamento = get(); // Pega o boleto retornado do doInBackground
                    if (boletoEmProcessamento == null) {
                        resultArea.append("\nErro crítico durante a fase inicial do processamento. Boleto não gerado.");
                        return;
                    }

                    // --- ETAPA DE CONFIRMAÇÃO DO USUÁRIO ---

                    // 1. Exibir informações do CNPJ e pedir confirmação
                    String razaoSocial = (boletoEmProcessamento.getRazaoSocialApi() != null && !boletoEmProcessamento.getRazaoSocialApi().isEmpty()) ? boletoEmProcessamento.getRazaoSocialApi() : "Não disponível";
                    String nomeFantasia = (boletoEmProcessamento.getNomeFantasiaApi() != null && !boletoEmProcessamento.getNomeFantasiaApi().isEmpty()) ? boletoEmProcessamento.getNomeFantasiaApi() : "Não disponível";
                    
                    String cnpjMessage = "--- Dados do CNPJ " + boletoEmProcessamento.getCnpjEmitente() + " Retornados pela BrasilAPI ---\n" +
                                         "Razão Social: " + razaoSocial + "\n" +
                                         "Nome Fantasia: " + nomeFantasia + "\n" +
                                         "As informações do CNPJ acima estão corretas?";
                    
                    int cnpjOption = JOptionPane.showConfirmDialog(TelaProcessadorLinha.this, cnpjMessage, 
                                                                    "Confirmação de CNPJ", JOptionPane.YES_NO_OPTION);
                    boolean usuarioConfirmouCnpj = (cnpjOption == JOptionPane.YES_OPTION);
                    
                    // 2. Exibir informações do Banco e pedir confirmação
                    String codigoBanco = boletoEmProcessamento.getBancoEmissor();
                    String nomeBanco = (boletoEmProcessamento.getNomeBancoApi() != null && !boletoEmProcessamento.getNomeBancoApi().isEmpty()) ? boletoEmProcessamento.getNomeBancoApi() : "Não disponível";
                    String nomeCompletoBanco = (boletoEmProcessamento.getNomeCompletoBancoApi() != null && !boletoEmProcessamento.getNomeCompletoBancoApi().isEmpty()) ? boletoEmProcessamento.getNomeCompletoBancoApi() : "Não disponível";
                    String ispb = (boletoEmProcessamento.getIspbBancoApi() != null && !boletoEmProcessamento.getIspbBancoApi().isEmpty()) ? boletoEmProcessamento.getIspbBancoApi() : "Não disponível";
                    
                    String bancoMessage = "--- Dados do Banco " + codigoBanco + " Retornados pela BrasilAPI ---\n" +
                                          "Nome do Banco: " + nomeBanco + "\n" +
                                          "Nome Completo: " + nomeCompletoBanco + "\n" +
                                          "ISPB: " + ispb + "\n" +
                                          "As informações do Banco acima estão corretas?";

                    int bancoOption = JOptionPane.showConfirmDialog(TelaProcessadorLinha.this, bancoMessage,
                                                                     "Confirmação de Banco", JOptionPane.YES_NO_OPTION);
                    boolean usuarioConfirmouBanco = (bancoOption == JOptionPane.YES_OPTION);

                    // --- SEGUNDA CHAMADA AO PROCESSADOR (COM AS CONFIRMAÇÕES DO USUÁRIO) ---
                    // Esta é a chamada "final" que considera a interação do usuário.
                    ProcessadorLinhaGui finalProcessor = new ProcessadorLinhaGui();
                    Boleto boletoFinalizado = finalProcessor.processarLinhaDigitavel(
                        linhaDigitavel, valorStr, dataVencimentoStr, 
                        false, // <--- Sempre passa 'false' para 'temDesconto'
                        usuarioConfirmouCnpj, usuarioConfirmouBanco, // Passa as confirmações do usuário
                        cnpjInput 
                    );

                    // Exibir os resultados finais do boleto
                    displayBoletoResults(boletoFinalizado);

                } catch (IllegalArgumentException ex) {
                    JOptionPane.showMessageDialog(TelaProcessadorLinha.this, ex.getMessage(), "Erro de Entrada", JOptionPane.ERROR_MESSAGE);
                    resultArea.setText("Erro de entrada: " + ex.getMessage());
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(TelaProcessadorLinha.this, "Erro de banco de dados: " + ex.getMessage(), "Erro no DB", JOptionPane.ERROR_MESSAGE);
                    resultArea.setText("Erro no banco de dados: " + ex.getMessage());
                    ex.printStackTrace();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(TelaProcessadorLinha.this, "Ocorreu um erro inesperado: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                    resultArea.setText("Erro inesperado: " + ex.getMessage());
                    ex.printStackTrace();
                } finally {
                    processButton.setEnabled(true); // Reabilita o botão após o processamento
                }
            }
        };
        worker.execute(); // Executa o processamento em background
    }

    private void displayBoletoResults(Boleto boleto) {
        if (boleto == null) {
            resultArea.setText("Não foi possível gerar o resumo do boleto. Houve um erro crítico.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("--- RESUMO DO BOLETO PROCESSADO ---\n");
        sb.append("Linha Digitável: ").append(boleto.getCodigoBarras()).append("\n");
        sb.append("Valor Informado: ").append(boleto.getValorAsBigDecimal()).append("\n");
        sb.append("Valor Extraído da Linha: ").append(boleto.getValorExtraidoLinhaDigital() != null ? boleto.getValorExtraidoLinhaDigital() : "N/A").append("\n");
        sb.append("Vencimento informado: ").append(boleto.getVencimento() != null ? boleto.getVencimento().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A").append("\n");
        sb.append("Data da Verificação: ").append(boleto.getDataExtracao() != null ? boleto.getDataExtracao().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) : "N/A").append("\n");

        sb.append("\n--- DADOS DO EMITENTE ---\n");
        sb.append("CNPJ Emitente: ").append(boleto.getCnpjEmitente() != null ? boleto.getCnpjEmitente() : "N/A").append("\n");
        sb.append("Razão Social: ").append(boleto.getRazaoSocialApi() != null ? boleto.getRazaoSocialApi() : "N/A").append("\n");
        sb.append("Nome Fantasia: ").append(boleto.getNomeFantasiaApi() != null ? boleto.getNomeFantasiaApi() : "N/A").append("\n");
        sb.append("Confirmação Usuário (CNPJ/Banco): ").append(boleto.isInformacoesConfirmadasPeloUsuario() ? "Sim" : "Não").append("\n");
        
        sb.append("\n--- DADOS DO BANCO ---\n");
        sb.append("Código do Banco: ").append(boleto.getBancoEmissor() != null ? boleto.getBancoEmissor() : "N/A").append("\n");
        sb.append("Nome do Banco: ").append(boleto.getNomeBancoApi() != null ? boleto.getNomeBancoApi() : "N/A").append("\n");
        sb.append("Nome Completo Banco: ").append(boleto.getNomeCompletoBancoApi() != null ? boleto.getNomeCompletoBancoApi() : "N/A").append("\n");
      


        sb.append("\n--- STATUS GERAL DA VALIDAÇÃO ---\n");
        // Verifica se a linha digitável é estruturalmente válida
        if ("ERRO_ESTRUTURA_OU_DV_LD".equals(boleto.getStatusValidacao())) {
            sb.append("Validação da Linha Digitável: ").append("INVÁLIDA").append("\n");
        } else {
            sb.append("Validação da Linha Digitável: ").append("VÁLIDA").append("\n");
        }
       
        

        // --- DETALHES DE ALERTAS/FALHAS ENCONTRADAS ---
        sb.append("\n--- DETALHES DE ALERTAS/FALHAS ENCONTRADAS ---\n");
        boolean possuiAlertas = false;

        if ("ERRO_ESTRUTURA_OU_DV_LD".equals(boleto.getStatusValidacao())) {
            sb.append("- Falha na estrutura ou dígitos verificadores da Linha Digitável.\n");
            possuiAlertas = true;
        }
        if ("VALOR_DIVERGENTE".equals(boleto.getStatusValidacao())) {
            sb.append("- O valor informado difere do valor extraído da Linha Digitável.\n");
            possuiAlertas = true;
        }
        if (boleto.getStatusValidacaoCnpj().contains("NAO_CONFIRMADO_USUARIO")) {
            sb.append("- O usuário NÃO confirmou os dados do CNPJ apresentados pela API.\n");
            possuiAlertas = true;
        }
        if (boleto.getStatusValidacaoBanco().contains("NAO_CONFIRMADO_USUARIO")) {
            sb.append("- O usuário NÃO confirmou os dados do Banco apresentados pela API.\n");
            possuiAlertas = true;
        }
        if (boleto.getStatusValidacaoCnpj().contains("ALERTA_API_OFFLINE")) {
            sb.append("- A consulta à API de CNPJ retornou erro ou estava offline.\n");
            possuiAlertas = true;
        }
        if (boleto.getStatusValidacaoBanco().contains("ALERTA_API_OFFLINE")) {
            sb.append("- A consulta à API de Bancos retornou erro ou estava offline.\n");
            possuiAlertas = true;
        }
        if ("ALERTA_FRAUDE_NOME_CNPJ_DIVERGENTE".equals(boleto.getStatusValidacao())) {
            sb.append("- **ALERTA DE FRAUDE:** Nome/Razão Social do CNPJ diverge da base de dados ou é genérico.\n");
            possuiAlertas = true;
        }
        if (!possuiAlertas && !"VALIDO_COMPLETO".equals(boleto.getStatusValidacao())) {
             // Caso existam outros status de "não conformidade" que não foram explicitamente detalhados acima
             sb.append("- Outra não conformidade detectada: ").append(boleto.getStatusValidacao()).append("\n");
             possuiAlertas = true;
        }
        if (!possuiAlertas) {
            sb.append("- Nenhuma falha ou alerta detectado diretamente na validação.\n");
        }


        sb.append("\n--- REPUTAÇÃO DO CNPJ EMITENTE ---\n");
        sb.append(String.format("Score de Reputação: %.2f%%\n", boleto.getScoreReputacaoCnpj()));
        sb.append("Classificação: ").append(boleto.getClassificacaoReputacao() != null ? boleto.getClassificacaoReputacao() : "N/A").append("\n");
        sb.append("Total de Boletos (CNPJ): ").append(boleto.getTotalBoletosCnpj()).append("\n");
        sb.append("Total de Suspeitas (CNPJ): ").append(boleto.getTotalDenunciasCnpj()).append("\n");
        
        // --- Linha "Boleto Considerado Suspeito" formatada ---
        sb.append("Boleto Considerado Suspeito: ");
        if (boleto.isSuspeito()) {
            sb.append("**SIM**\n");
        } else {
            sb.append("NÃO\n");
        }

        resultArea.setText(sb.toString());
    }


    private void clearFields() {
        lineInputField.setText("");
        valueInputField.setText("");
        dueDateField.setText("");
        cnpjInputField.setText("");
        // discountCheckBox.setSelected(false); // <--- REMOVIDO: Não é mais necessário
        resultArea.setText("");
        boletoEmProcessamento = null; // Limpa o boleto em processamento
    }
}