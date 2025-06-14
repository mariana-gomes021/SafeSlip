package usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Certifique-se de que esta classe está no pacote correto, como 'boleto'
public class Boleto {
    private String valor; // Manter como String para extração bruta, converter para BigDecimal depois
    private LocalDate vencimento;
    private String cnpjEmitente;
    private String nomeBeneficiario; // Nome extraído do PDF
    private String bancoEmissor; // Código do banco extraído do PDF ou linha digitável
    private String codigoBarras;
    private LocalDateTime dataExtracao;
    private String statusValidacao; // Ex: VALIDO, INVALIDO, ALERTA_FRAUDE_NOME_CNPJ_DIVERGENTE, etc.
    private String statusValidacaoCnpj;
    private String statusValidacaoBanco; // Ex: VALIDO_API, ERRO_API, BANCO_NAO_CONFIRMADO_USUARIO
    private boolean informacoesConfirmadasPeloUsuario;
    private int usuarioId; // ID do usuário que processou o boleto
    private boolean suspeito; // RENOMEADO DE 'denunciado' PARA 'suspeito'

    // Campos da BrasilAPI - CNPJ
    private String razaoSocialApi;
    private String nomeFantasiaApi;
    private String situacaoCadastralApi; // Pode ser útil manter, mesmo que não seja salvo no Boleto diretamente
    private String dataAberturaApi; // Pode ser útil, se extraído da API

    // Campos da BrasilAPI - Banco
    private String nomeBancoApi;
    private String nomeCompletoBancoApi;
    private String ispbBancoApi;
    private String codigoBancoApi;

    // Campos para a Reputação do CNPJ (não são persistidos no Boleto, mas são
    // preenchidos para exibição/lógica)
    private BigDecimal scoreReputacaoCnpj;
    private int totalBoletosCnpj;
    private int totalDenunciasCnpj;

    // NOVO CAMPO: Contador de atualizações para o boleto específico (lido do banco)
    private int totalAtualizacoes;

    // Construtor vazio
    public Boleto() {
        this.dataExtracao = LocalDateTime.now(); // Define a data de extração no momento da criação
        this.statusValidacao = "PENDENTE"; // Status inicial
        this.statusValidacaoBanco = "PENDENTE"; // Status inicial do banco
        this.informacoesConfirmadasPeloUsuario = false; // Padrão
        this.suspeito = false; // Padrão
        this.totalAtualizacoes = 0; // Padrão
    }

    // --- Getters e Setters ---

    public String getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) { // Aceita BigDecimal
        this.valor = valor != null ? valor.toPlainString() : null; // Converte para String
    }

    // Método para obter o valor como BigDecimal (útil para cálculos)
    public BigDecimal getValorAsBigDecimal() {
        try {
            return new BigDecimal(this.valor);
        } catch (NumberFormatException | NullPointerException e) {
            return BigDecimal.ZERO; // Retorna zero ou lança exceção, dependendo da sua regra de negócio
        }
    }

    public LocalDate getVencimento() {
        return vencimento;
    }

    public void setVencimento(LocalDate vencimento) {
        this.vencimento = vencimento;
    }

    public String getCnpjEmitente() {
        return cnpjEmitente;
    }

    public void setCnpjEmitente(String cnpjEmitente) {
        this.cnpjEmitente = cnpjEmitente;
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

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
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

    public String getStatusValidacaoCnpj() {
        return statusValidacaoCnpj;
    }

    public void setStatusValidacaoCnpj(String statusValidacaoCnpj) {
        this.statusValidacaoCnpj = statusValidacaoCnpj;
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

    public int getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    public boolean isSuspeito() { // RENOMEADO: antigo isDenunciado()
        return suspeito;
    }

    public void setSuspeito(boolean suspeito) { // RENOMEADO: antigo setDenunciado()
        this.suspeito = suspeito;
    }

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

    public String getDataAberturaApi() {
        return dataAberturaApi;
    }

    public void setDataAberturaApi(String dataAberturaApi) {
        this.dataAberturaApi = dataAberturaApi;
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

    public BigDecimal getScoreReputacaoCnpj() {
        return scoreReputacaoCnpj;
    }

    public void setScoreReputacaoCnpj(BigDecimal scoreReputacaoCnpj) {
        this.scoreReputacaoCnpj = scoreReputacaoCnpj;
    }

    public int getTotalBoletosCnpj() {
        return totalBoletosCnpj;
    }

    public void setTotalBoletosCnpj(int totalBoletosCnpj) {
        this.totalBoletosCnpj = totalBoletosCnpj;
    }

    public int getTotalDenunciasCnpj() {
        return totalDenunciasCnpj;
    }

    public void setTotalDenunciasCnpj(int totalDenunciasCnpj) {
        this.totalDenunciasCnpj = totalDenunciasCnpj;
    }

    public int getTotalAtualizacoes() { // NOVO GETTER
        return totalAtualizacoes;
    }

    public void setTotalAtualizacoes(int totalAtualizacoes) { // NOVO SETTER
        this.totalAtualizacoes = totalAtualizacoes;
    }

    public String getCodigoBancoApi() { // <--- Adicione este getter
        return codigoBancoApi;
    }

    public void setCodigoBancoApi(String codigoBancoApi) { // <--- Adicione este setter
        this.codigoBancoApi = codigoBancoApi;
    }
}
