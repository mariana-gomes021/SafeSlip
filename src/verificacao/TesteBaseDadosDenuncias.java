/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package verificacao;

import bancodedados.ConexaoBD;

/**
 *
 * @author luara
 */
public class TesteBaseDadosDenuncias {

    public static void main(String[] args) {
        ConexaoBD conexao = new ConexaoBD();
        BaseDadosDenuncias baseDenuncias = new BaseDadosDenuncias(conexao);

        String cnpjTeste = "12.000.000/0001-01";
        boolean denunciado = baseDenuncias.cnpjDenunciado(cnpjTeste);

        System.err.println("O CNPJ " + cnpjTeste + (denunciado ? " ja foi denunciado." : " nao foi denunciado."));
        // é pra retornar O CNPJ 12.000.000/0001-01 não foi denunciado.
    }
}
