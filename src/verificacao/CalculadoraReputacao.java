package verificacao;

import usuario.Boleto;
import bancodedados.RepositorioCnpjReputacao;

import java.math.BigDecimal;
import java.sql.SQLException;

public class CalculadoraReputacao {

    // O método calcularEAtualizarReputacao agora é estático e recebe o repositorio como parâmetro
    public static void calcularEAtualizarReputacao(Boleto boleto, boolean isBoletoFalhoParaReputacao, RepositorioCnpjReputacao repositorioCnpjReputacao) throws SQLException {
        String cnpj = boleto.getCnpjEmitente();
        if (cnpj == null || cnpj.isEmpty()) {
            System.err.println("Erro: CNPJ do boleto é nulo ou vazio para calculo de reputacao.");
            return;
        }

        try {
            // Primeiro, atualiza o total de boletos e denuncias no repositório
            // O isBoletoFalhoParaReputacao indica se este boleto específico contribui como uma "denúncia"
            repositorioCnpjReputacao.atualizarReputacaoCnpj(cnpj, isBoletoFalhoParaReputacao, boleto.getTotalAtualizacoes());

            // Em seguida, busca os dados de reputação atualizados para calcular o score e exibir
            Object[] reputacaoAtual = repositorioCnpjReputacao.buscarReputacaoCnpj(cnpj);
            if (reputacaoAtual != null) {
                BigDecimal score = (BigDecimal) reputacaoAtual[0];
                int totalBoletosCnpj = (int) reputacaoAtual[1];
                int totalDenunciasCnpj = (int) reputacaoAtual[2];

                // Atualiza o objeto Boleto com os dados da reputação
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
                System.out.println("Calculo de reputacao concluido para o CNPJ: " + cnpj); // Usar 'cnpj' diretamente

                if ((classificacao.equals("Reincidente") || classificacao.equals("Problemático")) && totalDenunciasCnpj >= 10) {
                    boleto.setSuspeito(true);
                    System.out.println("Boleto de CNPJ classificado como '" + classificacao + "' e com " + totalDenunciasCnpj + " denuncias. Marcado como SUSPEITO automaticamente!");
                    // Se a reputação for muito baixa, pode sobrepor um status de "VÁLIDO_COMPLETO" para "ALERTA_GERAL_NAO_CONFORMIDADE"
                    if ("VALIDO_COMPLETO".equals(boleto.getStatusValidacao())) {
                        boleto.setStatusValidacao("ALERTA_GERAL_NAO_CONFORMIDADE");
                    }
                }
            } else {
                System.out.println("Não foi possível buscar a reputação do CNPJ. Pode ser um novo CNPJ.");
                // Para um CNPJ novo, podemos definir um score inicial para o boleto
                boleto.setScoreReputacaoCnpj(new BigDecimal("100.00"));
                boleto.setTotalBoletosCnpj(1); // Se é um novo, este é o primeiro
                boleto.setTotalDenunciasCnpj(isBoletoFalhoParaReputacao ? 1 : 0);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao processar reputacao do CNPJ: " + e.getMessage());
            throw e; // Re-lança a exceção para que ProcessadorBoleto possa capturá-la
        }
    }
}