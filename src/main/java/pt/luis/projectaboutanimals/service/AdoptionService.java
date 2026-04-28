package pt.luis.projectaboutanimals.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.luis.projectaboutanimals.model.*;
import pt.luis.projectaboutanimals.dao.AdoptionEventRepository;
import pt.luis.projectaboutanimals.dao.AdoptionRequestRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class AdoptionService {

    private final AdoptionRequestRepository requests;
    private final AdoptionEventRepository events;
    private final MailService mail;
    private final IcsService ics;

    public AdoptionService(AdoptionRequestRepository requests,
                           AdoptionEventRepository events,
                           MailService mail,
                           IcsService ics) {
        this.requests = requests;
        this.events = events;
        this.mail = mail;
        this.ics = ics;
    }

    // ===================== CLIENT (MY ADOPTIONS) =====================

    @Transactional(readOnly = true)
    public List<AdoptionRequest> myAdoptions(Long applicantId) {
        if (applicantId == null) throw new IllegalArgumentException("applicantId obrigatório.");
        return requests.findMineWithReportAndApplicant(applicantId);
    }

    @Transactional(readOnly = true)
    public AdoptionRequest myAdoptionDetail(Long adoptionId, Long applicantId) {
        if (adoptionId == null || applicantId == null) {
            throw new IllegalArgumentException("Parâmetros inválidos.");
        }

        var mine = requests.findMineByIdWithReportAndApplicant(adoptionId, applicantId);
        if (mine.isPresent()) return mine.get();

        if (!requests.existsById(adoptionId)) {
            throw new IllegalArgumentException("Pedido não encontrado.");
        }

        throw new SecurityException("Sem permissões para ver este pedido.");
    }

    @Transactional(readOnly = true)
    public List<AdoptionEvent> myAdoptionEvents(Long adoptionId, Long applicantId) {
        if (adoptionId == null || applicantId == null) {
            throw new IllegalArgumentException("Parâmetros inválidos.");
        }

        // owner-check consistente
        myAdoptionDetail(adoptionId, applicantId);

        return events.findVisibleTimelineForApplicant(adoptionId, applicantId);
    }

    @Transactional(readOnly = true)
    public Set<Long> blockedReportIdsForAdoptionButton() {
        var ids = requests.findReportIdsWithActiveProcess(
                List.of(AdoptionStatus.REJEITADO, AdoptionStatus.CANCELADO, AdoptionStatus.ADOTADO)
        );
        return new java.util.HashSet<>(ids);
    }

    // ===================== ADMIN FLOW =====================

    /**
     * ✅ Mudança genérica de estado via dropdown (Admin).
     * Regista evento na timeline (visível ou não ao candidato).
     */
    @Transactional
    public void adminChangeStatus(Long adoptionId,
                                  AdoptionStatus status,
                                  String note,
                                  boolean visibleToApplicant) {

        if (adoptionId == null || adoptionId < 1) {
            throw new IllegalArgumentException("ID inválido.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status obrigatório.");
        }

        AdoptionRequest a = requests.findByIdWithReportAndApplicant(adoptionId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido de adoção não encontrado."));

        AdoptionStatus from = a.getStatus();

        // ✅ NOVO: se não há mudança real, não faz nada (evita duplicados "X -> X")
        if (from == status) {
            return;
        }

        // guards mínimos (ajusta conforme o teu negócio)
        if (from == AdoptionStatus.REJEITADO) {
            throw new IllegalStateException("Pedido rejeitado: não deve mudar de estado.");
        }
        if (from == AdoptionStatus.CANCELADO) {
            throw new IllegalStateException("Pedido cancelado: não deve mudar de estado.");
        }
        if (from == AdoptionStatus.ADOTADO) {
            throw new IllegalStateException("Pedido concluído (ADOTADO): não deve mudar de estado.");
        }

        // Exemplo de coerência: marcar visita só via scheduleVisitAndEmail
        if (status == AdoptionStatus.VISITA_MARCADA && a.getVisitStartAt() == null) {
            throw new IllegalStateException("Para VISITA_MARCADA tens de marcar a visita (data/hora).");
        }

        // atualizar estado
        a.setStatus(status);
        requests.save(a);

        // timeline
        String finalNote = buildStatusChangeNote(from, status, note);

        events.save(new AdoptionEvent(
                a,
                AdoptionEventType.STATUS_CHANGED,
                finalNote,
                visibleToApplicant
        ));
    }

    @Transactional
    public void scheduleVisitAndEmail(Long adoptionId,
                                      Instant start,
                                      int durationMinutes,
                                      String location,
                                      String note) {

        if (start == null) throw new IllegalArgumentException("Data/hora obrigatória.");
        if (durationMinutes < 10 || durationMinutes > 240) {
            throw new IllegalArgumentException("Duração inválida (10 a 240 minutos).");
        }
        if (start.isBefore(Instant.now().plusSeconds(60))) {
            throw new IllegalArgumentException("A visita tem de ser no futuro.");
        }

        AdoptionRequest a = requests.findByIdWithReportAndApplicant(adoptionId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido de adoção não encontrado."));

        if (a.getStatus() == AdoptionStatus.REJEITADO) {
            throw new IllegalStateException("Este pedido já foi rejeitado.");
        }
        if (a.getStatus() == AdoptionStatus.CANCELADO) {
            throw new IllegalStateException("Este pedido foi cancelado.");
        }
        if (a.getStatus() == AdoptionStatus.ADOTADO) {
            throw new IllegalStateException("Este pedido já foi concluído.");
        }

        Instant end = start.plus(Duration.ofMinutes(durationMinutes));

        String safeLocation = (location == null || location.isBlank())
                ? (a.getReport() != null ? ns(a.getReport().getLocationText()) : "")
                : location;

        String uid = (a.getIcsUid() == null || a.getIcsUid().isBlank())
                ? ics.newUid()
                : a.getIcsUid();

        a.setStatus(AdoptionStatus.VISITA_MARCADA);
        a.setVisitStartAt(start);
        a.setVisitEndAt(end);
        a.setVisitLocation(safeLocation);
        a.setVisitNote(note);
        a.setIcsUid(uid);

        requests.save(a);

        events.save(new AdoptionEvent(
                a,
                AdoptionEventType.VISIT_SCHEDULED,
                buildEventNote("Visita marcada", start, durationMinutes, safeLocation, note),
                true
        ));

        String reportTitle = (a.getReport() != null) ? ns(a.getReport().getTitle()) : "Report";
        String summary = "Visita para adoção — Pet Alerta (Pedido #" + a.getId() + ")";

        String description =
                "Olá " + ns(a.getFullName()) + "\\n\\n" +
                        "A tua visita foi marcada.\\n" +
                        "Pedido: #" + a.getId() + "\\n" +
                        (a.getReport() != null ? ("Report: #" + a.getReport().getId() + " — " + reportTitle + "\\n") : "") +
                        (note != null && !note.isBlank() ? ("\\nNota do admin: " + note + "\\n") : "") +
                        "\\nPet Alerta";

        String organizer = mail.organizerEmailOrNull();
        String icsContent = ics.buildVisitEvent(
                uid,
                start,
                end,
                summary,
                safeLocation,
                description,
                organizer,
                a.getEmail(),
                a.getFullName()
        );

        String emailBody =
                "Olá " + ns(a.getFullName()) + ",\n\n" +
                        "A tua visita foi marcada.\n" +
                        "Local: " + ns(safeLocation) + "\n" +
                        "Duração: " + durationMinutes + " min\n\n" +
                        (note != null && !note.isBlank() ? ("Nota: " + note + "\n\n") : "") +
                        "Segue em anexo o convite de calendário (.ics).\n\n" +
                        "Pet Alerta";

        mail.sendVisitScheduledWithIcs(
                a.getEmail(),
                "Visita marcada — Pedido de adoção #" + a.getId(),
                emailBody,
                "visita-adocao-" + a.getId() + ".ics",
                icsContent
        );

        a.setEmailSentAt(Instant.now());
        requests.save(a);

        events.save(new AdoptionEvent(
                a,
                AdoptionEventType.EMAIL_SENT,
                "Email de agendamento enviado com anexo .ics.",
                false
        ));
    }

    @Transactional(readOnly = true)
    public AdoptionRequest adminAdoptionDetail(Long adoptionId) {
        if (adoptionId == null || adoptionId < 1) {
            throw new IllegalArgumentException("ID inválido.");
        }
        return requests.findByIdWithReportAndApplicant(adoptionId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido de adoção não encontrado."));
    }

    @Transactional
    public void reject(Long adoptionId, String adminNote) {
        AdoptionRequest a = requests.findByIdWithReportAndApplicant(adoptionId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido de adoção não encontrado."));

        if (a.getStatus() == AdoptionStatus.REJEITADO) return;

        if (a.getStatus() == AdoptionStatus.VISITA_MARCADA) {
            throw new IllegalStateException("Já existe visita marcada; não rejeitar sem cancelar/remarcar.");
        }

        a.setStatus(AdoptionStatus.REJEITADO);
        requests.save(a);

        String note = (adminNote == null || adminNote.isBlank())
                ? "Pedido rejeitado pelo admin."
                : "Pedido rejeitado pelo admin. Nota: " + adminNote;

        events.save(new AdoptionEvent(a, AdoptionEventType.REJECTED, note, true));
    }

    // ===================== HELPERS =====================

    private String buildStatusChangeNote(AdoptionStatus from, AdoptionStatus to, String note) {
        String base = "Estado: " + (from == null ? "—" : from.name()) + " -> " + to.name();
        if (note == null || note.isBlank()) return base;
        return base + "\nNota: " + note;
    }

    private String buildEventNote(String title, Instant start, int durationMinutes, String location, String note) {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append("\n");
        sb.append("Início: ").append(start).append("\n");
        sb.append("Duração: ").append(durationMinutes).append(" min\n");
        sb.append("Local: ").append(ns(location)).append("\n");
        if (note != null && !note.isBlank()) sb.append("Nota: ").append(note).append("\n");
        return sb.toString();
    }

    private String ns(String s) { return s == null ? "" : s; }
}
