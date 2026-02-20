package services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService {

    // ------------------- CONFIGURATION GMAIL -------------------
    // Hôte SMTP de Gmail (vous n'avez pas besoin d'autre chose)
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587"; // Port TLS

    // Votre adresse Gmail complète
    private static final String USERNAME = "ghramine7@gmail.com";

    // ⚠️ REMPLACEZ CE MOT DE PASSE par celui que vous générerez dans votre compte Google
    // Il s'agit d'un "mot de passe d'application" (16 caractères)
    private static final String PASSWORD = "bikj pdqi xwhr nbbv";

    /**
     * Envoie un email pour informer l'utilisateur du changement de statut de son compte.
     *
     * @param toEmail   adresse email du destinataire
     * @param newStatus le nouveau statut ("Actif" ou "Suspendu")
     */
    public static void sendStatusChangeEmail(String toEmail, String newStatus) {
        // 1. Propriétés de connexion au serveur SMTP
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true"); // Active TLS
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        // 2. Création de la session avec authentification
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, PASSWORD);
            }
        });

        // Optionnel : activer le mode debug pour voir les échanges avec le serveur
        // session.setDebug(true);

        try {
            // 3. Création du message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Changement de statut de votre compte");

            // 4. Contenu personnalisé selon le statut
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

            // 5. Envoi du message
            Transport.send(message);
            System.out.println("✅ Email de notification envoyé à " + toEmail);

        } catch (MessagingException e) {
            System.err.println("❌ Erreur lors de l'envoi de l'email : " + e.getMessage());
            e.printStackTrace();
        }
    }
}