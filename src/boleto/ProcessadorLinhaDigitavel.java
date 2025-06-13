// Em boleto/ProcessadorLinhaDigitavel.java

package boleto;

import boleto.ValidadorLinhaDigitavel;
import verificacao.ConsultaCNPJ;
import verificacao.ConsultaBanco;
import usuario.Boleto;
import java.math.BigDecimal;
import java.util.Scanner;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate; // Importar LocalDate
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; // Importar DateTimeFormatter
import java.time.format.DateTimeParseException; // Importar DateTimeParseException
import bancodedados.RepositorioLinhaDigitavel; // Novo repositório
import bancodedados.RepositorioUsuario; // Assumindo que a classe RepositorioUsuario existe
import usuario.Usuario; 

import bancodedados.RepositorioCnpjReputacao; 
import bancodedados.ConexaoBD; // Importar ConexaoBD
import java.sql.Connection; // Importar Connection para o método inserirOuAtualizarCnpjEmitente
import java.sql.Date; // Importar Date
import java.sql.PreparedStatement; // Importar PreparedStatement
import java.sql.ResultSet; // Importar ResultSet



public class ProcessadorLinhaDigitavel {

    private Scanner scanner;
    private RepositorioLinhaDigitavel repositorioLinhaDigitavel; // Novo repositório
    private RepositorioUsuario repositorioUsuario; // Para criar/obter usuário anônimo
    private RepositorioCnpjReputacao repositorioCnpjReputacao; // NOVO: Instância do repositório de reputação


    public ProcessadorLinhaDigitavel(Scanner scanner) {
        this.scanner = scanner;
        this.repositorioLinhaDigitavel = new RepositorioLinhaDigitavel();
        this.repositorioUsuario = new RepositorioUsuario();
        this.repositorioCnpjReputacao = new RepositorioCnpjReputacao(); // NOVO: Inicialização
    }

