package boleto.extracao;

import usuario.Boleto;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.*;
import java.util.Scanner; // Importar Scanner

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class ExtracaoBoleto {

    // `caminhoToArquivo` agora será definido por um setter
    private File caminhoToArquivo = null; 
    
    // Construtor padrão
    public ExtracaoBoleto() {
        // O arquivo PDF será definido externamente através de setCaminhoToArquivo
    }

    /**
     * Define o arquivo PDF que será processado por esta instância de ExtracaoBoleto.
     * @param pdfFile O objeto File do arquivo PDF.
     */
    public void setCaminhoToArquivo(File pdfFile) {
        this.caminhoToArquivo = pdfFile;
    }

    /**
     * Gera (ou obtém) o objeto File correspondente ao arquivo TXT 
     * que seria ou foi extraído a partir de um PDF específico.
     * @param pdfFile O arquivo PDF de origem.
     * @return O objeto File representando o arquivo TXT esperado.
     */
    public File getArquivoTxtGerado(File pdfFile) {
        // Remove a extensão .pdf e adiciona .txt
        String caminhoSemExtensao = pdfFile.getAbsolutePath().replaceFirst("\\.pdf$", "");
        return new File(caminhoSemExtensao + ".txt");
    }

    /**
     * Transforma o conteúdo do PDF em texto e salva em um arquivo TXT,
     * ou lê o TXT existente se já foi criado.
     * Retorna o conteúdo textual do boleto.
     * @return O texto completo extraído do PDF (ou lido do TXT existente), ou null se não houver PDF.
     * @throws IOException Se ocorrer um erro durante a leitura/escrita de arquivos.
     */
    public String transformarPdfToTxt() throws IOException {
        // Verifica se um arquivo PDF foi selecionado antes de tentar processar
        if (this.caminhoToArquivo == null) {
            System.err.println("❌ **Erro:** Nenhum arquivo PDF definido para extração.");
            return null; 
        }

        // Obtém o File correspondente ao TXT que será gerado/lido
        File fileTxt = getArquivoTxtGerado(this.caminhoToArquivo); 

        // Se o arquivo TXT ainda não existe, extrai o texto do PDF e o salva
        if (!fileTxt.exists()) {
            System.out.println("⏳ Extraindo texto do PDF...");
            PDDocument document = Loader.loadPDF(this.caminhoToArquivo); // Carrega o PDF
            PDFTextStripper pdfStripper = new PDFTextStripper(); // Ferramenta para extrair texto
            String text = pdfStripper.getText(document); // Extrai o texto
            document.close(); // Fecha o documento PDF

            // Salva o texto extraído em um arquivo TXT
            try (FileWriter writer = new FileWriter(fileTxt)) {
                writer.write(text);
            }

            System.out.println("✅ Texto extraído e salvo com sucesso em '" + fileTxt.getAbsolutePath() + "'.");
            return text; // Retorna o texto extraído
        } else {
            // Se o arquivo TXT já existe, apenas lê seu conteúdo
            System.out.println("ℹ️ Texto já extraído e salvo em '" + fileTxt.getAbsolutePath() + "'. Lendo conteúdo existente.");
            try (Scanner fileScanner = new Scanner(fileTxt, "UTF-8")) { // Usar UTF-8 para evitar problemas de codificação
                // Usa um delimitador que significa "fim do input" para ler todo o arquivo de uma vez
                return fileScanner.useDelimiter("\\A").next(); 
            }
        }
    }

    /**
     * Processa o texto extraído do boleto e preenche um objeto Boleto.
     * @return Um objeto Boleto preenchido com as informações extraídas, ou um objeto Boleto vazio
     * se a extração mínima não for bem-sucedida.
     * @throws IOException Se ocorrer um erro durante a leitura do arquivo TXT.
     */
    public Boleto processarTxt() throws IOException {
        String texto = transformarPdfToTxt(); // Obtém o texto do boleto
        Boleto boleto = new Boleto();
        boleto.setDataExtracao(LocalDateTime.now()); // Registra a data/hora da extração

        if (texto != null) {
            texto = texto.replaceAll("\\s+", " ").trim(); // Normaliza espaços em branco

            System.out.println("===================================");
            System.out.println("\nExtraindo informações do boleto:\n");

            // Lógica de extração para boletos Cecred/Bradesco
            if (texto.trim().startsWith("BENEFICI")) {
                String valorStr = extrairCampoCecredEBradesco(texto, Pattern.compile("Valor (Cobrado|do Documento)[\\s:]*R?\\$?\\s*([\\d.,]+)"));
                String dataVencimentoStr = extrairCampoCecredEBradesco(texto, Pattern.compile("Data de Vencimento\\s*([\\d]{2}/[\\d]{2}/[\\d]{4})"));
                String cnpjEmitenteStr = extrairCampoCecredEBradesco(texto, Pattern.compile("CNPJ[:\\s]*([\\d]{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})"));
                String nomeBeneficiarioStr = extrairCampoCecredEBradesco(texto, Pattern.compile("BENEFICIÁRIO:\\s*([A-Z\\sÇÃÉÍÓÊÂÔÛÜ]+ LTDA)"));
                String bancoEmissorStr = extrairBancoEmissorCecredEBradesco(texto);
                String codigoBarrasStr = extrairLinhaDigitavel(texto);

                try {
                    // Preenche os campos do boleto, tratando formatos e nulos
                    if (valorStr != null) boleto.setValor(new BigDecimal(valorStr.replace(".", "").replace(",", ".")));
                    if (dataVencimentoStr != null) boleto.setVencimento(LocalDate.parse(dataVencimentoStr, DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    if (cnpjEmitenteStr != null) boleto.setCnpjEmitente(cnpjEmitenteStr.replaceAll("[^0-9]", "")); // Apenas dígitos
                    if (nomeBeneficiarioStr != null) boleto.setNomeBeneficiario(nomeBeneficiarioStr);
                    if (bancoEmissorStr != null) boleto.setBancoEmissor(bancoEmissorStr);
                    if (codigoBarrasStr != null) boleto.setCodigoBarras(codigoBarrasStr.replaceAll("[^0-9]", "")); // Apenas dígitos
                } catch (NumberFormatException | DateTimeParseException e) {
                    System.err.println("❌ **Erro ao parsear dados para o boleto (Cecred/Bradesco):** " + e.getMessage());
                }
            }
            // Lógica de extração para boletos Anhembi (ou outros formatos que você adicionar)
            else if (texto.trim().startsWith("ISCP")) {
                String valorStr = extrairCampoAnhembi(texto, Pattern.compile("(=)?\\s*VALOR\\s*COBRADO\\s*[:,]?\\s*([\\d.,-]+)"));
                String dataVencimentoStr = extrairCampoAnhembi(texto, Pattern.compile("VENCIMENTO[:\\s]*([\\d]{2}/[\\d]{2}/[\\d]{4})"));
                String cnpjEmitenteStr = extrairCampoAnhembi(texto, Pattern.compile("(CNPJ(:|\\s))?\\s*([\\d]{14}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})"));
                String nomeBeneficiarioStr = extrairCampoAnhembi(texto, Pattern.compile("(ISCP\\s*-?\\s*.*?\\s*LTDA)", Pattern.CASE_INSENSITIVE));
                String bancoEmissorStr = extrairCampoAnhembi(texto, Pattern.compile("Banco\\s+do\\s+Brasil", Pattern.CASE_INSENSITIVE));
                String codigoBarrasStr = extrairLinhaDigitavel(texto);

                try {
                    if (valorStr != null) boleto.setValor(new BigDecimal(valorStr.replace(".", "").replace(",", ".")));
                    if (dataVencimentoStr != null) boleto.setVencimento(LocalDate.parse(dataVencimentoStr, DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    if (cnpjEmitenteStr != null) boleto.setCnpjEmitente(cnpjEmitenteStr.replaceAll("[^0-9]", ""));
                    if (nomeBeneficiarioStr != null) boleto.setNomeBeneficiario(nomeBeneficiarioStr);
                    if (bancoEmissorStr != null) boleto.setBancoEmissor(bancoEmissorStr);
                    if (codigoBarrasStr != null) boleto.setCodigoBarras(codigoBarrasStr.replaceAll("[^0-9]", ""));
                } catch (NumberFormatException | DateTimeParseException e) {
                    System.err.println("❌ **Erro ao parsear dados para o boleto (Anhembi):** " + e.getMessage());
                }
            } else {
                System.out.println("⚠️ **Formato de boleto não reconhecido.** Extração pode ser incompleta.");
            }
        }
        return boleto;
    }

    /**
     * Extrai um campo específico do texto para boletos no formato Anhembi usando um padrão regex.
     * @param texto O texto completo do boleto.
     * @param padrao O padrão regex para encontrar o campo.
     * @return O valor extraído ou null se não encontrado.
     */
    private String extrairCampoAnhembi(String texto, Pattern padrao) {
        Matcher matcher = padrao.matcher(texto);
        if (matcher.find()) {
            String resultado;
            // Lógica específica para grupos de captura baseada no padrão
            if (padrao.pattern().contains("VALOR\\s*COBRADO") && matcher.groupCount() >= 2) {
                resultado = matcher.group(2);
            } else if (padrao.pattern().contains("VENCIMENTO") && matcher.groupCount() >= 1) {
                resultado = matcher.group(1);
            } else if (padrao.pattern().contains("CNPJ") && matcher.groupCount() >= 3) {
                resultado = matcher.group(3);
            } else if (matcher.groupCount() >= 1) {
                resultado = matcher.group(1);
            } else {
                resultado = matcher.group(0);
            }
            return resultado;
        } else {
            return null;
        }
    }

    /**
     * Extrai um campo específico do texto para boletos no formato Cecred/Bradesco usando um padrão regex.
     * @param texto O texto completo do boleto.
     * @param padrao O padrão regex para encontrar o campo.
     * @return O valor extraído ou null se não encontrado.
     */
    private String extrairCampoCecredEBradesco(String texto, Pattern padrao) {
        if (texto == null) {
            return null;
        }

        Matcher matcher = padrao.matcher(texto);
        if (matcher.find()) {
            String resultado;
            int groupCount = matcher.groupCount();

            // Lógica específica para grupos de captura baseada no padrão
            if ((padrao.pattern().contains("Valor (Cobrado|do Documento)") || padrao.pattern().contains("Data de Vencimento")) && groupCount >= 1) {
                resultado = matcher.group(groupCount); // Pega o último grupo de captura (o valor)
            } else if (groupCount >= 1) {
                resultado = matcher.group(1); // Pega o primeiro grupo de captura
            } else {
                resultado = matcher.group(0); // Pega a correspondência inteira
            }
            return resultado;
        } else {
            return null;
        }
    }

    /**
     * Extrai o nome do banco emissor para boletos Cecred/Bradesco.
     * @param texto O texto completo do boleto.
     * @return O nome do banco.
     */
    private String extrairBancoEmissorCecredEBradesco(String texto) {
        String banco;
        if (texto.toUpperCase().contains("SISTEMA AILOS")) {
            banco = "Sistema Ailos (Cooperativa)";
        } else {
            banco = "Bradesco";
        }
        return banco;
    }

    /**
     * Extrai a linha digitável (código de barras) do boleto.
     * @param texto O texto completo do boleto.
     * @return A linha digitável ou null se não encontrada.
     */
    private String extrairLinhaDigitavel(String texto) {
        if (texto == null) {
            return null;
        }

        // Normaliza espaços para facilitar a correspondência regex
        texto = texto.replaceAll("\\s+", " ").trim();

        // Padrão regex para encontrar a linha digitável (5.5 5.6 5.6 1 14)
        Pattern padrao = Pattern.compile("\\d{5}\\.\\d{5}\\s\\d{5}\\.\\d{6}\\s\\d{5}\\.\\d{6}\\s\\d\\s\\d{14}");
        Matcher matcher = padrao.matcher(texto);

        if (matcher.find()) {
            String resultado = matcher.group(0); // Retorna a correspondência completa
            return resultado;
        } else {
            return null;
        }
    }
}