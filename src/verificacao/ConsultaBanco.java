/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package verificacao;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.text.Normalizer; // Para remover acentos
import java.util.regex.Pattern; // Para remover acentos
import org.json.JSONObject;
import usuario.Boleto; // Certifique-se de que Boleto está no pacote correto

public class ConsultaBanco {

    private String codigoBancoBoleto; // Código do banco extraído do boleto
    private String nomeBancoBoleto;   // Nome do banco extraído do boleto (campo 'banco_emissor')

    // Construtor que recebe o objeto Boleto
    public ConsultaBanco(Boleto boleto) {
        if (boleto != null && boleto.getCodigoBarras() != null && boleto.getCodigoBarras().length() >= 3) {
            // Os 3 primeiros dígitos do código de barras são o código do banco
            this.codigoBancoBoleto = boleto.getCodigoBarras().substring(0, 3).trim();
        } else {
            this.codigoBancoBoleto = null; // Ou lance uma exceção, dependendo da sua regra de negócio
            System.err.println("⚠️ Não foi possível extrair o código do banco do boleto. Código de barras inválido ou ausente.");
        }
        this.nomeBancoBoleto = (boleto.getBancoEmissor() != null) ? boleto.getBancoEmissor().trim() : null;
    }

    /**
     * Normaliza uma string de nome de banco para facilitar a comparação.
     * Remove acentos, converte para minúsculas, remove termos comuns e espaços extras.
     *
     * @param nome O nome do banco a ser normalizado.
     * @return O nome do banco normalizado.
     */
    private String normalizarNomeBanco(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            return "";
        }
        // Remove acentos
        String nomeNormalizado = Normalizer.normalize(nome, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        nomeNormalizado = pattern.matcher(nomeNormalizado).replaceAll("");

        nomeNormalizado = nomeNormalizado.toLowerCase()
                .replace("s.a.", "")
                .replace("s/a", "")
                .replace("ltda.", "")
                .replace("bco", "banco") // Padroniza "bco" para "banco"
                .replaceAll("[^a-z0-9\\s]", "") // Remove caracteres especiais, exceto letras, números e espaços
                .replaceAll("\\s+", " ").trim(); // Remove espaços múltiplos e no início/fim
        return nomeNormalizado;
    }

