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

import bancodedados.ConexaoBD; // Importar ConexaoBD
import java.sql.Connection; // Importar Connection para o método inserirOuAtualizarCnpjEmitente
import java.sql.Date; // Importar Date
import java.sql.PreparedStatement; // Importar PreparedStatement
import java.sql.ResultSet; // Importar ResultSet



public class ProcessadorLinhaDigitavel {

    private Scanner scanner;
    private RepositorioLinhaDigitavel repositorioLinhaDigitavel; // Novo repositório
    private RepositorioUsuario repositorioUsuario; // Para criar/obter usuário anônimo

    public ProcessadorLinhaDigitavel(Scanner scanner) {
        this.scanner = scanner;
        this.repositorioLinhaDigitavel = new RepositorioLinhaDigitavel();
        this.repositorioUsuario = new RepositorioUsuario();
    }

   public void processar() throws SQLException {
        System.out.println("\n--- Processamento de Boleto por Linha Digital ---");
        System.out.println("Por favor, digite a linha digital (código de barras, com ou sem pontos/espaços):");
        
        String linhaDigitalInput = scanner.nextLine();
        String linhaDigital = linhaDigitalInput.trim().replaceAll("[^0-9]", "");

        if (linhaDigital.length() != 47) {
            System.err.println("❌ Erro: A linha digital deve conter 47 dígitos numéricos. Você digitou " + linhaDigitalInput.length() + " caracteres (incluindo pontos/espaços) que resultaram em " + linhaDigital.length() + " dígitos numéricos.");
            System.err.println("Por favor, verifique a entrada. Ex: 00190000090362072700200439729179799850000222900");
            return;
        }

        System.out.println("✅ Linha digital capturada e limpa: " + linhaDigital);

        Boleto boleto = new Boleto();
        boleto.setCodigoBarras(linhaDigital);
        boleto.setDataExtracao(LocalDateTime.now()); // Define a data de extração para agora

        // Validação da estrutura da linha digital
        System.out.println("\n--- Realizando validação detalhada da Linha Digital ---");
        boolean linhaDigitalEstruturaValida = ValidadorLinhaDigitavel.validar(linhaDigital);
        
        if (!linhaDigitalEstruturaValida) {
            System.out.println("❌ Validação de estrutura da Linha Digital FALHOU.");
            boleto.setStatusValidacao("ERRO_ESTRUTURA_LD"); 
            System.out.println("⚠️ No entanto, continuaremos com outras verificações.");
        } else {
            System.out.println("✅ Validação de estrutura da Linha Digital OK.");
            boleto.setStatusValidacao("VALIDO_ESTRUTURA_LD");
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
                System.err.println("❌ Formato de data inválido. Use o formato DD/MM/AAAA (ex: 25/12/2023).");
            }
        }

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
                    System.err.println("❌ Formato de valor inválido. Digite novamente o valor original (ex: 123.45):");
                }
            }
        } else {
            System.out.println("Por favor, digite o valor do pagamento do boleto (formato 00.00):");
            while (valorInformadoPeloUsuario == null) {
                try {
                    String valorInput = scanner.nextLine().replace(",", ".");
                    valorInformadoPeloUsuario = new BigDecimal(valorInput);
                } catch (NumberFormatException e) {
                    System.err.println("❌ Formato de valor inválido. Digite novamente o valor (ex: 123.45):");
                }
            }
        }
        boleto.setValor(valorInformadoPeloUsuario);

        BigDecimal valorDoCodigoBarras = null;
        // Tenta extrair o valor do código de barras da linha digital
        try {
             String valorStr = linhaDigital.substring(linhaDigital.length() - 10);
             String valorFormatado = valorStr.substring(0, 8) + "." + valorStr.substring(8, 10);
             valorDoCodigoBarras = new BigDecimal(valorFormatado);
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            System.err.println("Erro ao extrair valor do código de barras da linha digital: " + e.getMessage());
            // Se não conseguir extrair, mantém como null ou 0 para a comparação
            valorDoCodigoBarras = BigDecimal.ZERO; 
        }


        System.out.println("\n--- Verificação do Valor Final ---");
        System.out.println("Valor informado pelo usuário (sem desconto): " + valorInformadoPeloUsuario);
        System.out.println("Valor extraído da linha digital: " + valorDoCodigoBarras);

        if (valorInformadoPeloUsuario.compareTo(valorDoCodigoBarras) == 0) {
            System.out.println("✅ O valor informado corresponde ao valor na linha digital. Verificação de valor OK.");
        } else {
            System.out.println("⚠️ ATENÇÃO: O valor informado NÃO corresponde ao valor na linha digital. Isso pode indicar um problema.");
        }

        System.out.println("\n--- Verificação do CNPJ ---");
        System.out.println("Por favor, informe o CNPJ do beneficiário (somente números):");
        String cnpjInformado = scanner.nextLine().trim().replaceAll("[^0-9]", "");
        boleto.setCnpjEmitente(cnpjInformado);

        int verificacoesComFalha = 0; // Contador de falhas para o status geral

        if (cnpjInformado.length() == 14) {
            System.out.println("🌐 Consultando CNPJ na BrasilAPI...");
            ConsultaCNPJ consultaCnpj = new ConsultaCNPJ(boleto); // Passa o objeto boleto para ser preenchido
            String statusConsultaCnpj = consultaCnpj.validarDadosComApi();

            System.out.println("ℹ️ Status da validação do CNPJ com a API: " + statusConsultaCnpj);

            // Chamar o método para inserir/atualizar CNPJ na tabela CNPJ_Emitente
            inserirOuAtualizarCnpjEmitente(cnpjInformado, boleto.getRazaoSocialApi());


            System.out.println("\n--- Dados do CNPJ " + cnpjInformado + " Retornados pela BrasilAPI ---");
            System.out.println("📝 Razão Social: " + (boleto.getRazaoSocialApi() != null ? boleto.getRazaoSocialApi() : "Não disponível"));
            System.out.println("✨ Nome Fantasia: " + (boleto.getNomeFantasiaApi() != null ? boleto.getNomeFantasiaApi() : "Não disponível"));
            System.out.println("--------------------------------------------------");

            // Lógica de comparação de nomes (similar ao ProcessadorBoleto)
            String nomePdf = boleto.getNomeBeneficiario(); // Manterá o valor original ou será nulo/vazio
            String razaoApi = boleto.getRazaoSocialApi();

            if (nomePdf != null && !nomePdf.isEmpty() && razaoApi != null && !razaoApi.isEmpty()) {
                String nomePdfLimpo = nomePdf.toLowerCase().replaceAll("\\s+", "");
                String razaoApiLimpa = razaoApi.toLowerCase().replaceAll("\\s+", "");

                if (!nomePdfLimpo.equals(razaoApiLimpa) && !nomePdfLimpo.contains(razaoApiLimpa)
                        && !razaoApiLimpa.contains(nomePdfLimpo)) {
                    System.out.println("🚨 **ALERTA DE FRAUDE POTENCIAL:** Nome do beneficiário informado ('"
                            + boleto.getNomeBeneficiario() +
                            "') DIVERGE da Razão Social da API ('" + boleto.getRazaoSocialApi()
                            + "') para este CNPJ.");
                    boleto.setStatusValidacao("ALERTA_FRAUDE_NOME_CNPJ_DIVERGENTE");
                    verificacoesComFalha++;
                } else {
                    System.out.println("✅ O nome do beneficiário informado ('" + boleto.getNomeBeneficiario() +
                            "') BATE com a Razão Social da API ('" + boleto.getRazaoSocialApi() + "').");
                }
            } else {
                System.out.println(
                        "ℹ️ Não foi possível comparar o nome do beneficiário com a Razão Social da API (dados ausentes ou não disponíveis).");
            }


            System.out.println("\nAs informações do CNPJ acima estão corretas? (sim/nao)");
            String confirmacaoDadosCnpj = scanner.nextLine().trim().toLowerCase();

            if ("sim".equals(confirmacaoDadosCnpj)) {
                System.out.println("👍 Confirmação dos dados do CNPJ registrada.");
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
                System.out.println("🚫 Você indicou que os dados do CNPJ não estão corretos. Marcarei como 'CNPJ_NAO_CONFIRMADO_USUARIO'.");
                boleto.setInformacoesConfirmadasPeloUsuario(false);
                boleto.setStatusValidacao("CNPJ_NAO_CONFIRMADO_USUARIO");
                verificacoesComFalha++;
            }
        } else {
            System.out.println("⚠️ CNPJ inválido. A consulta não será realizada. Marcarei como 'CNPJ_INVALIDO_FORMATO'.");
            boleto.setStatusValidacao("CNPJ_INVALIDO_FORMATO");
            boleto.setInformacoesConfirmadasPeloUsuario(false);
            verificacoesComFalha++;
        }

        // --- INÍCIO DA LÓGICA PARA CONFIRMAÇÃO DETALHADA DO BANCO ---
        System.out.println("\n--- Verificação do Banco Emissor ---");
        String codigoBanco = linhaDigital.substring(0, 3);
        boleto.setBancoEmissor(codigoBanco); // Define o código do banco extraído da linha digitável

        System.out.println("🏦 Código do banco extraído da linha digital: " + codigoBanco);

        ConsultaBanco consultaBanco = new ConsultaBanco(boleto);
        String statusConsultaBanco = consultaBanco.validarBancoComApi(); // Isso preenche os campos do boleto
        boleto.setStatusValidacaoBanco(statusConsultaBanco); // Define o status de validação do banco no boleto

        System.out.println("ℹ️ Status da validação do banco com a API: " + statusConsultaBanco);

        // Exibir os dados puxados da API
        System.out.println("\n--- Dados do Banco " + codigoBanco + " Retornados pela BrasilAPI ---");
        System.out.println("🏦 Código do Banco (API): " + (boleto.getBancoEmissor() != null ? boleto.getBancoEmissor() : "Não disponível"));
        System.out.println("📝 Nome do Banco (API): " + (boleto.getNomeBancoApi() != null ? boleto.getNomeBancoApi() : "Não disponível"));
        System.out.println("✨ Nome Completo do Banco (API): " + (boleto.getNomeCompletoBancoApi() != null ? boleto.getNomeCompletoBancoApi() : "Não disponível"));
        System.out.println("🔢 ISPB (API): " + (boleto.getIspbBancoApi() != null ? boleto.getIspbBancoApi() : "Não disponível"));
        System.out.println("--------------------------------------------------");

        // Pergunta de confirmação ao usuário
        System.out.println("\nAs informações do banco acima estão corretas? (sim/nao)");
        String confirmacaoDadosBanco = scanner.nextLine().trim().toLowerCase();

        if ("sim".equals(confirmacaoDadosBanco)) {
            System.out.println("👍 Confirmação dos dados do banco registrada.");
            if (statusConsultaBanco.equals("VALIDO_API")) {
                boleto.setStatusValidacaoBanco("VALIDO_BANCO_API_E_USUARIO");
            } else {
                boleto.setStatusValidacaoBanco("BANCO_CONFIRMADO_USUARIO_COM_ALERTA"); 
            }
        } else {
            System.out.println("🚫 Você indicou que os dados do banco não estão corretos. Marcarei como 'BANCO_NAO_CONFIRMADO_USUARIO'.");
            boleto.setStatusValidacaoBanco("BANCO_NAO_CONFIRMADO_USUARIO");
            verificacoesComFalha++;
        }
        // --- FIM DA LÓGICA PARA CONFIRMAÇÃO DETALHADA DO BANCO ---

        // Atualiza o status geral do boleto com base nas falhas de verificação
        if (verificacoesComFalha > 0 && !"ALERTA_FRAUDE_NOME_CNPJ_DIVERGENTE".equals(boleto.getStatusValidacao())) {
            boleto.setStatusValidacao("ALERTA_GERAL_NAO_CONFORMIDADE");
            boleto.setDenunciado(true); // Marca como denunciado automaticamente se houver falhas
        } else if (verificacoesComFalha == 0 && !"ALERTA_FRAUDE_NOME_CNPJ_DIVERGENTE".equals(boleto.getStatusValidacao()) && !"ERRO_ESTRUTURA_LD".equals(boleto.getStatusValidacao())) {
            // Se não houve falhas e não há alerta de fraude por nome/CNPJ ou erro de estrutura LD
            boleto.setStatusValidacao("VALIDO_COMPLETO");
        }


        // Associa o boleto a um usuário anônimo antes de salvar
        Usuario usuarioAnonimo = repositorioUsuario.criarUsuarioAnonimo();
        if (usuarioAnonimo == null || usuarioAnonimo.getId() == 0) {
            System.err.println("❌ Falha crítica: Não foi possível criar um usuário anônimo. O boleto não será salvo.");
            return;
        }
        System.out.println("🔗 Novo usuário anônimo criado com ID: " + usuarioAnonimo.getId());
        boleto.setUsuarioId(usuarioAnonimo.getId());

        System.out.println("\n--- Resumo e Preparação para Salvamento ---");
        System.out.println("Linha Digital: " + boleto.getCodigoBarras());
        System.out.println("Valor Informado pelo Usuário: " + boleto.getValor());
        System.out.println("Valor Extraído da Linha Digital: " + valorDoCodigoBarras);
        System.out.println("CNPJ Beneficiário Informado: " + boleto.getCnpjEmitente());
        System.out.println("Razão Social (API): " + (boleto.getRazaoSocialApi() != null ? boleto.getRazaoSocialApi() : "Não disponível"));
        System.out.println("Nome Fantasia (API): " + (boleto.getNomeFantasiaApi() != null ? boleto.getNomeFantasiaApi() : "Não disponível"));
        System.out.println("Código Banco (Linha Digital): " + boleto.getBancoEmissor());
        System.out.println("Nome Banco (API): " + (boleto.getNomeBancoApi() != null ? boleto.getNomeBancoApi() : "Não disponível"));
        System.out.println("Nome Completo Banco (API): " + (boleto.getNomeCompletoBancoApi() != null ? boleto.getNomeCompletoBancoApi() : "Não disponível"));
        System.out.println("ISPB (API): " + (boleto.getIspbBancoApi() != null ? boleto.getIspbBancoApi() : "Não disponível"));
        System.out.println("Informações CNPJ e Banco Confirmadas pelo Usuário: " + (boleto.isInformacoesConfirmadasPeloUsuario() ? "Sim" : "Não"));
        System.out.println("Status de Validação Geral: " + boleto.getStatusValidacao());
        System.out.println("Status de Validação de Banco: " + boleto.getStatusValidacaoBanco());
        System.out.println("Denunciado Automaticamente: " + (boleto.isDenunciado() ? "Sim" : "Não"));

        System.out.println("\n💾 Tentando salvar boleto no banco de dados...");
        try {
            if (repositorioLinhaDigitavel.inserirBoletoPorLinhaDigitavel(boleto)) {
                System.out.println("🎉 Boleto salvo no banco de dados com sucesso!");
            } else {
                System.err.println("❌ Falha desconhecida ao salvar o boleto.");
            }
        } catch (SQLException e) {
            System.err.println("❌ Erro ao salvar boleto no banco de dados: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-lança a exceção para tratamento superior
        }

        System.out.println("\nProcessamento da linha digital concluído.");
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
                    System.out.println("✅ CNPJ Emitente '" + cnpj + "' já existe na tabela CNPJ_Emitente.");
                    return;
                }
            }

            try (PreparedStatement insertStmt = conexao.prepareStatement(insertSql)) {
                insertStmt.setString(1, cnpj);
                insertStmt.setString(2, nomeRazaoSocial != null && !nomeRazaoSocial.isEmpty() ? nomeRazaoSocial : "Desconhecido (Linha Digital)");
                insertStmt.setDate(3, Date.valueOf(LocalDate.now())); // Usa a data atual como data de abertura

                int linhasAfetadas = insertStmt.executeUpdate();
                if (linhasAfetadas > 0) {
                    System.out.println("➕ CNPJ Emitente '" + cnpj + "' inserido na tabela CNPJ_Emitente.");
                } else {
                    System.err.println("❌ Falha ao inserir CNPJ Emitente '" + cnpj + "' na tabela CNPJ_Emitente.");
                }
            }
        }
    }
}