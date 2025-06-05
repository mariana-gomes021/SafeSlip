package verificacao;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import org.json.JSONObject;
import usuario.Boleto;

public class ConsultaCNPJ {

    Boleto boleto = new Boleto();
    //private String cnpj2 = boleto.getCnpjEmitente();
    //private String razaoSocial2 = boleto.getNomeCnpjReceita().toLowerCase();
    private String razaoSocial = "ISCP - SOCIEDADE EDUCACIONAL LTDA".toLowerCase();
    private String cnpj = "6259640800015";
    private String url = "https://brasilapi.com.br/api/cnpj/v1/" + cnpj;

    public String[] getApi() {
        try {

            URI uri = new URI(url);
            HttpURLConnection conexao = (HttpURLConnection) uri.toURL()
                    .openConnection();
            conexao.setRequestMethod("GET");
            conexao.setRequestProperty("Accept", "application/json");

            int status = conexao.getResponseCode();
            //System.out.println(status);

            if (status != 200) {
                System.out.println("Erro ao consultar API. Código HTTP: "
                        + status);
                return null;
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conexao.getInputStream()));
            StringBuffer resposta = new StringBuffer();
            String linha;

            while ((linha = reader.readLine()) != null) {
                resposta.append(linha);
            }

            reader.close();

            JSONObject json = new JSONObject(resposta.toString());

            System.out.println("CNPJ: " + json.getString("cnpj"));
            System.out.println("Razao Social: " + json.getString("razao_social"));

            conexao.disconnect();

            String[] cnpjRazaoSocial = {json.getString("cnpj"),
                json.getString("razao_social").toLowerCase().replace(".", "")};;

            return cnpjRazaoSocial;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String validarRazaoSocial() {
        String[] api = getApi();

        if (api == null) {
            return null;
        }

        String razaoSocialApi = api[1];
        System.out.println(razaoSocialApi);
        
        if (!razaoSocialApi.equals(razaoSocial)) {
            System.out.println("Nome da empresa nao bate com o nome da api");
            return null;
        }

        System.out.println("Nome e CNPJ batem com o da API");
        return null;
    }


//    public String validarDenuncia (){
//        
//    }
}
