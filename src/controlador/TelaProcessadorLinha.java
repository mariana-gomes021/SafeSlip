package controlador;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import controlador.ProcessadoresGUI.ProcessadorLinhaGui;
import usuario.Boleto;

public class TelaProcessadorLinha extends JPanel {

    private CardLayout cardLayout;
    private JPanel parentContainer;

    private JTextField lineInputField;
    private JTextField valueInputField;
    private JTextField dueDateField;
    private JTextField cnpjInputField;
    private JTextArea resultArea;
    private JButton processButton;
    private JButton backButton;

    private Boleto boletoEmProcessamento; // Objeto Boleto que será populado e depois finalizado
    private ProcessadorLinhaGui processador; // Instância do processador

    public TelaProcessadorLinha(CardLayout cardLayout, JPanel parentContainer) {
        this.cardLayout = cardLayout;
        this.parentContainer = parentContainer;
        this.processador = new ProcessadorLinhaGui(); // Instancia o processador uma vez
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Processamento de Boletos via Linha Digitável", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
        inputPanel.add(new JLabel("Linha Digitável:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        lineInputField = new JTextField(30);
        inputPanel.add(lineInputField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST;
        inputPanel.add(new JLabel("Valor *original* do boleto (sem descontos, formato 00.00):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        valueInputField = new JTextField(15);
        inputPanel.add(valueInputField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
        inputPanel.add(new JLabel("Vencimento (DD/MM/AAAA):"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        dueDateField = new JTextField(10);
        inputPanel.add(dueDateField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.anchor = GridBagConstraints.EAST;
        inputPanel.add(new JLabel("CNPJ Beneficiário:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0;
        cnpjInputField = new JTextField(18);
        inputPanel.add(cnpjInputField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER;
        processButton = new JButton("Processar Linha Digitável");
        inputPanel.add(processButton, gbc);

        add(inputPanel, BorderLayout.CENTER);

        resultArea = new JTextArea(15, 40);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(resultArea);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        backButton = new JButton("Voltar à Página Inicial");
        buttonPanel.add(backButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(scrollPane, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        processButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                processarBoletoLinhaDigitavel();
            }
        });

        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(parentContainer, "telaEnvioBoleto");
                clearFields();
            }
        });
    }

    private void processarBoletoLinhaDigitavel() {
        final String linhaDigitavel = lineInputField.getText().trim();
        final String valorStr = valueInputField.getText().trim();
        final String dataVencimentoStr = dueDateField.getText().trim();
        final String cnpjInput = cnpjInputField.getText().trim();

        if (linhaDigitavel.isEmpty() || valorStr.isEmpty() || dataVencimentoStr.isEmpty() || cnpjInput.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, preencha todos os campos obrigatórios.", "Campos Vazios", JOptionPane.WARNING_MESSAGE);
            return;
        }

        resultArea.setText("Iniciando processamento da linha digitável...\n");
        processButton.setEnabled(false);

        SwingWorker<Boleto, Void> worker = new SwingWorker<Boleto, Void>() {
            @Override
            protected Boleto doInBackground() throws Exception {
                // APENAS A PRIMEIRA CHAMADA: Realiza as validações preliminares
                return processador.validarDadosPreliminares(linhaDigitavel, valorStr, dataVencimentoStr, cnpjInput);
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

                    String cnpjMessage = "--- Dados do CNPJ " + boletoEmProcessamento.getCnpjEmitente() + " " +
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

                    String bancoMessage = "--- Dados do Banco " + codigoBanco + " \n" +
                                          "Nome do Banco: " + nomeBanco + "\n" +
                                          "Nome Completo: " + nomeCompletoBanco + "\n" +
                                          "ISPB: " + ispb + "\n" +
                                          "As informações do Banco acima estão corretas?";

                    int bancoOption = JOptionPane.showConfirmDialog(TelaProcessadorLinha.this, bancoMessage,
                                                                     "Confirmação de Banco", JOptionPane.YES_NO_OPTION);
                    boolean usuarioConfirmouBanco = (bancoOption == JOptionPane.YES_OPTION);

                    // --- CHAMADA FINAL PARA PERSISTÊNCIA E REPUTAÇÃO ---
                    // Passa o MESMO objeto boleto e as confirmações do usuário
                    Boleto boletoFinalizado = processador.finalizarProcessamentoESalvar(
                        boletoEmProcessamento, usuarioConfirmouCnpj, usuarioConfirmouBanco
                    );

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
        worker.execute();
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
        sb.append("Boleto Suspeito: ").append(boleto.isSuspeito() ? "Sim" : "Não").append("\n");


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
        if (boleto.getStatusValidacaoCnpj().contains("ALERTA_API_OFFLINE") || boleto.getStatusValidacaoCnpj().contains("ERRO_CONSULTA_API_CNPJ")) {
            sb.append("- A consulta à API de CNPJ retornou erro ou estava offline.\n");
            possuiAlertas = true;
        }
        if (boleto.getStatusValidacaoBanco().contains("ALERTA_API_OFFLINE") || boleto.getStatusValidacaoBanco().contains("ERRO_CONSULTA_API_BANCO")) {
            sb.append("- A consulta à API de Bancos retornou erro ou estava offline.\n");
            possuiAlertas = true;
        }
        if ("ALERTA_FRAUDE_NOME_CNPJ_DIVERGENTE".equals(boleto.getStatusValidacao())) {
            sb.append("- **ALERTA DE FRAUDE:** Nome/Razão Social do CNPJ diverge da base de dados ou é genérico.\n");
            possuiAlertas = true;
        }
        if (boleto.isSuspeito() && !possuiAlertas) { // Caso a flag suspeito esteja true, mas não haja alertas específicos listados
            sb.append("- Boleto marcado como suspeito por inconsistências internas.\n");
            possuiAlertas = true;
        }
        if (!possuiAlertas) {
            sb.append("- Nenhuma alerta/falha significativa encontrada nas validações.\n");
        }
        
        sb.append("\n--- REPUTAÇÃO DO CNPJ EMITENTE ---\n");
        sb.append(String.format("Score de Reputação: %.2f%%\n", boleto.getScoreReputacaoCnpj()));
        sb.append("Classificação: ").append(boleto.getClassificacaoReputacao()).append("\n");
        sb.append("Total de Boletos Associados ao CNPJ: ").append(boleto.getTotalBoletosCnpj()).append("\n");
        sb.append("Total de Denúncias Associadas ao CNPJ: ").append(boleto.getTotalDenunciasCnpj()).append("\n");


        resultArea.setText(sb.toString());
    }

    private void clearFields() {
        lineInputField.setText("");
        valueInputField.setText("");
        dueDateField.setText("");
        cnpjInputField.setText("");
        resultArea.setText("");
        boletoEmProcessamento = null;
    }
}