    public void processar() throws SQLException { // Adicionado throws SQLException
        System.out.println("\n--- Processamento de Boleto por Linha Digitavel ---");
        System.out.println("Por favor, digite a linha digitavel (codigo de barras, com ou sem pontos/espacos):");
        
        String linhaDigitalInput = scanner.nextLine();
        String linhaDigital = linhaDigitalInput.trim().replaceAll("[^0-9]", "");

        if (linhaDigital.length() != 47) {
            System.err.println("Erro: A linha digitavel deve conter 47 digitos numericos. Voce digitou " + linhaDigitalInput.length() + " caracteres (incluindo pontos/espacos) que resultaram em " + linhaDigital.length() + " digitos numericos.");
            System.err.println("Por favor, verifique a entrada. Ex: 00190000090362072700200439729179799850000222900");
            return;
        }

        System.out.println("Linha digital capturada e limpa: " + linhaDigital);

        Boleto boleto = new Boleto();
        boleto.setCodigoBarras(linhaDigital);
        boleto.setDataExtracao(LocalDateTime.now()); // Define a data de extração para agora

        // Validação da estrutura da linha digital
        System.out.println("\n--- Realizando validacao detalhada da Linha Digitavel ---");
        boolean linhaDigitalEstruturaValida = ValidadorLinhaDigitavel.validar(linhaDigital);
        
        if (!linhaDigitalEstruturaValida) {
            System.out.println("Validacao de estrutura da Linha Digitavel FALHOU.");
            boleto.setStatusValidacao("ERRO_ESTRUTURA_LD"); 
            System.out.println("No entanto, continuaremos com outras verificacoes.");
        } else {
            System.out.println("Validacao de estrutura da Linha Digitavel OK.");
            boleto.setStatusValidacao("VALIDO_ESTRUTURA_LD");
        }

        // Solicita a data de vencimento
        LocalDate vencimento = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        while (vencimento == null) {
            System.out.println("\n Por favor, digite a data de vencimento do boleto (DD/MM/AAAA):");
            String dataInput = scanner.nextLine().trim();
            try {
                vencimento = LocalDate.parse(dataInput, formatter);
                boleto.setVencimento(vencimento);
            } catch (DateTimeParseException e) {
                System.err.println("Formato de data invalido. Use o formato DD/MM/AAAA (ex: 25/12/2023).");
            }
        }

        System.out.println("\n Este boleto possui algum desconto? (sim/nao)");
        String temDescontoStr = scanner.nextLine().trim().toLowerCase();
        boolean temDesconto = "sim".equals(temDescontoStr);

        BigDecimal valorInformadoPeloUsuario = null;
        if (temDesconto) {
            System.out.println("Por favor, digite o valor *original* do boleto (sem descontos, formato 00.00):");
            while (valorInformadoPeloUsuario == null) {
                try {
                    String valorInput = scanner.nextLine().replace(",", ".");
                    valorInformadoPeloUsuario = new BigDecimal(valorInput);
                } catch (NumberFormatException e) {
                    System.err.println("Formato de valor invalido. Digite novamente o valor original (ex: 123.45):");
                }
            }
        } else {
            System.out.println("Por favor, digite o valor do pagamento do boleto (formato 00.00):");
            while (valorInformadoPeloUsuario == null) {
                try {
                    String valorInput = scanner.nextLine().replace(",", ".");
                    valorInformadoPeloUsuario = new BigDecimal(valorInput);
                } catch (NumberFormatException e) {
                    System.err.println("Formato de valor invalido. Digite novamente o valor (ex: 123.45):");
                }
            }
        }
        boleto.setValor(valorInformadoPeloUsuario); // Usa o setter de BigDecimal

        BigDecimal valorDoCodigoBarras = null;
        // Tenta extrair o valor do código de barras da linha digital
        try {
             String valorStr = linhaDigital.substring(linhaDigital.length() - 10);
             String valorFormatado = valorStr.substring(0, 8) + "." + valorStr.substring(8, 10);
             valorDoCodigoBarras = new BigDecimal(valorFormatado);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            System.err.println("Erro ao extrair valor do codigo de barras da linha digitavel: " + e.getMessage());
            // Se não conseguir extrair, mantém como null ou 0 para a comparação
            valorDoCodigoBarras = BigDecimal.ZERO; 
        }


        System.out.println("\n--- Verificacao do Valor Final ---");
        System.out.println("Valor informado pelo usuario (sem desconto): " + valorInformadoPeloUsuario);
        System.out.println("Valor extraido da linha digitavel: " + valorDoCodigoBarras);

        if (valorInformadoPeloUsuario.compareTo(valorDoCodigoBarras) == 0) {
            System.out.println("O valor informado corresponde ao valor na linha digitavel. Verificacao de valor OK.");
        } else {
            System.out.println("ATENCAO: O valor informado NAO corresponde ao valor na linha digitavel. Isso pode indicar um problema.");
        }

        System.out.println("\n--- Verificacao do CNPJ ---");
        System.out.println("Por favor, informe o CNPJ do beneficiario (somente numeros):");
        String cnpjInformado = scanner.nextLine().trim().replaceAll("[^0-9]", "");
        boleto.setCnpjEmitente(cnpjInformado);

        int verificacoesComFalha = 0; // Contador de falhas para o status geral

        if (cnpjInformado.length() == 14) {
            System.out.println("Consultando CNPJ...");
            ConsultaCNPJ consultaCnpj = new ConsultaCNPJ(boleto); // Passa o objeto boleto para ser preenchido
            String statusConsultaCnpj = consultaCnpj.validarDadosComApi();

            System.out.println("Status da validacao do CNPJ: " + statusConsultaCnpj);

            // Chamar o método para inserir/atualizar CNPJ na tabela CNPJ_Emitente
            inserirOuAtualizarCnpjEmitente(cnpjInformado, boleto.getRazaoSocialApi());


            System.out.println("\n--- Dados do CNPJ " + cnpjInformado + " Retornados pela BrasilAPI ---");
            System.out.println("Razão Social: " + (boleto.getRazaoSocialApi() != null ? boleto.getRazaoSocialApi() : "Nao disponivel"));
            System.out.println("Nome Fantasia: " + (boleto.getNomeFantasiaApi() != null ? boleto.getNomeFantasiaApi() : "Nao disponivel"));
            System.out.println("--------------------------------------------------");

            // Lógica de comparação de nomes (similar ao ProcessadorBoleto)
            // Para boletos via linha digitável, o nome do beneficiário (nomePdf)
            // é o que o usuário *espera* ou o nome extraído de alguma outra fonte
            // que não seja o PDF. Se não for preenchido, a comparação será informativa.
            String nomePdf = boleto.getNomeBeneficiario(); // Manterá o valor original ou será nulo/vazio
            String razaoApi = boleto.getRazaoSocialApi();

            if (nomePdf != null && !nomePdf.isEmpty() && razaoApi != null && !razaoApi.isEmpty()) {
                String nomePdfLimpo = nomePdf.toLowerCase().replaceAll("\\s+", "");
                String razaoApiLimpa = razaoApi.toLowerCase().replaceAll("\\s+", "");

                if (!nomePdfLimpo.equals(razaoApiLimpa) && !nomePdfLimpo.contains(razaoApiLimpa)
                        && !razaoApiLimpa.contains(nomePdfLimpo)) {
                    System.out.println("**ALERTA DE FRAUDE POTENCIAL:** Nome do beneficiario informado ('"
                            + boleto.getNomeBeneficiario() +
                            "') DIVERGE da Razao Social da API ('" + boleto.getRazaoSocialApi()
                            + "') para este CNPJ.");
                    boleto.setStatusValidacao("ALERTA_FRAUDE_NOME_CNPJ_DIVERGENTE");
                    verificacoesComFalha++;
                } else {
                    System.out.println("O nome do beneficiario informado ('" + boleto.getNomeBeneficiario() +
                            "') BATE com a Razao Social da API ('" + boleto.getRazaoSocialApi() + "').");
                }
            } else {
                System.out.println(
                        "Nao foi possivel comparar o nome do beneficiario com a Razao Social da API (dados ausentes ou não disponiveis).");
            }


            System.out.println("\n As informacoes do CNPJ acima estao corretas? (sim/nao)");
            String confirmacaoDadosCnpj = scanner.nextLine().trim().toLowerCase();

            if ("sim".equals(confirmacaoDadosCnpj)) {
                System.out.println("Confirmacao dos dados do CNPJ registrada.");
                boleto.setInformacoesConfirmadasPeloUsuario(true);
                if (statusConsultaCnpj.equals("VALIDO_API")) {
                    // Mantém status de fraude se já foi detectado, senão atualiza
                    if (!"ALERTA_FRAUDE_NOME_CNPJ_DIVERGENTE".equals(boleto.getStatusValidacao())) {
                       boleto.setStatusValidacao("VALIDO_CNPJ_API_E_USUARIO");
                    }
                } else {
                     if (!"ALERTA_FRAUDE_NOME_CNPJ_DIVERGENTE".equals(boleto.getStatusValidacao())) {
                        boleto.setStatusValidacao("CNPJ_CONFIRMADO_USUARIO_COM_ALERTA");
                    }
                }
            } else {
                System.out.println("Voce indicou que os dados do CNPJ nao estao corretos. Marcarei como 'CNPJ_NAO_CONFIRMADO_USUARIO'.");
                boleto.setInformacoesConfirmadasPeloUsuario(false);
                boleto.setStatusValidacao("CNPJ_NAO_CONFIRMADO_USUARIO");
                verificacoesComFalha++;
            }
        } else {
            System.out.println("CNPJ invalido. A consulta nao sera realizada. Marcarei como 'CNPJ_INVALIDO_FORMATO'.");
            boleto.setStatusValidacao("CNPJ_INVALIDO_FORMATO");
            boleto.setInformacoesConfirmadasPeloUsuario(false);
            verificacoesComFalha++;
        }

        // --- INÍCIO DA LÓGICA PARA CONFIRMAÇÃO DETALHADA DO BANCO ---
        System.out.println("\n--- Verificacao do Banco Emissor ---");
        String codigoBanco = linhaDigital.substring(0, 3);
        boleto.setBancoEmissor(codigoBanco); // Define o código do banco extraído da linha digitável

        System.out.println("Codigo do banco extraido da linha digitavel: " + codigoBanco);

        ConsultaBanco consultaBanco = new ConsultaBanco(boleto);
        String statusConsultaBanco = consultaBanco.validarBancoComApi(); // Isso preenche os campos do boleto
        boleto.setStatusValidacaoBanco(statusConsultaBanco); // Define o status de validação do banco no boleto

        System.out.println("Status da validacao do banco: " + statusConsultaBanco);

        // Exibir os dados puxados da API
        System.out.println("\n--- Dados do Banco " + codigoBanco);
        System.out.println("Codigo do Banco (API): " + (boleto.getBancoEmissor() != null ? boleto.getBancoEmissor() : "Nao disponivel"));
        System.out.println("Nome do Banco (API): " + (boleto.getNomeBancoApi() != null ? boleto.getNomeBancoApi() : "Nao disponivel"));
        System.out.println("Nome Completo do Banco (API): " + (boleto.getNomeCompletoBancoApi() != null ? boleto.getNomeCompletoBancoApi() : "Nao disponivel"));
        System.out.println("ISPB (API): " + (boleto.getIspbBancoApi() != null ? boleto.getIspbBancoApi() : "Nao disponivel"));
        System.out.println("--------------------------------------------------");

        // Pergunta de confirmação ao usuário
        System.out.println("\n As informacoes acima estao corretas? (sim/nao)");
        String confirmacaoDadosBanco = scanner.nextLine().trim().toLowerCase();

        if ("sim".equals(confirmacaoDadosBanco)) {
            System.out.println("Confirmacao dos dados do banco registrada.");
            if (statusConsultaBanco.equals("VALIDO_API")) {
                boleto.setStatusValidacaoBanco("VALIDO_BANCO_API_E_USUARIO");
            } else {
                boleto.setStatusValidacaoBanco("BANCO_CONFIRMADO_USUARIO_COM_ALERTA"); 
            }
        } else {
            System.out.println("Voce indicou que os dados do banco nao estao corretos. Marcarei como 'BANCO_NAO_CONFIRMADO_USUARIO'.");
            boleto.setStatusValidacaoBanco("BANCO_NAO_CONFIRMADO_USUARIO");
            verificacoesComFalha++;
        }
        // --- FIM DA LÓGICA PARA CONFIRMAÇÃO DETALHADA DO BANCO ---

        // Atualiza o status geral do boleto com base nas falhas de verificação
        if (verificacoesComFalha > 0 && !"ALERTA_FRAUDE_NOME_CNPJ_DIVERGENTE".equals(boleto.getStatusValidacao())) {
            boleto.setStatusValidacao("ALERTA_GERAL_NAO_CONFORMIDADE");
            boleto.setSuspeito(true); // ATUALIZADO: setSuspeito(true)
        } else if (verificacoesComFalha == 0 && !"ALERTA_FRAUDE_NOME_CNPJ_DIVERGENTE".equals(boleto.getStatusValidacao()) && !"ERRO_ESTRUTURA_LD".equals(boleto.getStatusValidacao())) {
            boleto.setStatusValidacao("VALIDO_COMPLETO");
        }

        // Determinar se o boleto é "falho" para a reputação do CNPJ
        boolean isBoletoFalhoParaReputacao = false;
        if ("INVALIDO".equals(boleto.getStatusValidacao()) ||
            "ALERTA_FRAUDE_NOME_CNPJ_DIVERGENTE".equals(boleto.getStatusValidacao()) ||
            "CNPJ_INVALIDO_FORMATO".equals(boleto.getStatusValidacao()) ||
            "CNPJ_NAO_CONFIRMADO_USUARIO".equals(boleto.getStatusValidacao()) ||
            "BANCO_NAO_CONFIRMADO_USUARIO".equals(boleto.getStatusValidacaoBanco()) ||
            "ALERTA_GERAL_NAO_CONFORMIDADE".equals(boleto.getStatusValidacao())) {
            isBoletoFalhoParaReputacao = true;
        }

        // NOVO: Atualizar reputação do CNPJ
        try {
            // Passa o CNPJ emitente, se o boleto é "falho" e o total de atualizações deste boleto
            repositorioCnpjReputacao.atualizarReputacaoCnpj(boleto.getCnpjEmitente(), isBoletoFalhoParaReputacao, boleto.getTotalAtualizacoes());

            // Busca a reputação atualizada para exibir ao usuário e salvar no boleto
            Object[] reputacaoAtual = repositorioCnpjReputacao
                    .buscarReputacaoCnpj(boleto.getCnpjEmitente());
            if (reputacaoAtual != null) {
                BigDecimal score = (BigDecimal) reputacaoAtual[0];
                int totalBoletosCnpj = (int) reputacaoAtual[1];
                int totalDenunciasCnpj = (int) reputacaoAtual[2];

                // Define os valores de reputação no objeto Boleto
                boleto.setScoreReputacaoCnpj(score);
                boleto.setTotalBoletosCnpj(totalBoletosCnpj);
                boleto.setTotalDenunciasCnpj(totalDenunciasCnpj);

                String classificacao;
                if (score.compareTo(new BigDecimal("80.00")) > 0) {
                    classificacao = "Confiável";
                } else if (score.compareTo(new BigDecimal("50.00")) >= 0
                        && score.compareTo(new BigDecimal("80.00")) <= 0) {
                    classificacao = "Risco Moderado";
                } else {
                    if (score.compareTo(BigDecimal.ZERO) == 0) {
                        classificacao = "Reincidente";
                    } else {
                        classificacao = "Problemático";
                    }
                    System.out.println("\n Este CNPJ possui muitas denuncias anteriores. Risco elevado.");
                }

                System.out.println("Total de Boletos (CNPJ): " + totalBoletosCnpj);
                System.out.println("Total de Denuncias (CNPJ): " + totalDenunciasCnpj);
                System.out.printf("Score de Reputacao (CNPJ): %.2f%%\n", score);
                System.out.println("Classificacao (CNPJ): " + classificacao);
                System.out.println("Calculo de reputacao concluido para o CNPJ: " + boleto.getCnpjEmitente());

                if ((classificacao.equals("Reincidente") || classificacao.equals("Problemático"))
                        && totalDenunciasCnpj >= 10) {
                    boleto.setSuspeito(true); // ATUALIZADO: setSuspeito(true)
                    System.out.println("Boleto de CNPJ classificado como '" + classificacao + "' e com "
                            + totalDenunciasCnpj + " denuncias. Marcado como SUSPEITO automaticamente!"); // ATUALIZADO: SUSPEITO
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao processar reputacao do CNPJ: " + e.getMessage());
            // Não relança a exceção aqui para não impedir o salvamento do boleto
        }
        // FIM NOVO: Atualizar reputação do CNPJ

        // Associa o boleto a um usuário anônimo antes de salvar
        Usuario usuarioAnonimo = repositorioUsuario.criarUsuarioAnonimo();
        if (usuarioAnonimo == null || usuarioAnonimo.getId() == 0) {
            System.err.println("Falha critica: Nao foi possivel criar um usuario anonimo. O boleto nao sera salvo.");
            return;
        }
        System.out.println("Novo usuario anonimo criado com ID: " + usuarioAnonimo.getId());
        boleto.setUsuarioId(usuarioAnonimo.getId());

        System.out.println("\n--- Resumo e Preparacao para Salvamento ---");
        System.out.println("Linha Digital: " + boleto.getCodigoBarras());
        System.out.println("Valor Informado pelo Usuario: " + boleto.getValorAsBigDecimal()); // Usando o getter para BigDecimal
        System.out.println("Valor Extraido da Linha Digital: " + valorDoCodigoBarras);
        System.out.println("CNPJ Beneficiario Informado: " + boleto.getCnpjEmitente());
        System.out.println("Razao Social (API): " + (boleto.getRazaoSocialApi() != null ? boleto.getRazaoSocialApi() : "Nao disponivel"));
        System.out.println("Nome Fantasia (API): " + (boleto.getNomeFantasiaApi() != null ? boleto.getNomeFantasiaApi() : "Nao disponivel"));
        System.out.println("Codigo Banco (Linha Digital): " + boleto.getBancoEmissor());
        System.out.println("Nome Banco (API): " + (boleto.getNomeBancoApi() != null ? boleto.getNomeBancoApi() : "Nao disponivel"));
        System.out.println("Nome Completo Banco (API): " + (boleto.getNomeCompletoBancoApi() != null ? boleto.getNomeCompletoBancoApi() : "Nao disponivel"));
        System.out.println("ISPB (API): " + (boleto.getIspbBancoApi() != null ? boleto.getIspbBancoApi() : "Nao disponivel"));
        System.out.println("Informacoes CNPJ e Banco Confirmadas pelo Usuario: " + (boleto.isInformacoesConfirmadasPeloUsuario() ? "Sim" : "Nao"));
        System.out.println("Status de Validacao Geral: " + boleto.getStatusValidacao());
        System.out.println("Status de Validacao de Banco: " + boleto.getStatusValidacaoBanco());
        System.out.println("Suspeito Automaticamente: " + (boleto.isSuspeito() ? "Sim" : "Nao")); // ATUALIZADO: Suspeito
        System.out.println("Total de Atualizacoes deste Boleto: " + boleto.getTotalAtualizacoes()); // NOVO: Exibe total de atualizações

        System.out.println("\n Tentando salvar boleto no banco de dados...");
        try {
            if (repositorioLinhaDigitavel.inserirBoletoPorLinhaDigitavel(boleto)) {
                System.out.println("Boleto salvo no banco de dados com sucesso!");
            } else {
                System.err.println("Falha desconhecida ao salvar o boleto.");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao salvar boleto no banco de dados: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-lança a exceção para tratamento superior
        }

        System.out.println("\n Processamento da linha digital concluido.");
    }

    /**
     * Insere ou atualiza um CNPJ na tabela CNPJ_Emitente.
     * Este método foi duplicado do ProcessadorBoleto para garantir a consistência
     * da base de dados antes de inserir um Boleto que faz referência a este CNPJ.
     * @param cnpj O CNPJ a ser inserido/verificado.
     * @param nomeRazaoSocial O nome/razão social associado ao CNPJ.
     * @throws SQLException Se ocorrer um erro no acesso ao banco de dados.
     */
    private void inserirOuAtualizarCnpjEmitente(String cnpj, String nomeRazaoSocial) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM CNPJ_Emitente WHERE cnpj = ?";
        String insertSql = "INSERT INTO CNPJ_Emitente (cnpj, nome_razao_social, data_abertura) VALUES (?, ?, ?)";

        try (Connection conexao = ConexaoBD.getConexao()) {
            try (PreparedStatement checkStmt = conexao.prepareStatement(checkSql)) {
                checkStmt.setString(1, cnpj);
                ResultSet rs = checkStmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("CNPJ Emitente '" + cnpj + "' ja existe na tabela CNPJ_Emitente.");
                    return;
                }
            }

            try (PreparedStatement insertStmt = conexao.prepareStatement(insertSql)) {
                insertStmt.setString(1, cnpj);
                insertStmt.setString(2, nomeRazaoSocial != null && !nomeRazaoSocial.isEmpty() ? nomeRazaoSocial : "Desconhecido");
                insertStmt.setDate(3, Date.valueOf(LocalDate.now())); // Usa a data atual como data de abertura

                int linhasAfetadas = insertStmt.executeUpdate();
                if (linhasAfetadas > 0) {
                    System.out.println("CNPJ Emitente '" + cnpj + "' inserido na tabela CNPJ_Emitente.");
                } else {
                    System.err.println("Falha ao inserir CNPJ Emitente '" + cnpj + "' na tabela CNPJ_Emitente.");
                }
            }
        }
    }
}
