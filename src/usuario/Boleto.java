package usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Boleto {

    private int id;
    private String codigoBarras; // Este campo armazena a linha digitável de 47 dígitos.
    private String cnpjEmitente;
    private BigDecimal valor; // Este é o valor informado pelo usuário.
    private LocalDate vencimento;
    private LocalDateTime dataExtracao;
    private String statusValidacao;
    private String statusValidacaoBanco;
    private String nomeBeneficiario;
    private String bancoEmissor;
    private boolean denunciado;
    private String nomeCnpjReceita;
    private int usuarioId;
    private boolean informacoesConfirmadasPeloUsuario;

    // Novos campos para armazenar dados da API
    private String razaoSocialApi;
    private String nomeFantasiaApi;
    private String situacaoCadastralApi;
    private String nomeBancoApi;          // Nome curto da API (ex: BCO BRADESCO S.A.)
    private String nomeCompletoBancoApi;  // Nome completo da API (ex: Banco Bradesco S.A.)
    private String ispbBancoApi; 


    // --- Getters e Setters Existentes ---
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public String getCnpjEmitente() {
        return cnpjEmitente;
    }

    public void setCnpjEmitente(String cnpjEmitente) {
        this.cnpjEmitente = cnpjEmitente;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDate getVencimento() {
        return vencimento;
    }

    public void setVencimento(LocalDate vencimento) {
        this.vencimento = vencimento;
    }

    public LocalDateTime getDataExtracao() {
        return dataExtracao;
    }

    public void setDataExtracao(LocalDateTime dataExtracao) {
        this.dataExtracao = dataExtracao;
    }

    public String getStatusValidacao() {
        return statusValidacao;
    }

    public void setStatusValidacao(String statusValidacao) {
        this.statusValidacao = statusValidacao;
    }

    public String getNomeBeneficiario() {
        return nomeBeneficiario;
    }

    public void setNomeBeneficiario(String nomeBeneficiario) {
        this.nomeBeneficiario = nomeBeneficiario;
    }

    public String getBancoEmissor() {
        return bancoEmissor;
    }

    public void setBancoEmissor(String bancoEmissor) {
        this.bancoEmissor = bancoEmissor;
    }

    public boolean isDenunciado() {
        return denunciado;
    }

    public void setDenunciado(boolean denunciado) {
        this.denunciado = denunciado;
    }

    public String getNomeCnpjReceita() {
        return nomeCnpjReceita;
    }

    public void setNomeCnpjReceita(String nomeCnpjReceita) {
        this.nomeCnpjReceita = nomeCnpjReceita;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getStatusValidacaoBanco() {
        return statusValidacaoBanco;
    }

    public void setStatusValidacaoBanco(String statusValidacaoBanco) {
        this.statusValidacaoBanco = statusValidacaoBanco;
    }

     public boolean isInformacoesConfirmadasPeloUsuario() {
        return informacoesConfirmadasPeloUsuario;
    }

    public void setInformacoesConfirmadasPeloUsuario(boolean informacoesConfirmadasPeloUsuario) {
        this.informacoesConfirmadasPeloUsuario = informacoesConfirmadasPeloUsuario;
    }

     /**
      * Extrai o valor do boleto a partir da linha digitável de 47 dígitos.
      * O valor está no bloco 5, nos últimos 10 dígitos (posições 38 a 47).
      * @return O valor do boleto como BigDecimal.
      */
     public BigDecimal getValorDoCodigoBarras() {
         if (this.codigoBarras == null || this.codigoBarras.length() != 47) {
             System.err.println("Erro: Linha digitável inválida para extração de valor. Esperado 47 dígitos, recebido " + (this.codigoBarras != null ? this.codigoBarras.length() : "null"));
             return BigDecimal.ZERO;
         }

        // Para linha digitável (47 dígitos), o valor está nos últimos 10 dígitos, precedidos pelo 4º dígito verificador.
        // O campo valor no padrão FEBRABAN está nas posições 38-47 (10 dígitos).
        // Em Java, substring(startIndex, endIndexExclusive).
        // Então, para pegar da posição 38 até 47, é substring(37, 47).
        String valorStr = this.codigoBarras.substring(37, 47);
        try {
            // Insere a vírgula para converter para BigDecimal
            String valorFormatado = valorStr.substring(0, 8) + "." + valorStr.substring(8, 10);
            return new BigDecimal(valorFormatado);
        } catch (NumberFormatException e) {
            System.err.println("Erro ao converter valor do código de barras da linha digitável: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // --- NOVOS GETTERS E SETTERS PARA DADOS DA API ---
    public String getRazaoSocialApi() {
        return razaoSocialApi;
    }

    public void setRazaoSocialApi(String razaoSocialApi) {
        this.razaoSocialApi = razaoSocialApi;
    }

    public String getNomeFantasiaApi() {
        return nomeFantasiaApi;
    }

    public void setNomeFantasiaApi(String nomeFantasiaApi) {
        this.nomeFantasiaApi = nomeFantasiaApi;
    }

    public String getSituacaoCadastralApi() {
        return situacaoCadastralApi;
    }

    public void setSituacaoCadastralApi(String situacaoCadastralApi) {
        this.situacaoCadastralApi = situacaoCadastralApi;
    }

      public String getNomeBancoApi() {
        return nomeBancoApi;
    }

    public void setNomeBancoApi(String nomeBancoApi) {
        this.nomeBancoApi = nomeBancoApi;
    }

    public String getNomeCompletoBancoApi() {
        return nomeCompletoBancoApi;
    }

    public void setNomeCompletoBancoApi(String nomeCompletoBancoApi) {
        this.nomeCompletoBancoApi = nomeCompletoBancoApi;
    }

    public String getIspbBancoApi() {
        return ispbBancoApi;
    }

    public void setIspbBancoApi(String ispbBancoApi) {
        this.ispbBancoApi = ispbBancoApi;
    }


}