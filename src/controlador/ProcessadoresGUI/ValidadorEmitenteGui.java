package controlador.ProcessadoresGUI; // Mantenha o pacote original para ValidadorEmitente

import usuario.Boleto;
import java.sql.SQLException;
import bancodedados.RepositorioCnpjEmitente;
// Certifique-se de que ConsultaCNPJ e ConsultaBanco estão acessíveis
import verificacao.ConsultaBanco; // Se estiver em outro pacote, ajuste
 import verificacao.ConsultaCNPJ;// Se estiver em outro pacote, ajuste


public class ValidadorEmitenteGui {

    private Boleto boleto;
    private RepositorioCnpjEmitente repositorioCnpjEmitente;

    // Construtor sem Scanner para uso na GUI
    public ValidadorEmitenteGui(Boleto boleto) {
        this.boleto = boleto;
        this.repositorioCnpjEmitente = new RepositorioCnpjEmitente();
    }

    /**
     * Realiza a validação do CNPJ do beneficiário usando a BrasilAPI.
     * Esta é a Verificação 3.
     * NOTA: Este método AGORA NÃO INTERAGE COM O USUÁRIO VIA CONSOLE.
     * Ele apenas consulta a API e preenche o objeto Boleto.
     * A confirmação do usuário será feita na camada da GUI (TelaProcessadorLinha).
     *
     * @param cnpjInformado O CNPJ informado pelo usuário na GUI.
     * @return String indicando o status da consulta da API (VALIDO, INVALIDO, ERRO_API, etc.).
     */
    public String validarCnpjBeneficiario(String cnpjInformado) {
        String statusApi = "ERRO_FORMATO_CNPJ"; // Status padrão de erro

        // Limpa e define o CNPJ no boleto
        cnpjInformado = cnpjInformado.trim().replaceAll("[^0-9]", "");
        boleto.setCnpjEmitente(cnpjInformado);

        if (cnpjInformado.length() == 14) {
            ConsultaCNPJ consultaCnpj = new ConsultaCNPJ(boleto);
            statusApi = consultaCnpj.validarDadosComApi(); // Consulta a API e preenche boleto.razaoSocialApi etc.

            try {
                // Inserir/atualizar CNPJ na tabela CNPJ_Emitente
                repositorioCnpjEmitente.inserirOuAtualizarCnpjEmitente(cnpjInformado, boleto.getRazaoSocialApi());
            } catch (SQLException e) {
                // Logar o erro, não imprimir no console para GUI
                System.err.println("Erro ao tentar inserir/atualizar CNPJ emitente no banco de dados: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
             // Formato inválido, já está no status padrão "ERRO_FORMATO_CNPJ"
        }
        return statusApi; // Retorna o status da consulta API
    }

    /**
     * Realiza a validação dos dados bancários usando a BrasilAPI.
     * Esta é a Verificação 4.
     * NOTA: Este método AGORA NÃO INTERAGE COM O USUÁRIO VIA CONSOLE.
     * Ele apenas consulta a API e preenche o objeto Boleto.
     * A confirmação do usuário será feita na camada da GUI (TelaProcessadorLinha).
     *
     * @return String indicando o status da consulta da API (VALIDO, INVALIDO, ERRO_API, etc.).
     */
    public String validarDadosBancarios() {
        String codigoBanco = boleto.getCodigoBarras().substring(0, 3);
        boleto.setBancoEmissor(codigoBanco); // Define o código do banco extraído da linha digitável

        ConsultaBanco consultaBanco = new ConsultaBanco(boleto);
        String statusApi = consultaBanco.validarBancoComApi(); // Consulta a API e preenche nomeBancoApi etc.

        return statusApi; // Retorna o status da consulta API
    }
    
    // Método para definir a confirmação do usuário (chamado pela GUI)
    public void setInformacoesConfirmadasPeloUsuario(boolean confirmado) {
        boleto.setInformacoesConfirmadasPeloUsuario(confirmado);
    }
}