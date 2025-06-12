package verificacao;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.text.Normalizer;
import java.util.regex.Pattern;
import org.json.JSONObject;
import usuario.Boleto;

public class ConsultaBanco {

    private Boleto boleto; // O objeto Boleto agora é uma propriedade da classe
    private String codigoBancoExtraidoDoBoleto; // Código extraído para consulta

    // Construtor que recebe o objeto Boleto
    public ConsultaBanco(Boleto boleto) {
        this.boleto = boleto; // Armazena o objeto Boleto
        if (boleto != null && boleto.getCodigoBarras() != null && boleto.getCodigoBarras().length() >= 3) {
            // Os 3 primeiros dígitos do código de barras (ou linha digitável) são o código do banco
            // Assumimos que boleto.getBancoEmissor() já retorna o código de 3 dígitos,
            // ou você o extrai diretamente do código de barras
            this.codigoBancoExtraidoDoBoleto = boleto.getCodigoBarras().substring(0, 3).trim();
        } else {
            this.codigoBancoExtraidoDoBoleto = null;
            System.err.println("⚠️ Não foi possível extrair o código do banco do boleto. Código de barras inválido ou ausente.");
        }
        // O nomeBancoBoleto não é mais necessário para a validação automática,
        // pois a validação de nome será feita pela confirmação do usuário no ProcessadorLinhaDigitavel.
    }

    /**
     * Normaliza uma string de nome de banco para facilitar a comparação.
     * REMOVIDO: Este método não é mais usado na validação automática do nome.
     */
    // private String normalizarNomeBanco(String nome) { /* ... */ }

    /**
     * Consulta a API da BrasilAPI para obter dados de um banco pelo seu código.
     * Agora retorna um JSONObject completo ou null.
     */
    private JSONObject getDadosBancoDaApi(String codigoBanco) {
        if (codigoBanco == null || codigoBanco.trim().isEmpty()) {
            System.err.println("❌ Código do banco para consulta API é nulo ou vazio.");
            return null;
        }

        String urlApi = "https://brasilapi.com.br/api/banks/v1/" + codigoBanco.trim();

        try {
            URI uri = new URI(urlApi);
            HttpURLConnection conexao = (HttpURLConnection) uri.toURL().openConnection();
            conexao.setRequestMethod("GET");
            conexao.setRequestProperty("Accept", "application/json");

            conexao.setConnectTimeout(10000); // 10 segundos para tentar conectar
            conexao.setReadTimeout(15000);    // 15 segundos para ler os dados

            int status = conexao.getResponseCode();

            if (status == 404) {
                System.err.println("⚠️ Banco com código '" + codigoBanco + "' não encontrado na BrasilAPI (código HTTP 404).");
                return null;
            } else if (status != 200) {
                System.err.println("❌ Erro ao consultar API BrasilAPI para banco " + codigoBanco + ". Código HTTP: " + status);
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(conexao.getErrorStream()))) {
                    StringBuilder erroResposta = new StringBuilder();
                    String linhaErro;
                    while ((linhaErro = errorReader.readLine()) != null) {
                        erroResposta.append(linhaErro);
                    }
                    System.err.println("   Detalhe do erro: " + erroResposta.toString());
                } catch (Exception e) {
                    // Ignorar se não conseguir ler o corpo do erro
                }
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

            if (json.has("message") && json.getString("message").toLowerCase().contains("não encontrado")) {
                System.err.println("⚠️ Banco com código '" + codigoBanco + "' não encontrado na BrasilAPI (mensagem no corpo).");
                return null;
            }
            // Não imprimir aqui, vamos imprimir no ProcessadorLinhaDigitavel para confirmação do usuário.
            return json;

        } catch (java.net.SocketTimeoutException e) {
            System.err.println("❌ Timeout ao conectar/ler da API BrasilAPI para banco " + codigoBanco + ": " + e.getMessage());
            return null;
        } catch (org.json.JSONException e) {
            System.err.println("❌ Erro ao processar JSON da API para banco " + codigoBanco + ": " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("❌ Erro inesperado durante a consulta ou processamento da API para banco " + codigoBanco + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Valida o código do banco do boleto comparando com os dados da BrasilAPI.
     * Os dados da API são preenchidos diretamente no objeto Boleto.
     *
     * @return Uma String indicando o status da validação ('VALIDO_API', 'ERRO_API', 'CODIGO_DIVERGENTE', 'DADOS_NAO_ENCONTRADOS_API').
     */
    public String validarBancoComApi() {
        if (this.codigoBancoExtraidoDoBoleto == null || this.codigoBancoExtraidoDoBoleto.isEmpty()) {
            System.err.println("❌ Código do banco do boleto não encontrado. Não é possível validar com a API.");
            // Define os campos da API como "N/A" para indicar que não foram encontrados
            boleto.setNomeBancoApi("N/A");
            boleto.setNomeCompletoBancoApi("N/A");
            boleto.setIspbBancoApi("N/A");
            return "CODIGO_AUSENTE"; 
        }

        JSONObject dadosApi = getDadosBancoDaApi(this.codigoBancoExtraidoDoBoleto);

        if (dadosApi == null) {
            System.err.println("❌ Não foi possível obter dados do banco da API para validação ou banco não encontrado.");
            // Define os campos da API como "N/A" para indicar que não foram encontrados
            boleto.setNomeBancoApi("N/A");
            boleto.setNomeCompletoBancoApi("N/A");
            boleto.setIspbBancoApi("N/A");
            return "DADOS_NAO_ENCONTRADOS_API"; // Novo status para indicar falha na consulta ou banco não encontrado
        }

        // Extrai e preenche os dados no objeto Boleto
        String code = null;
        if (dadosApi.has("code")) {
            Object codeObj = dadosApi.get("code");
            if (codeObj instanceof Number) {
                code = String.format("%03d", ((Number) codeObj).intValue());
            } else if (codeObj instanceof String) {
                code = (String) codeObj;
            } else if (codeObj != JSONObject.NULL){
                code = String.valueOf(codeObj);
            }
        }
        
        String name = dadosApi.optString("name", "Não Informado na API");
        String fullName = dadosApi.optString("fullName", "Não Informado na API");
        String ispb = dadosApi.optString("ispb", "Não Informado na API");

        boleto.setNomeBancoApi(name);
        boleto.setNomeCompletoBancoApi(fullName);
        boleto.setIspbBancoApi(ispb);

        // Compara o código do banco
        if (code == null || !this.codigoBancoExtraidoDoBoleto.equals(code)) {
            System.err.println("🚫 Código do banco do boleto (" + this.codigoBancoExtraidoDoBoleto + ") difere do código retornado pela API (" + (code != null ? code : "N/A") + ").");
            return "CODIGO_DIVERGENTE";
        }
        
        System.out.println("✅ Dados do Banco recebidos da API.");
        return "VALIDO_API"; // Indica que a API retornou dados válidos para o código do banco
    }
}