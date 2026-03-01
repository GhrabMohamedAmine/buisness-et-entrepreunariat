package services;
import entities.Formation;
import entities.User;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.File;
import java.util.Properties;
public class MailServiceCertificat {




        // ⚠️ TON EMAIL ICI
        private final String username = "mariem@longevityplus.store";
        private final String password = "mariemmariem";

        public void sendCertificateEmail(User user, Formation formation, File certificate) {

            new Thread(() -> {

                try {

                    Properties props = new Properties();
                    props.put("mail.smtp.host", "longevityplus.store");
                    props.put("mail.smtp.port", "465");
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.ssl.enable", "true");

                    Session session = Session.getInstance(props, new Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password);
                        }
                    });

                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(username, "Plateforme Formation"));
                    message.setRecipients(
                            Message.RecipientType.TO,
                            InternetAddress.parse(user.getEmail())
                    );

                    message.setSubject("🎓 Votre certificat de réussite - " + formation.getTitre());

                    // ===== HTML BODY =====
                    MimeBodyPart textPart = new MimeBodyPart();
                    textPart.setContent(
                            "<h2>Félicitations " + user.getName() + " 👏</h2>"
                                    + "<p>Vous avez réussi la formation :</p>"
                                    + "<h3 style='color:#0f5c8d'>" + formation.getTitre() + "</h3>"
                                    + "<p>Votre certificat officiel est en pièce jointe.</p>"
                                    + "<br>"
                                    + "<b>MERCI DE LIRE CE COURRIEL AU COMPLET</b>"
                                    + "<p>Votre compte d'hébergement a été configuré et cet email contient toutes les informations nécessaires.</p>"
                                    + "<p>Veuillez conserver ce certificat. Il prouve votre réussite officielle.</p>"
                                    + "<br>"
                                    + "<p>Cordialement,<br><b>Plateforme e-Learning</b></p>",
                            "text/html; charset=utf-8"
                    );

                    // ===== ATTACHMENT =====
                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    attachmentPart.attachFile(certificate);

                    Multipart multipart = new MimeMultipart();
                    multipart.addBodyPart(textPart);
                    multipart.addBodyPart(attachmentPart);

                    message.setContent(multipart);

                    Transport.send(message);

                    System.out.println("EMAIL CERTIFICAT ENVOYÉ ✔");

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }).start();
        }

}
