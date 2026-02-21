package services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService {

    // ------------------- CONFIGURATION GMAIL -------------------
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String USERNAME = "ghramine7@gmail.com";
    private static final String PASSWORD = "bikj pdqi xwhr nbbv";

    /**
     * Envoie un email pour informer l'utilisateur du changement de statut de son compte.
     *
     * @param toEmail   adresse email du destinataire
     * @param newStatus le nouveau statut ("Actif" ou "Suspendu")
     */
    public static void sendStatusChangeEmail(String toEmail, String newStatus) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Changement de statut de votre compte");

            String content;
            if ("Actif".equalsIgnoreCase(newStatus) || "Active".equalsIgnoreCase(newStatus)) {
                content = "Bonjour,\n\n"
                        + "Votre compte a été activé. Vous pouvez désormais vous connecter.\n\n"
                        + "Cordialement,\n"
                        + "L'équipe.";
            } else {
                content = "Bonjour,\n\n"
                        + "Votre compte a été suspendu. Pour plus d'informations, veuillez contacter l'administrateur.\n\n"
                        + "Cordialement,\n"
                        + "L'équipe.";
            }
            message.setText(content);

            Transport.send(message);
            System.out.println("✅ Email de notification envoyé à " + toEmail);

        } catch (MessagingException e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Envoie un email de notification pour le changement de statut d'une réclamation.
     *
     * @param toEmail   adresse du destinataire (propriétaire de la réclamation)
     * @param titre     titre de la réclamation
     * @param projet    projet concerné
     * @param newStatus nouveau statut ("Rèsolu" ou "Fermer")
     */
    public static void sendReclamationStatusEmail(String toEmail, String titre, String projet, String newStatus) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Mise à jour de votre réclamation");

            String content = "Bonjour,\n\n"
                    + "Le statut de votre réclamation \"" + titre + "\" (projet : " + projet + ") "
                    + "a été mis à jour : " + newStatus + ".\n\n"
                    + "Cordialement,\n"
                    + "L'équipe de support.";

            message.setText(content);
            Transport.send(message);
            System.out.println("✅ Email de notification de réclamation envoyé à " + toEmail);

        } catch (MessagingException e) {
            System.err.println("❌ Erreur envoi email réclamation : " + e.getMessage());
            e.printStackTrace();
        }
    }
}