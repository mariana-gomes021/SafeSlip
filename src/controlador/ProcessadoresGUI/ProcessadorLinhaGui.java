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

    // ... (Seu método validarDadosPreliminares permanece o mesmo) ...
    public Boleto validarDadosPreliminares(String linhaDigitalInput, String valorStr,
                                           String dataVencimentoStr, String cnpjInformado)
                                           throws IllegalArgumentException, SQLException {
        // ... (conteúdo do validarDadosPreliminares) ...
        String linhaDigital = linhaDigitalInput.trim().replaceAll("[^0-9]", "");
        Boleto boleto = new Boleto();
        boleto.setCodigoBarras(linhaDigital);
        boleto.setDataExtracao(LocalDateTime.now());

        boolean linhaDigitalEstruturaEVsValida = ValidadorLinhaDigitavel.validar(linhaDigital);
        if (!linhaDigitalEstruturaEVsValida) {
            boleto.setStatusValidacao("ERRO_ESTRUTURA_OU_DV_LD");
            boleto.setSuspeito(true);
            boleto.addDetalheFalha("Falha: Estrutura da linha digitável ou dígitos verificadores inválidos.");
        } else {
            boleto.setStatusValidacao("VALIDO_ESTRUTURA_LD");
        }

        LocalDate vencimento = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        try {
            vencimento = LocalDate.parse(dataVencimentoStr.trim(), formatter);
            boleto.setVencimento(vencimento);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Formato de data de vencimento inválido. Use o formato DD/MM/AAAA.");
        }

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

        String cnpjLimpo = cnpjInformado.trim().replaceAll("[^0-9]", "");
        boleto.setCnpjEmitente(cnpjLimpo);

        if (cnpjLimpo.length() != 14) {
            boleto.setStatusValidacaoCnpj("CNPJ_INVALIDO_FORMATO");
            boleto.setSuspeito(true);
            if (!boleto.getStatusValidacao().startsWith("ERRO")) {
                boleto.setStatusValidacao("ERRO_CNPJ_INVALIDO_FORMATO");
            }
            boleto.addDetalheFalha("Falha: CNPJ informado possui formato inválido.");
        } else {
            ConsultaCNPJ consultaCnpj = new ConsultaCNPJ(boleto);
            String statusConsultaCnpjApi = consultaCnpj.validarDadosComApi();
            boleto.setStatusValidacaoCnpj(statusConsultaCnpjApi);

            if (!"VALIDO".equals(statusConsultaCnpjApi)) {
                boleto.setSuspeito(true);
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
            ConsultaBanco consultaBanco = new ConsultaBanco(boleto);
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
            if (!"VALIDO".equals(boleto.getStatusValidacaoCnpj())) {
                 boleto.setStatusValidacaoCnpj("CNPJ_CONFIRMADO_USUARIO_COM_ALERTA");
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
            if (!"VALIDO".equals(boleto.getStatusValidacaoBanco())) {
                boleto.setStatusValidacaoBanco("BANCO_CONFIRMADO_USUARIO_COM_ALERTA");
                verificacoesComFalha++;
            } else {
                boleto.setStatusValidacaoBanco("VALIDO_BANCO_API_E_USUARIO");
            }
        }

        if (usuarioConfirmouCnpj && usuarioConfirmouBanco && !boleto.isInformacoesConfirmadasPeloUsuario()) {
            boleto.setInformacoesConfirmadasPeloUsuario(true);
        }


        // --- Lógica para determinar o status final geral do boleto ---
        if (boleto.getStatusValidacao().startsWith("ERRO_") || "VALOR_DIVERGENTE".equals(boleto.getStatusValidacao())) {
            // Mantém o status de erro ou divergência de valor
        } else if (boleto.isSuspeito() || verificacoesComFalha > 0) {
            boleto.setStatusValidacao("ALERTA_GERAL_NAO_CONFORMIDADE");
        } else {
            boleto.setStatusValidacao("VALIDO_COMPLETO");
        }

        // --- COMEÇO DA MUDANÇA CRÍTICA AQUI ---
        // 1. GARANTIR QUE O CNPJ_EMITENTE EXISTA OU SEJA CRIADO PRIMEIRO
        if (boleto.getCnpjEmitente() != null && !boleto.getCnpjEmitente().isEmpty() &&
            !("CNPJ_INVALIDO_FORMATO".equals(boleto.getStatusValidacaoCnpj()))) {
            try {
                // Insere ou atualiza o registro na tabela CNPJ_Emitente
                repositorioCnpjEmitente.inserirOuAtualizarCnpjEmitente(
                    boleto.getCnpjEmitente(), boleto.getRazaoSocialApi());
            } catch (SQLException e) {
                System.err.println("Erro ao inserir/atualizar CNPJ_Emitente: " + e.getMessage());
                boleto.addDetalheFalha("Erro: Falha ao persistir CNPJ do emitente: " + e.getMessage());
                verificacoesComFalha++; // Contabiliza como falha
                throw e; // Relança a exceção pois é um erro crítico para a FK
            }
        } else {
            // Se o CNPJ não é válido ou está faltando, não podemos continuar com a reputação.
            boleto.addDetalheFalha("Alerta: CNPJ Emitente inválido ou ausente. Reputação não processada.");
            System.err.println("CNPJ Emitente inválido ou ausente. Reputação não processada.");
            // Não precisa de throw aqui, pois a validação inicial já pegou o erro de formato.
        }


        // --- Reputação do boleto ---
        // A reputação é calculada com base no status final do boleto.
        // Se o boleto NÃO é VÁLIDO_COMPLETO, ele é considerado "falho" para a reputação.
        boolean isBoletoFalhoParaReputacao = !"VALIDO_COMPLETO".equals(boleto.getStatusValidacao());

        // Agora, com a certeza de que CNPJ_Emitente existe (se o CNPJ for válido), podemos operar em CNPJ_Reputacao
        if (boleto.getCnpjEmitente() != null && !boleto.getCnpjEmitente().isEmpty() &&
            !("CNPJ_INVALIDO_FORMATO".equals(boleto.getStatusValidacaoCnpj()))) {
            try {
                // 2. Atualiza os contadores e o score de reputação do CNPJ no banco de dados.
                repositorioCnpjReputacao.atualizarReputacaoCnpj(
                        boleto.getCnpjEmitente(),
                        isBoletoFalhoParaReputacao,
                        boleto.getTotalAtualizacoes() // Passa o total de atualizações deste boleto específico
                );

                // 3. Busca os dados de reputação atualizados do CNPJ no banco de dados.
                Object[] reputacaoAtual = repositorioCnpjReputacao.buscarReputacaoCnpj(boleto.getCnpjEmitente());

                if (reputacaoAtual != null) {
                    BigDecimal score = (BigDecimal) reputacaoAtual[0];
                    int totalBoletosCnpj = (int) reputacaoAtual[1];
                    int totalDenunciasCnpj = (int) reputacaoAtual[2]; // Assumindo que o índice 2 é total_suspeitas/denuncias

                    boleto.setScoreReputacaoCnpj(score);
                    boleto.setTotalBoletosCnpj(totalBoletosCnpj);
                    boleto.setTotalDenunciasCnpj(totalDenunciasCnpj);

                    String classificacao;
                    if (totalBoletosCnpj < 5) {
                        classificacao = "Insuficiente";
                    } else if (score.compareTo(new BigDecimal("80.00")) > 0) {
                        classificacao = "Confiável";
                    } else if (score.compareTo(new BigDecimal("50.00")) >= 0) {
                        classificacao = "Risco Moderado";
                    } else if (score.compareTo(BigDecimal.ZERO) == 0 && totalDenunciasCnpj >= 3) {
                        classificacao = "Reincidente";
                    } else {
                        classificacao = "Problemático";
                    }
                    boleto.setClassificacaoReputacao(classificacao);

                    if ((classificacao.equals("Reincidente") || classificacao.equals("Problemático"))
                            && totalDenunciasCnpj >= 5) {
                        boleto.setSuspeito(true);
                        if ("VALIDO_COMPLETO".equals(boleto.getStatusValidacao())) {
                            boleto.setStatusValidacao("ALERTA_GERAL_NAO_CONFORMIDADE");
                        }
                        boleto.addDetalheFalha("Alerta: Boleto de CNPJ classificado como '" + classificacao + "' e com "
                                + totalDenunciasCnpj + " denúncias. Marcado como SUSPEITO!");
                    }

                } else {
                    boleto.addDetalheFalha("Alerta: Não foi possível buscar a reputação do CNPJ. Pode ser um novo CNPJ ou um erro na busca.");
                    System.err.println("Não foi possível buscar a reputação do CNPJ. Pode ser um novo CNPJ ou um erro na busca.");
                }
            } catch (SQLException e) {
                boleto.addDetalheFalha("Falha: Erro ao processar reputação do CNPJ: " + e.getMessage());
                System.err.println("Erro ao processar reputação do CNPJ: " + e.getMessage());
                verificacoesComFalha++;
                throw e;
            }
        } else {
            // Este else só é atingido se o CNPJ já for inválido/ausente do if externo,
            // garantindo que não se tenta processar a reputação para um CNPJ inválido.
            // A mensagem de falha já foi adicionada no bloco anterior.
        }

        // Lógica da Denuncia (se ainda for necessária aqui, pode precisar de ajuste)
        if (boleto.getCnpjEmitente() != null && !boleto.getCnpjEmitente().isEmpty()) {
            Denuncia denuncia = new Denuncia();
            denuncia.validarTotalSuspeitas();
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
                verificacoesComFalha++;
            }
        } catch (SQLException e) {
            System.err.println("Erro ao salvar boleto no banco de dados: " + e.getMessage());
            boleto.addDetalheFalha("Erro: Erro ao salvar boleto no banco de dados: " + e.getMessage());
            verificacoesComFalha++;
            throw e;
        }

        return boleto;
    }
}