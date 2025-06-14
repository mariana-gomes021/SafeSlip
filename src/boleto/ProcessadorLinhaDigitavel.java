package boleto;

import verificacao.ValidadorEmitente;
import usuario.Boleto;
import java.math.BigDecimal;
import java.util.Scanner;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import bancodedados.RepositorioLinhaDigitavel;
import bancodedados.RepositorioUsuario;
import usuario.Usuario;

import bancodedados.RepositorioCnpjReputacao;
import bancodedados.RepositorioCnpjEmitente;

public class ProcessadorLinhaDigitavel {

    private Scanner scanner;
    private RepositorioLinhaDigitavel repositorioLinhaDigitavel;
    private RepositorioUsuario repositorioUsuario;
    private RepositorioCnpjReputacao repositorioCnpjReputacao;
    private RepositorioCnpjEmitente repositorioCnpjEmitente;

    public ProcessadorLinhaDigitavel(Scanner scanner) {
        this.scanner = scanner;
        this.repositorioLinhaDigitavel = new RepositorioLinhaDigitavel();
        this.repositorioUsuario = new RepositorioUsuario();
        this.repositorioCnpjReputacao = new RepositorioCnpjReputacao();
        this.repositorioCnpjEmitente = new RepositorioCnpjEmitente();
    }

