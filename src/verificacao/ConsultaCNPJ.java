package verificacao;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import org.json.JSONObject;
import usuario.Boleto; // Certifique-se de que Boleto está no pacote correto

public class ConsultaCNPJ {

    private Boleto boleto; // Agora, esta é uma instância do boleto a ser validado
    private String cnpjBoleto; // CNPJ extraído do boleto
    private String nomeBeneficiarioBoleto; // Nome do beneficiário extraído do boleto

    // Construtor que recebe o objeto Boleto
    public ConsultaCNPJ(Boleto boleto) {
        this.boleto = boleto;
        // Garante que o CNPJ esteja apenas com dígitos e o nome em minúsculas para comparação
        this.cnpjBoleto = (boleto.getCnpjEmitente() != null) ? boleto.getCnpjEmitente().replaceAll("[^0-9]", "") : null;
        this.nomeBeneficiarioBoleto = (boleto.getNomeBeneficiario() != null) ? boleto.getNomeBeneficiario().toLowerCase() : null;
    }

    /**
     * Consulta a API da BrasilAPI para obter dados de um CNPJ.
     * @param cnpj O CNPJ a ser consultado (apenas dígitos).
     * @return Um array de String contendo [cnpj_api, razao_social_api] ou null em caso de erro.
     */
    public String[] getDadosCnpjDaApi(String cnpj) {
        if (cnpj == null || cnpj.isEmpty()) {
            System.err.println("❌ CNPJ para consulta API é nulo ou vazio.");
            return null;
        }

        String urlApi = "https://minhareceita.org/" + cnpj;

        try {
            URI uri = new URI(urlApi);
            HttpURLConnection conexao = (HttpURLConnection) uri.toURL().openConnection();
            conexao.setRequestMethod("GET");
            conexao.setRequestProperty("Accept", "application/json");

            int status = conexao.getResponseCode();

            if (status != 200) {
                System.err.println("❌ Erro ao consultar API para CNPJ " + cnpj + ". Código HTTP: " + status);
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conexao.getInputStream()));
            StringBuilder resposta = new StringBuilder(); // Usar StringBuilder é mais eficiente que StringBuffer
            String linha;

            while ((linha = reader.readLine()) != null) {
                resposta.append(linha);
            }

            reader.close();
            conexao.disconnect();

            JSONObject json = new JSONObject(resposta.toString());

            // Verifica se a API retornou um erro (ex: CNPJ inválido ou não encontrado)
            // A BrasilAPI pode retornar status 200 mesmo com erro lógico no corpo
            if (json.has("message") && json.getString("message").contains("não encontrado")) {
                 System.err.println("⚠️ CNPJ não encontrado na API: " + cnpj);
                 return null;
            }

            String cnpjApi = json.getString("cnpj").replaceAll("[^0-9]", ""); // Normaliza para apenas dígitos
            String razaoSocialApi = json.getString("razao_social").toLowerCase();

            System.out.println("✅ Dados da API para CNPJ " + cnpj + ":");
            System.out.println("   CNPJ da API: " + cnpjApi);
            System.out.println("   Razão Social da API: " + razaoSocialApi);

            String[] dadosApi = {cnpjApi, razaoSocialApi};
            return dadosApi;

        } catch (Exception e) {
            System.err.println("❌ Erro durante a consulta ou processamento da API para CNPJ " + cnpj + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Valida o CNPJ e a Razão Social do boleto comparando com os dados da BrasilAPI.
     * @return Uma String indicando o status da validação ('VALIDO', 'INVALIDO', 'ERRO_API')
     * ou null se as informações essenciais do boleto estiverem faltando.
     */
    public String validarDadosComApi() {
        if (this.cnpjBoleto == null || this.cnpjBoleto.isEmpty()) {
            System.err.println("❌ CNPJ do boleto não encontrado. Não é possível validar com a API.");
            return "ERRO"; // Ou 'INVALIDO' dependendo da sua regra de negócio
        }
        if (this.nomeBeneficiarioBoleto == null || this.nomeBeneficiarioBoleto.isEmpty()) {
            System.err.println("❌ Nome do beneficiário do boleto não encontrado. Não é possível validar com a API.");
            return "ERRO";
        }

        String[] dadosApi = getDadosCnpjDaApi(this.cnpjBoleto);

        if (dadosApi == null) {
            System.err.println("❌ Não foi possível obter dados da API para validação. Considerar como 'ERRO'.");
            return "ERRO_API"; // Um novo status para indicar falha na consulta à API
        }

        String cnpjApi = dadosApi[0];
        String razaoSocialApi = dadosApi[1];
        
        // Remove quaisquer caracteres especiais, pontos e traços do CNPJ para comparação
        String cnpjBoletoNormalizado = this.cnpjBoleto.replaceAll("[^0-9]", "");
        String cnpjApiNormalizado = cnpjApi.replaceAll("[^0-9]", "");

        // Compara CNPJ (normalizado)
        if (!cnpjBoletoNormalizado.equals(cnpjApiNormalizado)) {
            System.out.println("🚫 CNPJ do boleto (" + cnpjBoletoNormalizado + ") não bate com o da API (" + cnpjApiNormalizado + ").");
            return "INVALIDO";
        }

        // Compara Razão Social (ambos em minúsculas para ignorar case)
        // Adicionei .trim() para remover espaços extras e .replace(".", "") para pontos
        // Você pode precisar de mais normalizações dependendo da variação dos nomes
        String nomeBoletoNormalizado = this.nomeBeneficiarioBoleto.trim().replace(".", "");
        String razaoApiNormalizada = razaoSocialApi.trim().replace(".", "");


        if (!nomeBoletoNormalizado.equals(razaoApiNormalizada)) {
            System.out.println("🚫 Nome do beneficiário do boleto ('" + nomeBoletoNormalizado + "') não bate com a Razão Social da API ('" + razaoApiNormalizada + "').");
            return "INVALIDO";
        }

        System.out.println("✅ **CNPJ e Razão Social batem com os dados da API!**");
        return "VALIDO";
    }
}