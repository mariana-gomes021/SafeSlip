/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package boleto; // Ou outro pacote adequado, como 'safeslip.servicos'
import boleto.extracao.ExtracaoBoleto;
import boleto.EnvioBoleto;
import usuario.Boleto;
import usuario.Usuario;
import bancodedados.RepositorioBoleto;
import bancodedados.RepositorioUsuario;
import bancodedados.ConexaoBD;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date; 
import java.time.LocalDate;
import java.util.Scanner;

public class ProcessadorBoleto {

    private ExtracaoBoleto extracaoBoleto;
    private RepositorioBoleto repositorioBoleto;
    private RepositorioUsuario repositorioUsuario;
    private Scanner scanner;
    private EnvioBoleto envioBoleto;
    private File arquivoTxtParaApagar; 

    public ProcessadorBoleto(Scanner scanner) {
        this.extracaoBoleto = new ExtracaoBoleto();
        this.repositorioBoleto = new RepositorioBoleto();
        this.repositorioUsuario = new RepositorioUsuario();
        this.scanner = scanner;
        this.envioBoleto = new EnvioBoleto();
    }

    public void processarNovoBoleto() throws IOException, SQLException {
        File pdfSelecionado = envioBoleto.selecionarArquivoPDF(); 
        if (pdfSelecionado == null) {
            System.err.println("❌ Processamento cancelado: Nenhum arquivo PDF foi selecionado.");
            return;
        }
        
        extracaoBoleto.setCaminhoToArquivo(pdfSelecionado);
        this.arquivoTxtParaApagar = extracaoBoleto.getArquivoTxtGerado(pdfSelecionado);

        System.out.println("\n⏳ Iniciando extração do boleto...");
        Boleto boletoExtraido = extracaoBoleto.processarTxt(); 

        boolean extracaoMinimaBemSucedida = 
            boletoExtraido != null && 
            boletoExtraido.getCodigoBarras() != null && 
            !boletoExtraido.getCodigoBarras().isEmpty() &&
            boletoExtraido.getCnpjEmitente() != null &&
            !boletoExtraido.getCnpjEmitente().isEmpty();

        if (extracaoMinimaBemSucedida) {
            System.out.println("\n--- Detalhes do Boleto Extraído para Confirmação ---");
            System.out.println("💰 Valor: " + (boletoExtraido.getValor() != null ? boletoExtraido.getValor() : "Não encontrado"));
            System.out.println("🗓️ Data de Vencimento: " + (boletoExtraido.getVencimento() != null ? boletoExtraido.getVencimento().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Não encontrado"));
            System.out.println("🏢 CNPJ do Beneficiário: " + (boletoExtraido.getCnpjEmitente() != null ? boletoExtraido.getCnpjEmitente() : "Não encontrado"));
            System.out.println("📝 Nome do Beneficiário: " + (boletoExtraido.getNomeBeneficiario() != null ? boletoExtraido.getNomeBeneficiario() : "Não encontrado"));
            System.out.println("🏦 Banco Emissor: " + (boletoExtraido.getBancoEmissor() != null ? boletoExtraido.getBancoEmissor() : "Não encontrado"));
            System.out.println("🔢 Número do Código de Barras: " + (boletoExtraido.getCodigoBarras() != null ? boletoExtraido.getCodigoBarras() : "Não encontrado"));
            System.out.println("--------------------------------------------------");

            System.out.println("\nAs informações do boleto estão corretas? (sim/nao)");
            String confirmacao = scanner.nextLine();

            if ("sim".equalsIgnoreCase(confirmacao) || "nao".equalsIgnoreCase(confirmacao)) {
                System.out.println("👍 Confirmação registrada. Prosseguindo com o salvamento/verificação.");

                String cnpjEmitente = boletoExtraido.getCnpjEmitente();
                if (cnpjEmitente != null && !cnpjEmitente.isEmpty()) {
                    inserirOuAtualizarCnpjEmitente(cnpjEmitente, boletoExtraido.getNomeBeneficiario()); 
                } else {
                    System.err.println("⚠️ CNPJ do emitente não extraído ou inválido. O boleto não poderá ser salvo devido à restrição de chave estrangeira.");
                    return; 
                }
                
                Usuario usuarioAnonimo = repositorioUsuario.criarUsuarioAnonimo();
                if (usuarioAnonimo == null || usuarioAnonimo.getId() == 0) {
                    System.err.println("❌ Falha crítica: Não foi possível criar um usuário anônimo. O boleto não será salvo.");
                    return;
                }
                System.out.println("🔗 Novo usuário anônimo criado com ID: " + usuarioAnonimo.getId());
                
                boletoExtraido.setUsuarioId(usuarioAnonimo.getId()); 

                System.out.println("\n💾 Tentando salvar boleto no banco de dados...");
                // A chamada para inserirBoleto agora trata a duplicidade internamente
                if (repositorioBoleto.inserirBoleto(boletoExtraido)) { 
                    System.out.println("🎉 Operação concluída para o boleto (salvo ou já existente)!");
                    apagarArquivoTxtGerado(); 
                } else {
                    System.err.println("❌ Falha desconhecida ao salvar o boleto.");
                }

            } else { 
                System.out.println("❓ Resposta inválida. O boleto não será salvo."); 
            }

        } else {
            System.out.println("\n🚫 Não foi possível extrair informações essenciais do boleto (código de barras ou CNPJ). Verifique o arquivo.");
        }
    }

    private void apagarArquivoTxtGerado() {
        if (arquivoTxtParaApagar != null && arquivoTxtParaApagar.exists()) {
            if (arquivoTxtParaApagar.delete()) {
                System.out.println("🗑️ Arquivo TXT '" + arquivoTxtParaApagar.getName() + "' apagado com sucesso.");
            } else {
                System.err.println("❌ Não foi possível apagar o arquivo TXT '" + arquivoTxtParaApagar.getName() + "'.");
            }
        } else {
            System.out.println("ℹ️ Nenhum arquivo TXT para apagar ou arquivo não existe.");
        }
    }

    private void inserirOuAtualizarCnpjEmitente(String cnpj, String nomeRazaoSocial) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM CNPJ_Emitente WHERE cnpj = ?";
        String insertSql = "INSERT INTO CNPJ_Emitente (cnpj, nome_razao_social, situacao_cadastral, data_abertura) VALUES (?, ?, ?, ?)";
        
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
                insertStmt.setString(2, nomeRazaoSocial != null && !nomeRazaoSocial.isEmpty() ? nomeRazaoSocial : "Desconhecido"); 
                insertStmt.setString(3, "ATIVA"); 
                insertStmt.setDate(4, Date.valueOf(LocalDate.now())); 
                
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