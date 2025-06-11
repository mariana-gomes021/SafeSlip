package verificacao;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import org.json.JSONObject;
import usuario.Boleto;

public class ConsultaCNPJ {

    private Boleto boleto;
    private String cnpjBoleto;

    public ConsultaCNPJ(Boleto boleto) {
        this.boleto = boleto;
        this.cnpjBoleto = (boleto.getCnpjEmitente() != null) ? boleto.getCnpjEmitente().replaceAll("[^0-9]", "") : null;
    }

    /**
     * Consulta a API da BrasilAPI para obter dados de um CNPJ.
     * Agora retorna um JSONObject completo ou null em caso de erro/não encontrado.
     */
    public JSONObject getDadosCnpjDaApi(String cnpj) {
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
            StringBuilder resposta = new StringBuilder();
            String linha;

            while ((linha = reader.readLine()) != null) {
                resposta.append(linha);
            }

            reader.close();
            conexao.disconnect();

            JSONObject json = new JSONObject(resposta.toString());

            // Verifica se a API retornou um erro lógico (ex: CNPJ não encontrado)
            if (json.has("message") && json.getString("message").contains("não encontrado")) {
                 System.err.println("⚠️ CNPJ não encontrado na API: " + cnpj);
                 return null; // Retorna null se CNPJ não for encontrado
            }
            
            // Se chegou aqui, a consulta foi bem-sucedida e o CNPJ foi encontrado
            return json;

        } catch (Exception e) {
            System.err.println("❌ Erro durante a consulta ou processamento da API para CNPJ " + cnpj + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Valida o CNPJ do boleto comparando com os dados da BrasilAPI.
     * Os dados da API são preenchidos diretamente no objeto Boleto.
     * @return Uma String indicando o status da validação ('VALIDO', 'ERRO_API', 'CNPJ_DIVERGENTE', 'DADOS_NAO_ENCONTRADOS_API').
     */
    public String validarDadosComApi() {
        if (this.cnpjBoleto == null || this.cnpjBoleto.isEmpty()) {
            System.err.println("❌ CNPJ do boleto não encontrado. Não é possível validar com a API.");
            return "CNPJ_AUSENTE"; 
        }

        JSONObject dadosApi = getDadosCnpjDaApi(this.cnpjBoleto);

        if (dadosApi == null) {
            System.err.println("❌ Não foi possível obter dados da API para validação ou CNPJ não encontrado na API.");
            // Define os campos da API como "N/A" para indicar que não foram encontrados
            boleto.setRazaoSocialApi("N/A");
            boleto.setNomeFantasiaApi("N/A");
            boleto.setSituacaoCadastralApi("N/A");
            return "DADOS_NAO_ENCONTRADOS_API"; // Novo status para indicar falha na consulta ou CNPJ não encontrado
        }

        // Extrai e preenche os dados no objeto Boleto
        String cnpjApiRetornado = dadosApi.optString("cnpj", "").replaceAll("[^0-9]", "");
        String razaoSocialApi = dadosApi.optString("razao_social", "Não Informado na API");
        String nomeFantasiaApi = dadosApi.optString("nome_fantasia", "Não Informado na API");
        String situacaoCadastralApi = dadosApi.optString("situacao_cadastral", "Não Informado na API");

        boleto.setRazaoSocialApi(razaoSocialApi);
        boleto.setNomeFantasiaApi(nomeFantasiaApi);
        boleto.setSituacaoCadastralApi(situacaoCadastralApi);

        // Compara CNPJ para verificar se o que foi pesquisado é o que foi retornado
        String cnpjBoletoNormalizado = this.cnpjBoleto.replaceAll("[^0-9]", "");
        if (!cnpjBoletoNormalizado.equals(cnpjApiRetornado)) {
            System.err.println("🚫 CNPJ pesquisado (" + cnpjBoletoNormalizado + ") difere do CNPJ retornado pela API (" + cnpjApiRetornado + ").");
            return "CNPJ_DIVERGENTE";
        }

        System.out.println("✅ Dados do CNPJ recebidos da API.");
        return "VALIDO_API"; // Indica que a API retornou dados válidos para o CNPJ
    }
}