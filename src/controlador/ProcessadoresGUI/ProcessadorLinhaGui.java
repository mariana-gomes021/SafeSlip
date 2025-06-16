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
import verificacao.ConsultaCNPJ;
import verificacao.ConsultaBanco;

public class ProcessadorLinhaGui {

    private RepositorioLinhaDigitavel repositorioLinhaDigitavel;
    private RepositorioUsuario repositorioUsuario;
    private RepositorioCnpjReputacao repositorioCnpjReputacao;
    private RepositorioCnpjEmitente repositorioCnpjEmitente;

    public ProcessadorLinhaGui() {
        this.repositorioLinhaDigitavel = new RepositorioLinhaDigitavel();
        this.repositorioUsuario = new RepositorioUsuario();
        this.repositorioCnpjReputacao = new RepositorioCnpjReputacao();
        this.repositorioCnpjEmitente = new RepositorioCnpjEmitente();
    }

    /**
     * Realiza as validações iniciais da linha digitável e preenche o objeto Boleto
     * com os dados extraídos e resultados das consultas às APIs (CNPJ e Banco).
     * Este método NÃO salva o boleto no banco de dados e NÃO atualiza a reputação.
     *
     * @param linhaDigitalInput A linha digitável informada pelo usuário.
     * @param valorStr O valor do pagamento como String.
     * @param dataVencimentoStr A data de vencimento como String (DD/MM/AAAA).
     * @param cnpjInformado O CNPJ digitado pelo usuário na GUI.
     * @return Um objeto Boleto preenchido com as validações preliminares.
     * @throws IllegalArgumentException Se os formatos de entrada (valor, data) forem inválidos.
     * @throws SQLException Se ocorrer um erro ao interagir com o banco de dados (especialmente RepositorioCnpjEmitente).
     */
    public Boleto validarDadosPreliminares(String linhaDigitalInput, String valorStr,
                                           String dataVencimentoStr, String cnpjInformado)
                                           throws IllegalArgumentException, SQLException {

        String linhaDigital = linhaDigitalInput.trim().replaceAll("[^0-9]", "");
        Boleto boleto = new Boleto();
        boleto.setCodigoBarras(linhaDigital);
        boleto.setDataExtracao(LocalDateTime.now());

        // --- 1. Validação da estrutura e dígitos verificadores da linha digitável ---
        boolean linhaDigitalEstruturaEVsValida = ValidadorLinhaDigitavel.validar(linhaDigital);
        if (!linhaDigitalEstruturaEVsValida) {
            boleto.setStatusValidacao("ERRO_ESTRUTURA_OU_DV_LD");
            boleto.setSuspeito(true);
            boleto.addDetalheFalha("Falha: Estrutura da linha digitável ou dígitos verificadores inválidos.");
        } else {
            boleto.setStatusValidacao("VALIDO_ESTRUTURA_LD");
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

        BigDecimal valorDoCodigoBarras = BigDecimal.ZERO;
        try {
            String valorExtractedStr = linhaDigital.substring(linhaDigital.length() - 10);
            String valorFormatado = valorExtractedStr.substring(0, 8) + "." + valorExtractedStr.substring(8, 10);
            valorDoCodigoBarras = new BigDecimal(valorFormatado);
            boleto.setValorExtraidoLinhaDigital(valorDoCodigoBarras);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            System.err.println("Erro ao extrair valor do codigo de barras da linha digitavel: " + e.getMessage());
            boleto.addDetalheFalha("Alerta: Não foi possível extrair o valor do código de barras da linha digitável.");
        }

        boolean valorBate = ValidadorLinhaDigitavel.validarValor(valorInformadoPeloUsuario, linhaDigital);
        if (!valorBate) {
            if ("VALIDO_ESTRUTURA_LD".equals(boleto.getStatusValidacao())) {
                boleto.setStatusValidacao("VALOR_DIVERGENTE");
            }
            boleto.setSuspeito(true);
            boleto.addDetalheFalha("Falha: Valor informado difere do valor extraído da linha digitável.");
        }

        // --- 4. Validação de CNPJ com ConsultaCNPJ ---
        String cnpjLimpo = cnpjInformado.trim().replaceAll("[^0-9]", "");
        boleto.setCnpjEmitente(cnpjLimpo);

        if (cnpjLimpo.length() != 14) {
            boleto.setStatusValidacaoCnpj("CNPJ_INVALIDO_FORMATO");
            boleto.setSuspeito(true);
            if (!boleto.getStatusValidacao().startsWith("ERRO")) { // Não sobrescreve um erro mais grave
                boleto.setStatusValidacao("ERRO_CNPJ_INVALIDO_FORMATO");
            }
            boleto.addDetalheFalha("Falha: CNPJ informado possui formato inválido.");
        } else {
            ConsultaCNPJ consultaCnpj = new ConsultaCNPJ(boleto); // Passa o boleto para a consulta preencher
            String statusConsultaCnpjApi = consultaCnpj.validarDadosComApi();
            boleto.setStatusValidacaoCnpj(statusConsultaCnpjApi);

            if (!"VALIDO".equals(statusConsultaCnpjApi)) {
                boleto.setSuspeito(true);
                // Define um status mais genérico se não for um erro de estrutura ou valor
                if (!boleto.getStatusValidacao().startsWith("ERRO_") && !"VALOR_DIVERGENTE".equals(boleto.getStatusValidacao())) {
                    boleto.setStatusValidacao("ALERTA_OU_ERRO_CNPJ");
                }
                if ("ERRO_API".equals(statusConsultaCnpjApi)) {
                    boleto.addDetalheFalha("Alerta: API do CNPJ offline ou erro na consulta.");
                } else {
                    boleto.addDetalheFalha("Alerta: CNPJ não validado pela API ou inconsistente. Status: " + statusConsultaCnpjApi);
                }
            }
        }

        // --- 5. Validação de Dados Bancários com ConsultaBanco ---
        String codigoBancoExtraido = "";
        if (linhaDigital.length() >= 3) {
            codigoBancoExtraido = linhaDigital.substring(0, 3);
            boleto.setBancoEmissor(codigoBancoExtraido);
        } else {
             boleto.setBancoEmissor("N/A");
             boleto.setStatusValidacaoBanco("ERRO_CODIGO_BANCO_INVALIDO");
             boleto.setSuspeito(true);
             if (!boleto.getStatusValidacao().startsWith("ERRO_")) {
                boleto.setStatusValidacao("ERRO_CODIGO_BANCO_INVALIDO");
            }
             boleto.addDetalheFalha("Falha: Não foi possível extrair o código do banco da linha digitável.");
        }

        if (codigoBancoExtraido.length() != 3) {
            boleto.setStatusValidacaoBanco("ERRO_CODIGO_BANCO_INVALIDO");
            boleto.setSuspeito(true);
            if (!boleto.getStatusValidacao().startsWith("ERRO_")) {
                boleto.setStatusValidacao("ERRO_CODIGO_BANCO_INVALIDO");
            }
            boleto.addDetalheFalha("Falha: Código do banco extraído tem formato inválido.");
        } else {
            ConsultaBanco consultaBanco = new ConsultaBanco(boleto); // Passa o boleto para a consulta preencher
            String statusConsultaBancoApi = consultaBanco.validarBancoComApi();
            boleto.setStatusValidacaoBanco(statusConsultaBancoApi);

            if (!"VALIDO".equals(statusConsultaBancoApi)) {
                boleto.setSuspeito(true);
                if (!boleto.getStatusValidacao().startsWith("ERRO_") && !"VALOR_DIVERGENTE".equals(boleto.getStatusValidacao()) && !"ALERTA_OU_ERRO_CNPJ".equals(boleto.getStatusValidacao())) {
                    boleto.setStatusValidacao("ALERTA_OU_ERRO_BANCO");
                }
                if ("ERRO_API".equals(statusConsultaBancoApi)) {
                    boleto.addDetalheFalha("Alerta: API do Banco offline ou erro na consulta.");
                } else {
                    boleto.addDetalheFalha("Alerta: Dados do banco não validados pela API ou inconsistentes. Status: " + statusConsultaBancoApi);
                }
            }
        }

        return boleto;
    }

    /**
     * Finaliza o processamento do boleto, aplicando as confirmações do usuário,
     * calculando a reputação e salvando o boleto no banco de dados.
     * Este método deve ser chamado APÓS o usuário confirmar os dados na GUI.
     *
     * @param boleto O objeto Boleto preenchido com as validações preliminares.
     * @param usuarioConfirmouCnpj Se o usuário confirmou os dados do CNPJ.
     * @param usuarioConfirmouBanco Se o usuário confirmou os dados do Banco.
     * @return O objeto Boleto finalizado com todos os status e reputação.
     * @throws SQLException Se ocorrer um erro ao interagir com o banco de dados.
     */
    public Boleto finalizarProcessamentoESalvar(Boleto boleto, boolean usuarioConfirmouCnpj, boolean usuarioConfirmouBanco) throws SQLException {

        // Inicializa o contador de falhas para esta fase final
        int verificacoesComFalha = 0;

        // Adiciona detalhes de falha se o usuário NÃO confirmou
        if (!usuarioConfirmouCnpj) {
            boleto.setInformacoesConfirmadasPeloUsuario(false);
            boleto.setStatusValidacaoCnpj("CNPJ_NAO_CONFIRMADO_USUARIO");
            boleto.setSuspeito(true);
            boleto.addDetalheFalha("Falha: Usuário NÃO confirmou os dados do CNPJ.");
            verificacoesComFalha++;
        } else {
            // Se o usuário confirmou, mas a API indicou um problema, mantenha o alerta
            if (!"VALIDO".equals(boleto.getStatusValidacaoCnpj())) {
                 boleto.setStatusValidacaoCnpj("CNPJ_CONFIRMADO_USUARIO_COM_ALERTA");
                 // Detalhe de falha já adicionado na fase preliminar se houvesse problema na API
                 verificacoesComFalha++;
            } else {
                boleto.setStatusValidacaoCnpj("VALIDO_CNPJ_API_E_USUARIO");
            }
        }

        if (!usuarioConfirmouBanco) {
            boleto.setInformacoesConfirmadasPeloUsuario(false);
            boleto.setStatusValidacaoBanco("BANCO_NAO_CONFIRMADO_USUARIO");
            boleto.setSuspeito(true);
            boleto.addDetalheFalha("Falha: Usuário NÃO confirmou os dados do Banco.");
            verificacoesComFalha++;
        } else {
            // Se o usuário confirmou, mas a API indicou um problema, mantenha o alerta
            if (!"VALIDO".equals(boleto.getStatusValidacaoBanco())) {
                boleto.setStatusValidacaoBanco("BANCO_CONFIRMADO_USUARIO_COM_ALERTA");
                // Detalhe de falha já adicionado na fase preliminar se houvesse problema na API
                verificacoesComFalha++;
            } else {
                boleto.setStatusValidacaoBanco("VALIDO_BANCO_API_E_USUARIO");
            }
        }
        
        // Se a flag geral de confirmação do usuário não foi definida como false ainda, e ambas foram true
        if (usuarioConfirmouCnpj && usuarioConfirmouBanco && !boleto.isInformacoesConfirmadasPeloUsuario()) {
            boleto.setInformacoesConfirmadasPeloUsuario(true);
        }
        

        // --- Lógica para determinar o status final geral do boleto ---
        // Se o boleto já foi marcado como ERRO ou ALERTA (na fase preliminar ou pelas confirmações),
        // ele mantém esse status, a menos que uma falha mais grave seja detectada.
        if (boleto.getStatusValidacao().startsWith("ERRO_") || "VALOR_DIVERGENTE".equals(boleto.getStatusValidacao())) {
            // Mantém o status de erro ou divergência de valor
        } else if (boleto.isSuspeito() || verificacoesComFalha > 0) {
            // Se foi marcado como suspeito em qualquer fase ou teve falhas na confirmação
            boleto.setStatusValidacao("ALERTA_GERAL_NAO_CONFORMIDADE");
        } else {
            // Se tudo passou e não há alertas/erros/suspeitas
            boleto.setStatusValidacao("VALIDO_COMPLETO");
        }


        // --- Reputação do boleto ---
        // A reputação é calculada com base no status final do boleto.
        // Se o boleto NÃO é VÁLIDO_COMPLETO, ele é considerado "falho" para a reputação.
        boolean isBoletoFalhoParaReputacao = !"VALIDO_COMPLETO".equals(boleto.getStatusValidacao());

        try {
            if (boleto.getCnpjEmitente() != null && !boleto.getCnpjEmitente().isEmpty()) {
                // 1. Atualiza os contadores e o score de reputação do CNPJ no banco de dados.
                // Passa 'isBoletoFalhoParaReputacao' para o método
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
                    int totalDenunciasCnpj = (int) reputacaoAtual[2]; // Assumindo que o índice 2 é total_suspeitas/denuncias

                    // 3. Atribui os valores de reputação ao objeto Boleto
                    boleto.setScoreReputacaoCnpj(score);
                    boleto.setTotalBoletosCnpj(totalBoletosCnpj);
                    boleto.setTotalDenunciasCnpj(totalDenunciasCnpj); // Define o total de denúncias/suspeitas

                    // 4. Classifica a reputação do CNPJ para exibição
                    String classificacao;
                    if (totalBoletosCnpj < 5) {
                        classificacao = "Insuficiente";
                    } else if (score.compareTo(new BigDecimal("80.00")) > 0) {
                        classificacao = "Confiável";
                    } else if (score.compareTo(new BigDecimal("50.00")) >= 0) {
                        classificacao = "Risco Moderado";
                    } else if (score.compareTo(BigDecimal.ZERO) == 0 && totalDenunciasCnpj >= 3) {
                        // Classificação "Reincidente" agora também considera 3+ denúncias para um score de 0
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
                        // mas o CNPJ emitente agora é considerado suspeito por reputação.
                        if ("VALIDO_COMPLETO".equals(boleto.getStatusValidacao())) {
                            boleto.setStatusValidacao("ALERTA_GERAL_NAO_CONFORMIDADE");
                        }
                        boleto.addDetalheFalha("Alerta: Boleto de CNPJ classificado como '" + classificacao + "' e com "
                                + totalDenunciasCnpj + " denúncias. Marcado como SUSPEITO!");
                        // Não incrementa verificacoesComFalha aqui, pois já estamos tratando o status do boleto
                        // com base no que veio da reputação.
                    }

                } else {
                    boleto.addDetalheFalha("Alerta: Não foi possível buscar a reputação do CNPJ. Pode ser um novo CNPJ ou um erro na busca.");
                    System.err.println("Não foi possível buscar a reputação do CNPJ. Pode ser um novo CNPJ ou um erro na busca.");
                }
            } else {
                boleto.addDetalheFalha("Alerta: Não foi possível calcular reputação: CNPJ Emitente não extraído ou vazio.");
                System.err.println("Não foi possível calcular reputação: CNPJ Emitente não extraído ou vazio.");
            }
        } catch (SQLException e) {
            boleto.addDetalheFalha("Falha: Erro ao processar reputação do CNPJ: " + e.getMessage());
            System.err.println("Erro ao processar reputação do CNPJ: " + e.getMessage());
            verificacoesComFalha++; // Conta como falha se houver exceção de SQL
            throw e; // Relança a exceção de SQL para tratamento da camada superior
        }

        // Lógica da Denuncia (se ainda for necessária aqui, pode precisar de ajuste)
        // Se a intenção é que Denuncia.validarTotalSuspeitas() use os dados do boleto,
        // o boleto precisa ser passado para ela ou ela deve ter acesso ao RepositorioCnpjReputacao.
        // Conforme está, ela não está diretamente ligando a esta instância de boleto.
        if (boleto.getCnpjEmitente() != null && !boleto.getCnpjEmitente().isEmpty()) {
            Denuncia denuncia = new Denuncia();
            denuncia.validarTotalSuspeitas(); // Isso parece ser uma lógica para a Denuncia em si, não para o Boleto.
        }

        // Atualiza a tabela cnpj_emitente (se o CNPJ for válido)
        if (boleto.getCnpjEmitente() != null && !boleto.getCnpjEmitente().isEmpty() &&
            !("CNPJ_INVALIDO_FORMATO".equals(boleto.getStatusValidacaoCnpj()))) {
            repositorioCnpjEmitente.inserirOuAtualizarCnpjEmitente(
                boleto.getCnpjEmitente(), boleto.getRazaoSocialApi());
        }

        // Associa o boleto a um usuário anônimo antes de salvar
        Usuario usuarioAnonimo = repositorioUsuario.criarUsuarioAnonimo();
        if (usuarioAnonimo == null || usuarioAnonimo.getId() == 0) {
            System.err.println("Falha crítica: Não foi possível criar um usuário anônimo. O boleto não será salvo.");
            boleto.addDetalheFalha("Erro crítico: Não foi possível criar usuário anônimo. Boleto não salvo.");
            throw new SQLException("Não foi possível criar usuário anônimo.");
        }
        boleto.setUsuarioId(usuarioAnonimo.getId());

        // Salva o boleto no banco de dados
        try {
            if (repositorioLinhaDigitavel.inserirBoletoPorLinhaDigitavel(boleto)) {
                System.out.println("Boleto salvo no banco de dados com sucesso!");
            } else {
                System.err.println("Falha desconhecida ao salvar o boleto.");
                boleto.addDetalheFalha("Erro: Falha desconhecida ao salvar o boleto no banco de dados.");
                verificacoesComFalha++; // Conta como falha se não conseguir salvar
            }
        } catch (SQLException e) {
            System.err.println("Erro ao salvar boleto no banco de dados: " + e.getMessage());
            boleto.addDetalheFalha("Erro: Erro ao salvar boleto no banco de dados: " + e.getMessage());
            verificacoesComFalha++; // Conta como falha se houver exceção
            throw e;
        }

        return boleto;
    }
}