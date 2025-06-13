package safeslip;

import java.sql.SQLException;
import java.util.Scanner;
import usuario.Escolha;

public class SafeSlip {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("Seja bem vindo ao SafeSlip, seu verificador de boletos suspeitos!");
        System.out.println("Digite sua escolha (pdf ou linha digitavel):");
        String entrada = scanner.nextLine();


        Escolha escolha = new Escolha();
        escolha.setResposta(entrada, scanner);
        try {
            escolha.escolha(); // <- ESSENCIAL
        } catch (SQLException e) {
            System.out.println(e);
        }

    }
}


