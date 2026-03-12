package pt.luis.projectaboutanimals.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class IcsService {

    private static final DateTimeFormatter ICS_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

    public String newUid() {
        return UUID.randomUUID() + "@petalerta";
    }

    /**
     * Gera um convite ICS (METHOD:REQUEST) robusto.
     *
     * organizerEmail: pode ser null/blank (nesse caso não escreve ORGANIZER)
     * attendeeEmail: idem (mas é recomendável para mail clients tratarem como convite)
     */
    public String buildVisitEvent(String uid,
                                  Instant start,
                                  Instant end,
                                  String summary,
                                  String location,
                                  String description,
                                  String organizerEmail,
                                  String attendeeEmail,
                                  String attendeeName) {

        if (uid == null || uid.isBlank()) throw new IllegalArgumentException("uid obrigatório.");
        if (start == null || end == null) throw new IllegalArgumentException("start/end obrigatórios.");
        if (summary == null) summary = "";
        if (location == null) location = "";
        if (description == null) description = "";

        String dtStamp = ICS_FMT.format(Instant.now());
        String dtStart = ICS_FMT.format(start);
        String dtEnd   = ICS_FMT.format(end);

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("PRODID:-//Pet Alerta//PT\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("METHOD:REQUEST\r\n");

        // timezone-less UTC timestamps (Z)
        sb.append("BEGIN:VEVENT\r\n");
        sb.append(fold("UID:" + escape(uid))).append("\r\n");
        sb.append("DTSTAMP:").append(dtStamp).append("\r\n");
        sb.append("DTSTART:").append(dtStart).append("\r\n");
        sb.append("DTEND:").append(dtEnd).append("\r\n");

        // Sequência ajuda em updates
        sb.append("SEQUENCE:0\r\n");
        sb.append("STATUS:CONFIRMED\r\n");
        sb.append("TRANSP:OPAQUE\r\n");

        sb.append(fold("SUMMARY:" + escape(summary))).append("\r\n");

        if (!location.isBlank()) {
            sb.append(fold("LOCATION:" + escape(location))).append("\r\n");
        }
        if (!description.isBlank()) {
            sb.append(fold("DESCRIPTION:" + escape(description))).append("\r\n");
        }

        // Organizer / Attendee — melhora MUITO o comportamento em Outlook/Apple Mail
        if (organizerEmail != null && !organizerEmail.isBlank()) {
            sb.append(fold("ORGANIZER:MAILTO:" + escapeEmail(organizerEmail.trim()))).append("\r\n");
        }

        if (attendeeEmail != null && !attendeeEmail.isBlank()) {
            String cn = (attendeeName == null || attendeeName.isBlank()) ? attendeeEmail : attendeeName;
            sb.append(fold("ATTENDEE;CN=" + escapeParam(cn) + ";ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE:MAILTO:" +
                    escapeEmail(attendeeEmail.trim()))).append("\r\n");
        }

        sb.append("END:VEVENT\r\n");
        sb.append("END:VCALENDAR\r\n");

        return sb.toString();
    }

    // Escape básico para valores ICS (texto)
    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace(",", "\\,")
                .replace(";", "\\;");
    }

    // Escape para parâmetros (CN=...)
    private String escapeParam(String s) {
        if (s == null) return "";
        // em params, vírgula e ponto-e-vírgula também são sensíveis
        return s.replace("\\", "\\\\")
                .replace("\"", "'")
                .replace(",", "\\,")
                .replace(";", "\\;");
    }

    // emails: manter simples e seguro
    private String escapeEmail(String s) {
        if (s == null) return "";
        return s.replace("\r", "").replace("\n", "").trim();
    }

    /**
     * Folding (RFC5545): linhas > 75 octetos devem ser dobradas.
     * Implementação simples por comprimento de chars (suficiente para PT na prática).
     */
    private String fold(String line) {
        if (line == null) return "";
        int limit = 73; // deixa margem; o folding adiciona " " (space)
        if (line.length() <= limit) return line;

        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < line.length()) {
            int end = Math.min(i + limit, line.length());
            out.append(line, i, end);
            i = end;
            if (i < line.length()) out.append("\r\n ");
        }
        return out.toString();
    }
}
