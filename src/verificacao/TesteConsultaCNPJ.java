package verificacao;

import usuario.Boleto;

public class TesteConsultaCNPJ {
    public static void main(String[] args) {
        // Cria um boleto com CNPJ e nome beneficiário válidos
        Boleto boleto = new Boleto();
        boleto.setCnpjEmitente("00.000.000/0001-91"); // Coloque um CNPJ válido para teste, sem formatação ou com, pois você limpa depois
        boleto.setNomeBeneficiario("banco do brasil sa"); // Coloque o nome exato esperado pela API

        ConsultaCNPJ consulta = new ConsultaCNPJ(boleto);

        String resultado = consulta.validarDadosComApi();

        System.out.println("Resultado da validacao: " + resultado);

        // deve retornar ✅ Dados da API para CNPJ 00000000000191:
        //   CNPJ da API: 00000000000191
        //   Razão Social da API: banco do brasil sa
        //✅ **CNPJ e Razão Social batem com os dados da API!**
        //Resultado da validacao: VALIDO
    }
}
