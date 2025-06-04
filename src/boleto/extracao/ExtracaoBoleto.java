package boleto.extracao;

import boleto.EnvioBoleto;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.*;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONObject;

public class ExtracaoBoleto {

    EnvioBoleto envioBoleto = new EnvioBoleto();
    private File caminhoToArquivo = envioBoleto.selecionarArquivoPDF();

    public String transformarPdfToTxt() throws IOException {
        String caminhoSemExtensao = caminhoToArquivo.getAbsolutePath().replaceFirst("\\.pdf$", "");
        File fileTxt = new File(caminhoSemExtensao + ".txt");

        if (!fileTxt.exists()) {
            PDDocument document = Loader.loadPDF(caminhoToArquivo);
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);
            document.close();

            FileWriter writer = new FileWriter(fileTxt);
            writer.write(text);
            writer.close();

            System.out.println("Texto extraído e salvo com sucesso em '" + fileTxt.getAbsolutePath() + "'.");
            return text;
        }

        System.out.println("Texto ja extraido e salvo em '" + fileTxt.getAbsolutePath() + "'.");
        return null;
    }

    public String processarTxt() throws IOException {
        String texto = transformarPdfToTxt();
        JSONObject resultado = new JSONObject();
        if (texto != null) {

            if (texto.trim().startsWith("BENEFICI")) {
                System.out.println("===================================");
                System.out.println("\nExtraindo informações do boleto:\n");

                resultado.put("Valor", extrairCampoCecredEBradesco("Valor", texto, Pattern.compile("Valor (Cobrado|do Documento)[\\s:]*R?\\$?\\s*([\\d.,]+)")));
                resultado.put("Data de Vencimento", extrairCampoCecredEBradesco("Data de Vencimento", texto, Pattern.compile("Data de Vencimento\\s*([\\d]{2}/[\\d]{2}/[\\d]{4})")));
                resultado.put("CNPJ do Beneficiário", extrairCampoCecredEBradesco("CNPJ do Beneficiário", texto, Pattern.compile("CNPJ[:\\s]*([\\d]{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})")));
                resultado.put("Nome do Beneficiário", extrairCampoCecredEBradesco("Nome do Beneficiário", texto, Pattern.compile("BENEFICIÁRIO:\\s*([A-Z\\sÇÃÉÍÓÊÂÔÛÜ]+ LTDA)")));
                resultado.put("Banco Emissor", extrairBancoEmissorCecredEBradesco(texto));
                resultado.put("Numero do codigo de barras", extrairCampoCecredEBradesco("Numero do codigo de barras", texto, Pattern.compile("\\d{5}\\.\\d{5}\\s\\d{5}\\.\\d{6}\\s\\d{5}\\.\\d{6}\\s\\d\\s\\d{14}")));

                return resultado.toString(1);
            }

            if (texto.trim().startsWith("ISCP")) {
                System.out.println("===================================");
                System.out.println("\nExtraindo informações do boleto:\n");
                resultado.put("Valor", extrairCampoAnhembi("Valor", texto, Pattern.compile("(=)?\\s*VALOR\\s*COBRADO\\s*[:,]?\\s*([\\d.,-]+)")));
                resultado.put("Data de Vencimento", extrairCampoAnhembi("Data de Vencimento", texto, Pattern.compile("VENCIMENTO[:\\s]*([\\d]{2}/[\\d]{2}/[\\d]{4})")));
                resultado.put("CNPJ do Beneficiario", extrairCampoAnhembi("CNPJ do Beneficiario", texto, Pattern.compile("(CNPJ(:|\\s))?\\s*([\\d]{14}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})")));
                resultado.put("Nome do Beneficiario", extrairCampoAnhembi("Nome do Beneficiario", texto, Pattern.compile("(ISCP\\s*-?\\s*.*?\\s*LTDA)", Pattern.CASE_INSENSITIVE)));
                resultado.put("Banco Emissor", extrairCampoAnhembi("Banco Emissor", texto, Pattern.compile("Banco\\s+do\\s+Brasil", Pattern.CASE_INSENSITIVE)));
                resultado.put("Numero do codigo de barras", extrairCampoAnhembi("Numero do codigo de barras", texto, Pattern.compile("\\b\\d{3}-\\d\\s\\d{5}\\.\\d{5}\\b")));

                return resultado.toString(1);
            }

        }

        return null;

    }

    private String extrairCampoAnhembi(String nomeCampo, String texto, Pattern padrao) {
        Matcher matcher = padrao.matcher(texto);
        if (matcher.find()) {
            String resultado;
            if (nomeCampo.equals("Valor") && matcher.groupCount() >= 2) {
                resultado = matcher.group(2);
            } else if (nomeCampo.equals("Data de Vencimento") && matcher.groupCount() >= 1) {
                resultado = matcher.group(1);
            } else if (nomeCampo.equals("CNPJ do Beneficiario") && matcher.groupCount() >= 3) {
                resultado = matcher.group(3);
            } else if (matcher.groupCount() >= 1) {
                resultado = matcher.group(1);
            } else {
                resultado = matcher.group(0);
            }
            System.out.println(nomeCampo + ": " + resultado);
            return resultado;
        } else {
            System.out.println(nomeCampo + ": Não encontrado.");
            return null;
        }
    }

    private String extrairCampoCecredEBradesco(String nomeCampo, String texto, Pattern padrao) {
        if (texto == null) {
            System.out.println(nomeCampo + ": Texto de entrada é null.");
            return null;
        }

        Matcher matcher = padrao.matcher(texto);
        if (matcher.find()) {
            String resultado;
            int groupCount = matcher.groupCount();

            if ((nomeCampo.equals("Valor") || nomeCampo.equals("Data de Vencimento")) && groupCount >= 1) {
                resultado = matcher.group(groupCount);
            } else if (groupCount >= 1) {
                resultado = matcher.group(1);
            } else {
                resultado = matcher.group(0);
            }

            System.out.println(nomeCampo + ": " + resultado);
            return resultado;
        } else {
            System.out.println(nomeCampo + ": Não encontrado.");
            return null;
        }
    }

    private String extrairBancoEmissorCecredEBradesco(String texto) {
        String banco;
        String bancoEmissor = "Banco Emissor: ";

        if (texto.toUpperCase().contains("SISTEMA AILOS")) {
            banco = "Sistema Ailos (Cooperativa)";
            System.out.println(bancoEmissor + banco);
            return banco;
        } else {
            banco = "Bradesco";
            System.out.println(bancoEmissor + banco);
            return banco;
        }
    }

}
