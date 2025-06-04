/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package verificacao;

/**
 *
 * @author DELL
 */

public class AlertaUsuario {

    public static void exibirErro(String mensagem) {
        System.out.println("❌ ERRO: " + mensagem);
    }

    public static void exibirSucesso(String mensagem) {
        System.out.println("✅ SUCESSO: " + mensagem);
    }

    public static void exibirAviso(String mensagem) {
        System.out.println("⚠️ AVISO: " + mensagem);
    }

    public static void exibirInfo(String mensagem) {
        System.out.println("ℹ️ INFO: " + mensagem);
    }

    // Mensagens prontas que você pode chamar direto
    public static void erroExtracao() {
        exibirErro("Não foi possível extrair os dados do boleto. Tente novamente.");
    }

    public static void erroCodigoInvalido() {
        exibirErro("O código de barras informado é inválido ou está corrompido.");
    }

    public static void alertaFraude() {
        exibirAviso("Este boleto tem indícios de fraude. Verifique os dados com atenção.");
    }

    public static void boletoValido() {
        exibirSucesso("Boleto verificado com sucesso. Nenhuma suspeita encontrada.");
    }

    public static void confirmacaoDados() {
        exibirInfo("Confira os dados extraídos e confirme se estão corretos.");
    }

    public static void denunciaRealizada() {
        exibirSucesso("Denúncia registrada com sucesso. Obrigado por ajudar!");
    }

    public static void erroBanco() {
        exibirErro("Erro ao acessar o banco de dados. Contate o suporte.");
    }
}

