package boleto;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author DELL
 */
public class EnvioBoleto {
    //Finalizado
    //Responsável pelo upload do PDF
    //Teste upload pdf
    private File arquivoPDF;
    public File selecionarArquivoPDF() {
        JFileChooser seletor = new JFileChooser();
        seletor.setDialogTitle("Selecione o boleto em PDF");
        seletor.setFileFilter(new FileNameExtensionFilter("Arquivos PDF", "pdf"));

        int resultado = seletor.showOpenDialog(null);

        if (resultado == JFileChooser.APPROVE_OPTION) {
            arquivoPDF = seletor.getSelectedFile();
            System.out.println("Arquivo selecionado: " + arquivoPDF.getAbsolutePath());
            return arquivoPDF;
        } else {
            System.out.println("Nenhum arquivo selecionado.");
            return null;
        }
    }

    public File getArquivoPDF() {
        return arquivoPDF;
    }

    public void setArquivoPDF(File arquivoPDF) {
        this.arquivoPDF = arquivoPDF;
    }
    
}