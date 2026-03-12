package pt.luis.projectaboutanimals.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.replyTo:}")
    private String replyTo;

    // opcional mas recomendado para ICS (aparece como organizador)
    @Value("${app.mail.organizer:}")
    private String organizer;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envia email com convite ICS (compatível com Outlook/Gmail/Apple Mail):
     * - texto normal + part text/calendar inline + attachment .ics
     */
    public void sendVisitScheduledWithIcs(String to,
                                          String subject,
                                          String bodyText,
                                          String icsFilename,
                                          String icsContent) {

        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Destino (to) é obrigatório.");
        }
        if (subject == null) subject = "";
        if (bodyText == null) bodyText = "";
        if (icsFilename == null || icsFilename.isBlank()) icsFilename = "convite.ics";
        if (icsContent == null || icsContent.isBlank()) {
            throw new IllegalArgumentException("Conteúdo ICS é obrigatório.");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();

            // multipart = true para anexos e partes alternativas
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom(from);

            if (replyTo != null && !replyTo.isBlank()) {
                helper.setReplyTo(replyTo);
            }

            // Corpo do email (texto normal)
            helper.setText(bodyText, false);

            // ICS: bytes UTF-8
            byte[] icsBytes = icsContent.getBytes(StandardCharsets.UTF_8);
            ByteArrayResource icsRes = new ByteArrayResource(icsBytes);

            // 1) inline calendar part (alguns clientes só “entendem” assim)
            // Nota: alguns clientes esperam method=REQUEST no content-type
            helper.addInline("invite-ics",
                    icsRes,
                    "text/calendar; charset=UTF-8; method=REQUEST"
            );

            // 2) attachment .ics (outros clientes só permitem guardar/abrir assim)
            helper.addAttachment(
                    icsFilename,
                    icsRes,
                    "text/calendar; charset=UTF-8; method=REQUEST"
            );

            // Headers extra que ajudam (especialmente Outlook)
            message.addHeader("Content-Class", "urn:content-classes:calendarmessage");
            message.addHeader("X-MS-OLK-FORCEINSPECTOROPEN", "TRUE");

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Falha ao compor email: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao enviar email: " + e.getMessage(), e);
        }
    }

    public void sendSimple(String to, String subject, String bodyText) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Destino (to) é obrigatório.");
        }
        if (subject == null) subject = "";
        if (bodyText == null) bodyText = "";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(bodyText, false);
            helper.setFrom(from);
            if (replyTo != null && !replyTo.isBlank()) helper.setReplyTo(replyTo);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao enviar email: " + e.getMessage(), e);
        }
    }

    /**
     * Opcional: expõe organizador para o IcsService (se quiseres usar)
     */
    public String organizerEmailOrNull() {
        if (organizer == null || organizer.isBlank()) return null;
        return organizer.trim();
    }
}
