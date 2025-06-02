/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package usuario;

import boleto.EnvioBoleto;
import boleto.extracao.ExtracaoBoleto;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class Usuario {

    private int id;
    private String nome, cpf;
    private EnvioBoleto envioBoleto = new EnvioBoleto();
    private ExtracaoBoleto extracaoBoleto = new ExtracaoBoleto();

    public Usuario(String nome, String cpf) {
        this.nome = nome;
        this.cpf = cpf;
    }

    public Usuario() {
    }

    public File enviarBoleto() {
        File boletoSelecionado = envioBoleto.selecionarArquivoPDF();

        // fazer essa tratativa no botao nao aqui, aqui é so o envio
        if (boletoSelecionado != null) {
            System.out.println("Boleto enviado com sucesso:");
            System.out.println("Nome do arquivo: " + boletoSelecionado.getName());
            System.out.println("Caminho completo: " + boletoSelecionado.getAbsolutePath());
            return boletoSelecionado;
        } else {
            System.out.println("Nenhum boleto foi enviado.");
            return null;
        }
    }

    public String visualizarEConfirmarDadosPdf() throws IOException {
        extracaoBoleto.processarTxt();
        System.out.println("===================================");
        System.out.println("As informações do boleto estao corretas? (sim/nao)");
        String simOuNao;
        Scanner scan = new Scanner(System.in);
        simOuNao = scan.nextLine();

        if (simOuNao.equals("nao") ) {
            System.out.println("nao sei o que escrever!!");
            return null;
        }
        if (simOuNao.equals("sim")) {
            System.out.println("Certo iremos analisar o seu boleto!");
            return "";
        } else {
            System.out.println("sei la");
            return null;
        }
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

}
