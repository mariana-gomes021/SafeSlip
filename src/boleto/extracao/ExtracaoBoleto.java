package boleto.extracao;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.*;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONObject;

public class ExtracaoBoleto {

    private String caminhoPdf = "src/img/teste3";
    private File file = new File(caminhoPdf + ".pdf");

    // ✅ Método 1: Transforma PDF em .txt
    public String transformarPdfToTxt() throws IOException {
        PDDocument document = Loader.loadPDF(file);
        PDFTextStripper pdfStripper = new PDFTextStripper();
        String text = pdfStripper.getText(document);
        document.close();

        FileWriter writer = new FileWriter(caminhoPdf + ".txt");
        writer.write(text);
        writer.close();

        System.out.println("Texto extraído e salvo com sucesso em '" + caminhoPdf + ".txt'.");
        //System.out.println(text);
        return text;
    }

    // ✅ Método 2: Processa o .txt gerado e extrai os campos
    public String processarTxt() throws IOException {
        String pdfToTxt = transformarPdfToTxt();
        String texto = pdfToTxt.replaceAll("\\s+", " ").trim();

        System.out.println("===================================");

        System.out.println("\nExtraindo informacoes do boleto:\n");

        JSONObject resultado = new JSONObject();

        resultado.put("Valor", extrairCampo("Valor", texto, Pattern.compile("(=)?\\s*VALOR\\s*COBRADO\\s*[:,]?\\s*([\\d.,-]+)")));
        resultado.put("Data de Vencimento", extrairCampo("Data de Vencimento", texto, Pattern.compile("VENCIMENTO[:\\s]*([\\d]{2}/[\\d]{2}/[\\d]{4})")));
        resultado.put("CNPJ do Beneficiario", extrairCampo("CNPJ do Beneficiario", texto, Pattern.compile("(CNPJ(:|\\s))?\\s*([\\d]{14}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2})")));
        resultado.put("Nome do Beneficiario", extrairCampo("Nome do Beneficiario", texto, Pattern.compile("(ISCP\\s*-?\\s*.*?\\s*LTDA)", Pattern.CASE_INSENSITIVE)));
        resultado.put("Banco Emissor", extrairCampo("Banco Emissor", texto, Pattern.compile("Banco\\s+do\\s+Brasil", Pattern.CASE_INSENSITIVE)));
        resultado.put("Codigo de Barras", extrairCampo("Codigo de Barras", texto, Pattern.compile("\\d{5}\\.\\d{5}\\s\\d{5}\\.\\d{6}\\s\\d{5}\\.\\d{6}\\s\\d\\s\\d{14}")));

        System.out.println("Valor: " + resultado.optString("Valor", "Não encontrado"));
        System.out.println("Data de Vencimento: " + resultado.optString("Data de Vencimento", "Não encontrado"));
        System.out.println("CNPJ do Beneficiário: " + resultado.optString("CNPJ do Beneficiario", "Não encontrado"));
        System.out.println("Nome do Beneficiário: " + resultado.optString("Nome do Beneficiario", "Não encontrado"));
        System.out.println("Banco Emissor: " + resultado.optString("Banco Emissor", "Não encontrado"));
        System.out.println("Codigo de Barras: " + resultado.optString("Codigo de Barras", "Não encontrado"));
        return resultado.toString(4);
    }

    // ✅ Método auxiliar de extração que retorna o valor
    private String extrairCampo(String nomeCampo, String texto, Pattern padrao) {
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
}
