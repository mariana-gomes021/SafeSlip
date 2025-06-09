package usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Boleto {

    private int id;
    private String codigoBarras;
    private String cnpjEmitente;
    private BigDecimal valor;
    private LocalDate vencimento;
    private LocalDateTime dataExtracao;
    private String statusValidacao; // Mantido para o status da validação do CNPJ
    private String statusValidacaoBanco; // Novo campo para o status da validação do Banco
    private String nomeBeneficiario;
    private String bancoEmissor;
    private boolean denunciado;
    private String nomeCnpjReceita;
    private int usuarioId;

    // Getters e Setters
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

    // Novo getter e setter para statusValidacaoBanco
    public String getStatusValidacaoBanco() {
        return statusValidacaoBanco;
    }

    public void setStatusValidacaoBanco(String statusValidacaoBanco) {
        this.statusValidacaoBanco = statusValidacaoBanco;
    }
}