    public void processar() throws SQLException {
        System.out.println("\n--- Processamento de Boleto por Linha Digitavel ---");
        System.out.println("Por favor, digite a linha digitavel (codigo de barras, com ou sem pontos/espacos):");

        String linhaDigitalInput = scanner.nextLine();
        String linhaDigital = linhaDigitalInput.trim().replaceAll("[^0-9]", "");
        scanner.nextLine();

        Boleto boleto = new Boleto();
        boleto.setCodigoBarras(linhaDigital);
        boleto.setDataExtracao(LocalDateTime.now());

        // Variáveis para rastrear o sucesso de cada etapa de validação
        boolean linhaDigitalEstruturaEVsValida = false;
        boolean valorBate = false;
        String statusCnpj = "NAO_VALIDADO";
        String statusBanco = "NAO_VALIDADO";
        BigDecimal valorDoCodigoBarras = BigDecimal.ZERO; // Inicializa para uso em exibição

        // --- Verificação 1: Validação da estrutura e dígitos verificadores da linha digitavel ---
        System.out.println("\n--- Realizando validacao detalhada da Linha Digitavel ---");
        linhaDigitalEstruturaEVsValida = ValidadorLinhaDigitavel.validar(linhaDigital);

        if (!linhaDigitalEstruturaEVsValida) {
            System.out.println("Validacao de estrutura e DVs da Linha Digitavel FALHOU.");
            boleto.setStatusValidacao("ERRO_ESTRUTURA_OU_DV_LD");
            // Não interrompe mais aqui, apenas registra o status
        } else {
            System.out.println("Validacao de estrutura e DVs da Linha Digitavel OK.");
            boleto.setStatusValidacao("VALIDO_ESTRUTURA_LD"); // Status inicial de sucesso
        }

        // Solicita a data de vencimento
        LocalDate vencimento = null;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        while (vencimento == null) {
            System.out.println("\nPor favor, digite a data de vencimento do boleto (DD/MM/AAAA):");
            String dataInput = scanner.nextLine().trim();
            try {
                vencimento = LocalDate.parse(dataInput, formatter);
                boleto.setVencimento(vencimento);
            } catch (DateTimeParseException e) {
                System.err.println("Formato de data invalido. Use o formato DD/MM/AAAA (ex: 25/12/2023).");
            }
        }

        // --- Verificação 2: Valor do Boleto bate com o valor indicado na linha digitavel? ---
        System.out.println("\nEste boleto possui algum desconto? (sim/nao)");
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
        boleto.setValor(valorInformadoPeloUsuario);

        // Extrai o valor do código de barras diretamente aqui no Processador para comparação e exibição
        try {
            String valorStr = linhaDigital.substring(linhaDigital.length() - 10);
            String valorFormatado = valorStr.substring(0, 8) + "." + valorStr.substring(8, 10);
            valorDoCodigoBarras = new BigDecimal(valorFormatado);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            System.err.println("Erro ao extrair valor do codigo de barras da linha digitavel para comparacao: " + e.getMessage());
            // Se não conseguir extrair, valorDoCodigoBarras permanece 0, o que fará a validação de valor falhar
        }
        // Não estamos usando boleto.setValorExtraidoLinhaDigital(valorDoCodigoBarras); aqui,
        // mas valorDoCodigoBarras será usado no resumo final.

        valorBate = ValidadorLinhaDigitavel.validarValor(valorInformadoPeloUsuario, linhaDigital);
        if (!valorBate) {
            // Se já não era válido estruturalmente, mantém o status anterior mais grave.
            // Se era válido na estrutura, mas o valor divergiu, atualiza.
            if ("VALIDO_ESTRUTURA_LD".equals(boleto.getStatusValidacao())) {
                boleto.setStatusValidacao("VALOR_DIVERGENTE");
            }
        }

        // --- Verificação 3 & 4: Dados do CNPJ e Bancários conferem? ---
        ValidadorEmitente validadorEmitente = new ValidadorEmitente(boleto, scanner);

        statusCnpj = validadorEmitente.validarCnpjBeneficiario();
        boleto.setStatusValidacaoCnpj(statusCnpj);

        statusBanco = validadorEmitente.validarDadosBancarios();
        boleto.setStatusValidacaoBanco(statusBanco);

        // --- Lógica para determinar o status final do boleto ---
        // Começa com o status já definido e o deteriora se novas falhas forem encontradas
        String statusFinal = boleto.getStatusValidacao();

        // Se o valor não bateu OU CNPJ/Banco não são válidos OU houve alerta de fraude
        if (!valorBate ||
            !"VALIDO_CNPJ_API_E_USUARIO".equals(statusCnpj) ||
            !"VALIDO_BANCO_API_E_USUARIO".equals(statusBanco) ||
            "ALERTA_FRAUDE_NOME_CNPJ_DIVERGENTE".equals(statusCnpj))
        {
            // Se o status atual é de sucesso inicial (VALIDO_ESTRUTURA_LD),
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
            // e não há alerta de fraude, então o boleto é considerado 'VALIDO_COMPLETO' antes da reputação.
            if ("VALIDO_ESTRUTURA_LD".equals(statusFinal)) { // Garante que a estrutura inicial era válida
                 statusFinal = "VALIDO_COMPLETO";
            }
        }
        boleto.setStatusValidacao(statusFinal); // Define o status final geral do boleto

        // --- Verificação 5: Reputação do boleto por dados corretos ---
        // A reputação é calculada com base no status final do boleto até agora.
        boolean isBoletoFalhoParaReputacao = !"VALIDO_COMPLETO".equals(boleto.getStatusValidacao());

        try {
            // Garante que o CNPJ Emitente foi extraído para a reputação
            if (boleto.getCnpjEmitente() != null && !boleto.getCnpjEmitente().isEmpty()) {
                repositorioCnpjReputacao.atualizarReputacaoCnpj(boleto.getCnpjEmitente(), isBoletoFalhoParaReputacao, boleto.getTotalAtualizacoes());

                Object[] reputacaoAtual = repositorioCnpjReputacao.buscarReputacaoCnpj(boleto.getCnpjEmitente());
                if (reputacaoAtual != null) {
                    BigDecimal score = (BigDecimal) reputacaoAtual[0];
                    int totalBoletosCnpj = (int) reputacaoAtual[1];
                    int totalDenunciasCnpj = (int) reputacaoAtual[2];

                    boleto.setScoreReputacaoCnpj(score);
                    boleto.setTotalBoletosCnpj(totalBoletosCnpj);
                    boleto.setTotalDenunciasCnpj(totalDenunciasCnpj);

                    String classificacao;
                    if (score.compareTo(new BigDecimal("80.00")) > 0) {
                        classificacao = "Confiável";
                    } else if (score.compareTo(new BigDecimal("50.00")) >= 0 && score.compareTo(new BigDecimal("80.00")) <= 0) {
                        classificacao = "Risco Moderado";
                    } else {
                        classificacao = (score.compareTo(BigDecimal.ZERO) == 0) ? "Reincidente" : "Problemático";
                        System.out.println("\nEste CNPJ possui muitas denuncias anteriores. Risco elevado.");
                    }

                    System.out.println("Total de Boletos (CNPJ): " + totalBoletosCnpj);
                    System.out.println("Total de Denuncias (CNPJ): " + totalDenunciasCnpj);
                    System.out.printf("Score de Reputacao (CNPJ): %.2f%%\n", score);
                    System.out.println("Classificacao (CNPJ): " + classificacao);
                    System.out.println("Calculo de reputacao concluido para o CNPJ: " + boleto.getCnpjEmitente());

                    if ((classificacao.equals("Reincidente") || classificacao.equals("Problemático")) && totalDenunciasCnpj >= 10) {
                        boleto.setSuspeito(true);
                        System.out.println("Boleto de CNPJ classificado como '" + classificacao + "' e com " + totalDenunciasCnpj + " denuncias. Marcado como SUSPEITO automaticamente!");
                        // Se a reputação for muito baixa, pode sobrepor um status de "VÁLIDO_COMPLETO" para "ALERTA_GERAL_NAO_CONFORMIDADE"
                        if ("VALIDO_COMPLETO".equals(boleto.getStatusValidacao())) {
                            boleto.setStatusValidacao("ALERTA_GERAL_NAO_CONFORMIDADE");
                        }
                    }
                } else {
                     System.out.println(" Nao foi possivel buscar a reputacao do CNPJ. Pode ser um novo CNPJ.");
                }
            } else {
                System.out.println(" Nao foi possivel calcular reputacao: CNPJ Emitente nao extraido ou vazio.");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao processar reputacao do CNPJ: " + e.getMessage());
        }

        // Associa o boleto a um usuário anônimo antes de salvar
        Usuario usuarioAnonimo = repositorioUsuario.criarUsuarioAnonimo();
        if (usuarioAnonimo == null || usuarioAnonimo.getId() == 0) {
            System.err.println("Falha critica: Nao foi possivel criar um usuario anonimo. O boleto nao sera salvo.");
            return; // Interrompe se não puder criar o usuário anônimo
        }
        System.out.println("Novo usuario anonimo criado com ID: " + usuarioAnonimo.getId());
        boleto.setUsuarioId(usuarioAnonimo.getId());

        // Resumo e Preparação para Salvamento
        System.out.println("\n--- Resumo e Preparacao para Salvamento ---");
        System.out.println("Linha Digital: " + boleto.getCodigoBarras());
        System.out.println("Valor Informado pelo Usuario: " + boleto.getValorAsBigDecimal());
        System.out.println("Valor Extraido da Linha Digital: " + valorDoCodigoBarras); // Usa a variável local
        System.out.println("CNPJ Beneficiario Informado: " + boleto.getCnpjEmitente());
        System.out.println("Razao Social (API): " + (boleto.getRazaoSocialApi() != null ? boleto.getRazaoSocialApi() : "Nao disponivel"));
        System.out.println("Nome Fantasia (API): " + (boleto.getNomeFantasiaApi() != null ? boleto.getNomeFantasiaApi() : "Nao disponivel"));
        System.out.println("Codigo Banco (Linha Digital): " + boleto.getBancoEmissor());
        System.out.println("Nome Banco (API): " + (boleto.getNomeBancoApi() != null ? boleto.getNomeBancoApi() : "Nao disponivel"));
        System.out.println("Nome Completo Banco (API): " + (boleto.getNomeCompletoBancoApi() != null ? boleto.getNomeCompletoBancoApi() : "Nao disponivel"));
        System.out.println("ISPB (API): " + (boleto.getIspbBancoApi() != null ? boleto.getIspbBancoApi() : "Nao disponivel"));
        // Imprime o Código do Banco da API se disponível (requer getCodigoBancoApi() em Boleto.java)
        System.out.println("Codigo Banco (API): " + (boleto.getCodigoBancoApi() != null ? boleto.getCodigoBancoApi() : "Nao disponivel")); 
        System.out.println("Informacoes CNPJ e Banco Confirmadas pelo Usuario: " + (boleto.isInformacoesConfirmadasPeloUsuario() ? "Sim" : "Nao"));
        System.out.println("Status de Validacao Geral: " + boleto.getStatusValidacao());
        System.out.println("Status de Validacao de CNPJ: " + boleto.getStatusValidacaoCnpj());
        System.out.println("Status de Validacao de Banco: " + boleto.getStatusValidacaoBanco());
        System.out.println("Suspeito Automaticamente: " + (boleto.isSuspeito() ? "Sim" : "Nao"));
        System.out.println("Total de Atualizacoes deste Boleto: " + boleto.getTotalAtualizacoes());
        System.out.printf("Score de Reputacao (CNPJ): %.2f%%\n", boleto.getScoreReputacaoCnpj());
        System.out.println("Total de Boletos (CNPJ): " + boleto.getTotalBoletosCnpj());
        System.out.println("Total de Denuncias (CNPJ): " + boleto.getTotalDenunciasCnpj());


        System.out.println("\nTentando salvar boleto no banco de dados...");
        try {
            if (repositorioLinhaDigitavel.inserirBoletoPorLinhaDigitavel(boleto)) {
                System.out.println(" Boleto salvo no banco de dados com sucesso!");
            } else {
                System.err.println("Falha desconhecida ao salvar o boleto.");
            }
        } catch (SQLException e) {
            System.err.println("Erro ao salvar boleto no banco de dados: " + e.getMessage());
            e.printStackTrace();
            throw e; // Lança a exceção para que o chamador possa tratá-la
        }

        System.out.println("\nProcessamento da linha digital concluido.");
    }
}