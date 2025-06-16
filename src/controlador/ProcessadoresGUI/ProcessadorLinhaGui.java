package controlador.ProcessadoresGUI;

import usuario.Boleto;
import Denuncia.Denuncia; 
import boleto.ValidadorLinhaDigitavel;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import bancodedados.RepositorioLinhaDigitavel;
import bancodedados.RepositorioUsuario;
import bancodedados.RepositorioCnpjReputacao;
import bancodedados.RepositorioCnpjEmitente; 
import usuario.Usuario;

public class ProcessadorLinhaGui {

    private RepositorioLinhaDigitavel repositorioLinhaDigitavel;
    private RepositorioUsuario repositorioUsuario;
    private RepositorioCnpjReputacao repositorioCnpjReputacao;
    private RepositorioCnpjEmitente repositorioCnpjEmitente; 
    private ValidadorEmitenteGui validadorEmitenteGui; // Alterado para ValidadorEmitenteGui

    public ProcessadorLinhaGui() {
        this.repositorioLinhaDigitavel = new RepositorioLinhaDigitavel();
        this.repositorioUsuario = new RepositorioUsuario();
        this.repositorioCnpjReputacao = new RepositorioCnpjReputacao();
        this.repositorioCnpjEmitente = new RepositorioCnpjEmitente();
    }

