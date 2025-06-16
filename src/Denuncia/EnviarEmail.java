package Denuncia;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class EnviarEmail {

    private String host = "smtp.gmail.com";
    private final String USUARIO = "compliance.safeslip@gmail.com";
    private final String SENHA_APP = "igdwxftzrwvsqftp"; //nao mexa

    private String destinatario = "boletos.suspeitos.bcb.gov@gmail.com";

    private Properties props = new Properties();

    private void estabelecerConexao() {
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", "587");
    }

    private Session autenticacao() {
        return Session.getInstance(props,
                new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(USUARIO, SENHA_APP);
                    }
                });
    }

    public void enviar(String cnpj, int totalSuspeitas, Date periodo) {
        estabelecerConexao();
        Session session = autenticacao();

        try {
            Message mensagem = new MimeMessage(session);
            mensagem.setFrom(new InternetAddress(USUARIO));
            mensagem.setRecipient(Message.RecipientType.TO, new InternetAddress(destinatario));
            mensagem.setSubject("🚨 Alerta de atividade suspeita - CNPJ " + cnpj);

            SimpleDateFormat sdf = new SimpleDateFormat("MMMM 'de' yyyy"); // Ex: Junho de 2025
            String periodoFormatado = sdf.format(periodo);

            String corpo = String.format("""
                Prezados(a),

                A SafeSlip, empresa prestadora de serviços especializada na verificação e validação de documentos de cobrança, vem por meio deste comunicar a identificação de indícios de atividade suspeita relacionada ao CNPJ abaixo:

                CNPJ identificado: %s
                Quantidade de boletos suspeitos associados: %d
                Período de ocorrência: %s

                Os comportamentos observados podem ser:
                * Emissão recorrente de boletos com informações inconsistentes;
                * Divergência entre os dados do beneficiário e registros oficiais;
                * Indícios de tentativa de mascaramento por variação nos dados de cobrança.

                A SafeSlip realiza a análise automatizada e manual de milhares de boletos diariamente. O CNPJ em questão ultrapassou os parâmetros de tolerância definidos para anomalias, sendo classificado em nosso sistema de risco como potencialmente fraudulento.

                Permanecemos à disposição para esclarecimentos adicionais.

                Atenciosamente,
                Equipe de Conformidade e Riscos – SafeSlip
                📧 compliance.safeslip@gmail.com.br
                """, cnpj, totalSuspeitas, periodoFormatado);

            mensagem.setText(corpo);

            Transport.send(mensagem);
            System.out.println("E-mail enviado com sucesso para " + destinatario + " sobre o CNPJ " + cnpj + " com " + totalSuspeitas + " suspeitas.");

        } catch (MessagingException ex) {
            System.err.println("Erro ao enviar e-mail para " + destinatario + ": " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}