    /**
     * Consulta a API da BrasilAPI para obter dados de um banco pelo seu código.
     *
     * @param codigoBanco O código do banco a ser consultado (ex: "001", "237").
     * @return Um array de String contendo [codigo_banco, nome_banco,
     * nome_completo_banco, ispb] ou null em caso de erro ou banco não
     * encontrado.
     */
    private String[] getDadosBancoDaApi(String codigoBanco) {
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
                // Adicionar leitura do corpo do erro, se houver
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

            // A API retorna o código como string para alguns bancos e int para outros.
            // Vamos garantir que seja sempre uma string.
            String code = null;
            if (json.has("code")) {
                Object codeObj = json.get("code");
                if (codeObj instanceof Number) {
                    // Formata para ter 3 dígitos, preenchendo com zeros à esquerda se necessário
                    code = String.format("%03d", ((Number) codeObj).intValue());
                } else if (codeObj instanceof String) {
                    code = (String) codeObj;
                } else if (codeObj != JSONObject.NULL){
                     code = String.valueOf(codeObj);
                }
            }

            String name = json.optString("name", null);
            String fullName = json.optString("fullName", null);
            String ispb = json.optString("ispb", null);

            System.out.println("✅ Dados da API para Banco " + codigoBanco + ":");
            System.out.println("   Código do Banco (API): " + (code != null ? code : "Não disponível"));
            System.out.println("   Nome do Banco (API): " + (name != null ? name : "Não disponível"));
            System.out.println("   Nome Completo do Banco (API): " + (fullName != null ? fullName : "Não disponível"));
            System.out.println("   ISPB: " + (ispb != null ? ispb : "Não disponível"));

            return new String[]{code, name, fullName, ispb};

        } catch (java.net.SocketTimeoutException e) {
            System.err.println("❌ Timeout ao conectar/ler da API BrasilAPI para banco " + codigoBanco + ": " + e.getMessage());
            return null;
        }
         catch (org.json.JSONException e) {
            System.err.println("❌ Erro ao processar JSON da API para banco " + codigoBanco + ": " + e.getMessage());
            return null;
        }
        catch (Exception e) {
            System.err.println("❌ Erro inesperado durante a consulta ou processamento da API para banco " + codigoBanco + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Valida o código e nome do banco do boleto comparando com os dados da
     * BrasilAPI.
     *
     * @return Uma String indicando o status da validação ('VALIDO', 'INVALIDO_CODIGO',
     * 'INVALIDO_NOME', 'ERRO_API', 'NAO_VALIDADO')
     */
    public String validarBancoComApi() {
        if (this.codigoBancoBoleto == null || this.codigoBancoBoleto.isEmpty()) {
            System.err.println("❌ Código do banco do boleto não encontrado. Não é possível validar com a API.");
            return "NAO_VALIDADO"; // Não há código para validar
        }

        String[] dadosApi = getDadosBancoDaApi(this.codigoBancoBoleto);

        if (dadosApi == null) {
            System.err.println("❌ Não foi possível obter dados do banco da API para validação.");
            return "ERRO_API";
        }

        String codigoApi = dadosApi[0];
        String nomeApi = dadosApi[1];
        String fullNameApi = dadosApi[2];

        // 1. Validação do Código do Banco
        // A API pode retornar o código como número, então formatamos para string com 3 dígitos.
        // O código do boleto já é string.
        if (codigoApi == null || !this.codigoBancoBoleto.equals(codigoApi)) {
            System.out.println("🚫 Código do banco do boleto ('" + this.codigoBancoBoleto + "') não bate com o da API ('" + (codigoApi != null ? codigoApi : "N/A") + "').");
            return "INVALIDO_CODIGO";
        }
        System.out.println("✅ Código do Banco ('" + this.codigoBancoBoleto + "') bate com o da API.");

        // 2. Validação do Nome do Banco (se disponível no boleto)
        if (this.nomeBancoBoleto == null || this.nomeBancoBoleto.isEmpty()) {
            System.out.println("ℹ️ Nome do banco não extraído do boleto. Validação do nome pulada. Banco considerado VÁLIDO pelo código.");
            return "VALIDO"; // Válido pelo código, nome não disponível para checagem.
        }

        String nomeBoletoNormalizado = normalizarNomeBanco(this.nomeBancoBoleto);
        String nomeApiNormalizado = normalizarNomeBanco(nomeApi);
        String fullNameApiNormalizado = normalizarNomeBanco(fullNameApi);

        System.out.println("   Nome Boleto (Original): '" + this.nomeBancoBoleto + "' -> Normalizado: '" + nomeBoletoNormalizado + "'");
        System.out.println("   Nome API (Curto): '" + (nomeApi != null ? nomeApi : "N/A") + "' -> Normalizado: '" + nomeApiNormalizado + "'");
        System.out.println("   Nome API (Completo): '" + (fullNameApi != null ? fullNameApi : "N/A") + "' -> Normalizado: '" + fullNameApiNormalizado + "'");

        // Tenta a correspondência com o nome curto ou nome completo da API
        boolean nomeCorresponde = false;
        if (!nomeApiNormalizado.isEmpty() && nomeBoletoNormalizado.contains(nomeApiNormalizado)) {
            nomeCorresponde = true;
        } else if (!fullNameApiNormalizado.isEmpty() && nomeBoletoNormalizado.contains(fullNameApiNormalizado)) {
            nomeCorresponde = true;
        }
        // Verificação adicional: se o nome da API (mais curto) está contido no nome do boleto
         else if (!nomeApiNormalizado.isEmpty() && !nomeBoletoNormalizado.isEmpty() && nomeApiNormalizado.contains(nomeBoletoNormalizado) ) {
            nomeCorresponde = true;
        }


        if (!nomeCorresponde) {
            System.out.println("🚫 Nome do banco do boleto (Normalizado: '" + nomeBoletoNormalizado +
                               "') não corresponde aos nomes da API (Normalizados: '" + nomeApiNormalizado +
                               "' / '" + fullNameApiNormalizado + "').");
            return "INVALIDO_NOME";
        }

        System.out.println("✅ **Nome do Banco bate com os dados da API (após normalização)!**");
        return "VALIDO";
    }
}
