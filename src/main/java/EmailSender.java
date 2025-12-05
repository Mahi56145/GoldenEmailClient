import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.File;
import java.util.List;
import java.util.Properties;

public class EmailSender {
    private String userEmail;
    private String userPassword;
    private Session session;
    private Transport transport;

    public EmailSender(String email, String password) {
        this.userEmail = email;
        this.userPassword = password;
    }

    private void ensureConnected() throws MessagingException {
        if (transport == null || !transport.isConnected()) {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");

            // INCREASE TIMEOUTS to 15 seconds (15000 ms)
            props.put("mail.smtp.connectiontimeout", "15000");
            props.put("mail.smtp.timeout", "15000");
            props.put("mail.smtp.writetimeout", "15000");

            String host = "smtp.gmail.com";
            if (userEmail.contains("yahoo")) host = "smtp.mail.yahoo.com";
            else if (userEmail.contains("outlook")) host = "smtp.office365.com";

            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", "587");

            session = Session.getInstance(props, new Authenticator() {
                @Override protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(userEmail, userPassword);
                }
            });

            transport = session.getTransport("smtp");
            transport.connect(host, userEmail, userPassword);
        }
    }

    public void sendEmail(String toEmail, String subject, String body, List<File> attachments) throws Exception {
        ensureConnected(); // Reuse connection if possible

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(userEmail));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);

        Multipart multipart = new MimeMultipart();
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setContent(body, "text/html; charset=utf-8");
        multipart.addBodyPart(textPart);

        if (attachments != null) {
            for (File item : attachments) {
                MimeBodyPart filePart = new MimeBodyPart();
                filePart.attachFile(item);
                multipart.addBodyPart(filePart);
            }
        }
        message.setContent(multipart);

        // Use the open transport to send
        transport.sendMessage(message, message.getAllRecipients());
        System.out.println("Email sent successfully!");

        // Note: We intentionally DO NOT close the transport here to keep it ready for the next email.
    }
}