    /**
     * Processa a linha digitável e os dados do boleto fornecidos pela GUI.
     * Não utiliza Scanner; recebe todos os dados como parâmetros e retorna um objeto Boleto.
     *
     * @param linhaDigitalInput A linha digitável informada pelo usuário.
     * @param valorStr O valor do pagamento como String.
     * @param dataVencimentoStr A data de vencimento como String (DD/MM/AAAA).
     * @param temDesconto Se o boleto possui desconto (booleano).
     * @param usuarioConfirmouCnpj Se o usuário confirmou os dados do CNPJ.
     * @param usuarioConfirmouBanco Se o usuário confirmou os dados do Banco.
     * @param cnpjInformado O CNPJ digitado pelo usuário na GUI.
     * @return Um objeto Boleto preenchido com os resultados das validações, ou null em caso de erro crítico
     * que impeça a criação do usuário anônimo ou o salvamento inicial.
     * @throws SQLException Se ocorrer um erro ao interagir com o banco de dados.
     * @throws IllegalArgumentException Se os formatos de entrada (valor, data) forem inválidos.
     */
    public Boleto processarLinhaDigitavel(String linhaDigitalInput, String valorStr,
                                          String dataVencimentoStr, boolean temDesconto,
                                          boolean usuarioConfirmouCnpj, boolean usuarioConfirmouBanco,
                                          String cnpjInformado)
                                          throws SQLException, IllegalArgumentException {

        // Limpa e normaliza a linha digitável
        String linhaDigital = linhaDigitalInput.trim().replaceAll("[^0-9]", "");

        Boleto boleto = new Boleto();
        boleto.setCodigoBarras(linhaDigital);
        boleto.setDataExtracao(LocalDateTime.now());
        
        // Instancia o ValidadorEmitenteGui com o objeto Boleto
        this.validadorEmitenteGui = new ValidadorEmitenteGui(boleto); // Alterado para ValidadorEmitenteGui

        // Variáveis para rastrear o sucesso de cada etapa de validação
        boolean linhaDigitalEstruturaEVsValida = false;
        boolean valorBate = false;
        String statusCnpj = "NAO_VALIDADO";
        String statusBanco = "NAO_VALIDADO";
        BigDecimal valorDoCodigoBarras = BigDecimal.ZERO; // Inicializa para uso em exibição

        // --- 1. Validação da estrutura e dígitos verificadores da linha digitável ---
        linhaDigitalEstruturaEVsValida = ValidadorLinhaDigitavel.validar(linhaDigital);
        if (!linhaDigitalEstruturaEVsValida) {
            boleto.setStatusValidacao("ERRO_ESTRUTURA_OU_DV_LD");
        } else {
            boleto.setStatusValidacao("VALIDO_ESTRUTURA_LD"); // Status inicial de sucesso
        }

        // --- 2. Processamento da Data de Vencimento ---
        LocalDate vencimento = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        try {
            vencimento = LocalDate.parse(dataVencimentoStr.trim(), formatter);
            boleto.setVencimento(vencimento);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de data de vencimento inválido. Use o formato DD/MM/AAAA.");
        }

        // --- 3. Processamento do Valor do Boleto ---
        BigDecimal valorInformadoPeloUsuario = null;
        try {
            valorInformadoPeloUsuario = new BigDecimal(valorStr.replace(",", "."));
            boleto.setValor(valorInformadoPeloUsuario);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Formato de valor inválido. Use o formato 00.00.");
        }

        // Extrai o valor do código de barras da linha digitável para comparação e exibição
        try {
            String valorExtractedStr = linhaDigital.substring(linhaDigital.length() - 10);
            String valorFormatado = valorExtractedStr.substring(0, 8) + "." + valorExtractedStr.substring(8, 10);
            valorDoCodigoBarras = new BigDecimal(valorFormatado);
            boleto.setValorExtraidoLinhaDigital(valorDoCodigoBarras); // Adicionado para armazenar no Boleto
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            // Logar o erro internamente, mas não parar a execução.
            System.err.println("Erro ao extrair valor do codigo de barras da linha digitavel: " + e.getMessage());
        }
        
        valorBate = ValidadorLinhaDigitavel.validarValor(valorInformadoPeloUsuario, linhaDigital);
        if (!valorBate) {
            if ("VALIDO_ESTRUTURA_LD".equals(boleto.getStatusValidacao())) {
                boleto.setStatusValidacao("VALOR_DIVERGENTE");
            }
        }

        // --- 4. Validação de CNPJ (com confirmação da GUI) ---
        // validadorEmitenteGui.validarCnpjBeneficiario() agora recebe o CNPJ como parâmetro e não interage com Scanner
        String statusConsultaCnpjApi = validadorEmitenteGui.validarCnpjBeneficiario(cnpjInformado); // Alterado para validadorEmitenteGui
        
        if (!usuarioConfirmouCnpj) {
            statusCnpj = "CNPJ_NAO_CONFIRMADO_USUARIO";
        } else {
            if ("VALIDO".equals(statusConsultaCnpjApi)) { // Status retornado pela ConsultaCNPJ
                statusCnpj = "VALIDO_CNPJ_API_E_USUARIO";
            } else if ("ERRO_API".equals(statusConsultaCnpjApi)) {
                statusCnpj = "CNPJ_CONFIRMADO_USUARIO_COM_ALERTA_API_OFFLINE";
            } else { // INVALIDO, ERRO_FORMATO_CNPJ etc.
                statusCnpj = "CNPJ_CONFIRMADO_USUARIO_COM_ALERTA"; // Confirmação do usuário mas API não validou
            }
        }
        boleto.setStatusValidacaoCnpj(statusCnpj);

        // --- 5. Validação de Dados Bancários (com confirmação da GUI) ---
        // validadorEmitenteGui.validarDadosBancarios() não interage com Scanner
        String statusConsultaBancoApi = validadorEmitenteGui.validarDadosBancarios(); // Alterado para validadorEmitenteGui

        if (!usuarioConfirmouBanco) {
            statusBanco = "BANCO_NAO_CONFIRMADO_USUARIO";
        } else {
            if ("VALIDO".equals(statusConsultaBancoApi)) { // Status retornado pela ConsultaBanco
                statusBanco = "VALIDO_BANCO_API_E_USUARIO";
            } else if ("ERRO_API".equals(statusConsultaBancoApi)) {
                statusBanco = "BANCO_CONFIRMADO_USUARIO_COM_ALERTA_API_OFFLINE";
            } else { // INVALIDO, ERRO, etc.
                statusBanco = "BANCO_CONFIRMADO_USUARIO_COM_ALERTA"; // Confirmação do usuário mas API não validou
            }
        }
        boleto.setStatusValidacaoBanco(statusBanco);
        
        // Define a flag geral de confirmação do usuário NO BOLETO, baseada em AMBAS as confirmações
        if (!usuarioConfirmouCnpj || !usuarioConfirmouBanco) {
            boleto.setInformacoesConfirmadasPeloUsuario(false);
        } else {
            boleto.setInformacoesConfirmadasPeloUsuario(true);
        }


        // --- Lógica para determinar o status final do boleto ---
        String statusFinal = boleto.getStatusValidacao();

        // Se a validação da estrutura, valor, CNPJ ou Banco falhou, ou se houve alerta de fraude
        if (!linhaDigitalEstruturaEVsValida || !valorBate ||
                !statusCnpj.equals("VALIDO_CNPJ_API_E_USUARIO") || // Verifica o status final do CNPJ
                !statusBanco.equals("VALIDO_BANCO_API_E_USUARIO") || // Verifica o status final do Banco
                "ALERTA_FRAUDE_NOME_CNPJ_DIVERGENTE".equals(statusCnpj)) { // Usar statusCnpj diretamente para detectar fraude

            // Se o status atual é de sucesso inicial (VALIDO_ESTRUTURA_LD)
            // ou se já é VALOR_DIVERGENTE, elevamos o nível para ALERTA_GERAL_NAO_CONFORMIDADE.
            // ERRO_ESTRUTURA_OU_DV_LD é o status mais grave, então não o substituímos por um "alerta geral".
            if ("VALIDO_ESTRUTURA_LD".equals(statusFinal) || "VALOR_DIVERGENTE".equals(statusFinal)) {
                statusFinal = "ALERTA_GERAL_NAO_CONFORMIDADE";
            }

            // O status de fraude sobrepõe outros alertas, pois é mais crítico
            if ("ALERTA_FRAUDE_NOME_CNPJ_DIVERGENTE".equals(statusCnpj)) {
                statusFinal = "ALERTA_FRAUDE_NOME_CNPJ_DIVERGENTE";
            }
            boleto.setSuspeito(true);
        } else {
            // Se todas as validações anteriores (estrutura, DVs, valor, CNPJ, Banco) passaram,
            // e não há alerta de fraude, então o boleto é considerado 'VALIDO_COMPLETO'.
            if ("VALIDO_ESTRUTURA_LD".equals(statusFinal)) { // Garante que a estrutura inicial era válida
                statusFinal = "VALIDO_COMPLETO";
            }
        }
        boleto.setStatusValidacao(statusFinal); // Define o status final geral do boleto

        // --- 6. Reputação do boleto ---
        // A reputação é calculada com base no status final do boleto até agora.
        boolean isBoletoFalhoParaReputacao = !"VALIDO_COMPLETO".equals(boleto.getStatusValidacao());

        try {
            // Garante que o CNPJ Emitente foi extraído e está disponível
            if (boleto.getCnpjEmitente() != null && !boleto.getCnpjEmitente().isEmpty()) {

                // 1. Atualiza os contadores e o score de reputação do CNPJ no banco de dados.
                repositorioCnpjReputacao.atualizarReputacaoCnpj(
                        boleto.getCnpjEmitente(),
                        isBoletoFalhoParaReputacao,
                        boleto.getTotalAtualizacoes() // Passa o total de atualizações deste boleto específico
                );

                // 2. Busca os dados de reputação atualizados do CNPJ no banco de dados.
                Object[] reputacaoAtual = repositorioCnpjReputacao.buscarReputacaoCnpj(boleto.getCnpjEmitente());

                if (reputacaoAtual != null) {
                    BigDecimal score = (BigDecimal) reputacaoAtual[0]; // Score já calculado pelo repositório
                    int totalBoletosCnpj = (int) reputacaoAtual[1];
                    int totalDenunciasCnpj = (int) reputacaoAtual[2];

                    // 3. Atribui os valores de reputação ao objeto Boleto
                    boleto.setScoreReputacaoCnpj(score);
                    boleto.setTotalBoletosCnpj(totalBoletosCnpj);
                    boleto.setTotalDenunciasCnpj(totalDenunciasCnpj);

                    // 4. Classifica a reputação do CNPJ para exibição
                    String classificacao;
                    if (totalBoletosCnpj < 5) {
                        classificacao = "Insuficiente";
                    } else if (score.compareTo(new BigDecimal("80.00")) > 0) {
                        classificacao = "Confiável";
                    } else if (score.compareTo(new BigDecimal("50.00")) >= 0) {
                        classificacao = "Risco Moderado";
                    } else if (score.compareTo(BigDecimal.ZERO) == 0 && totalDenunciasCnpj >= 3) {
                        classificacao = "Reincidente";
                    } else { // Score entre 0 (exclusive) e 50 (exclusive)
                        classificacao = "Problemático";
                    }
                    boleto.setClassificacaoReputacao(classificacao); // Armazena a classificação no Boleto

                    // 5. Marca o boleto como suspeito se a reputação for muito baixa e houver histórico suficiente
                    if ((classificacao.equals("Reincidente") || classificacao.equals("Problemático"))
                            && totalDenunciasCnpj >= 5) { // Limite de 5 denúncias para marcar automaticamente como suspeito
                        boleto.setSuspeito(true);
                        // Ajusta o status de validação do boleto se ele estava 'VALIDO_COMPLETO'
                        // mas o CNPJ emitente agora é considerado suspeito.
                        if ("VALIDO_COMPLETO".equals(boleto.getStatusValidacao())) {
                            boleto.setStatusValidacao("ALERTA_GERAL_NAO_CONFORMIDADE");
                        }
                    }

                } else {
                    System.err.println("Não foi possível buscar a reputação do CNPJ. Pode ser um novo CNPJ ou um erro na busca.");
                }
            } else {
                System.err.println("Não foi possível calcular reputação: CNPJ Emitente não extraído ou vazio.");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao processar reputação do CNPJ: " + e.getMessage());
            // Não relança a exceção aqui para não impedir o salvamento do boleto
        }

        if (boleto.getCnpjEmitente() != null && !boleto.getCnpjEmitente().isEmpty()) {
            Denuncia denuncia = new Denuncia();
            denuncia.validarTotalSuspeitas();
        }

        // Associa o boleto a um usuário anônimo antes de salvar
        Usuario usuarioAnonimo = repositorioUsuario.criarUsuarioAnonimo();
        if (usuarioAnonimo == null || usuarioAnonimo.getId() == 0) {
            System.err.println("Falha crítica: Não foi possível criar um usuário anônimo. O boleto não será salvo.");
            return null; // Retorna null se não puder criar o usuário anônimo
        }
        boleto.setUsuarioId(usuarioAnonimo.getId());

        // Salva o boleto no banco de dados
        try {
            if (repositorioLinhaDigitavel.inserirBoletoPorLinhaDigitavel(boleto)) {
                System.out.println("Boleto salvo no banco de dados com sucesso!");
            } else {
                System.err.println("Falha desconhecida ao salvar o boleto.");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao salvar boleto no banco de dados: " + e.getMessage());
            e.printStackTrace();
            throw e; // Lança a exceção para que o chamador (GUI) possa tratá-la
        }
        
        return boleto; // Retorna o objeto Boleto preenchido
    }
}