package pt.luis.projectaboutanimals.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "adoption_requests", indexes = {
        @Index(name = "ix_adoption_requests_applicant", columnList = "applicant_id"),
        @Index(name = "ix_adoption_requests_report", columnList = "report_id"),
        @Index(name = "ix_adoption_requests_created", columnList = "created_at")
})
public class AdoptionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // quem pede adoção
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id", nullable = false)
    private User applicant;

    // para que report
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private FoundAnimalReport report;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, length = 40)
    private String phone;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(nullable = false, length = 1200)
    private String message;

    // ✅ NOVO: status do processo
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private AdoptionStatus status = AdoptionStatus.PENDENTE;

    // ✅ NOVO: agendamento
    @Column(name = "visit_start_at")
    private Instant visitStartAt;

    @Column(name = "visit_end_at")
    private Instant visitEndAt;

    @Column(name = "visit_location", length = 180)
    private String visitLocation;

    @Column(name = "visit_note", length = 800)
    private String visitNote;

    // ✅ NOVO: ics
    @Column(name = "ics_uid", length = 120)
    private String icsUid;

    // ✅ NOVO: controlo de envio
    @Column(name = "email_sent_at")
    private Instant emailSentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }

    public User getApplicant() { return applicant; }
    public void setApplicant(User applicant) { this.applicant = applicant; }

    public FoundAnimalReport getReport() { return report; }
    public void setReport(FoundAnimalReport report) { this.report = report; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public AdoptionStatus getStatus() { return status; }
    public void setStatus(AdoptionStatus status) { this.status = status; }

    public Instant getVisitStartAt() { return visitStartAt; }
    public void setVisitStartAt(Instant visitStartAt) { this.visitStartAt = visitStartAt; }

    public Instant getVisitEndAt() { return visitEndAt; }
    public void setVisitEndAt(Instant visitEndAt) { this.visitEndAt = visitEndAt; }

    public String getVisitLocation() { return visitLocation; }
    public void setVisitLocation(String visitLocation) { this.visitLocation = visitLocation; }

    public String getVisitNote() { return visitNote; }
    public void setVisitNote(String visitNote) { this.visitNote = visitNote; }

    public String getIcsUid() { return icsUid; }
    public void setIcsUid(String icsUid) { this.icsUid = icsUid; }

    public Instant getEmailSentAt() { return emailSentAt; }
    public void setEmailSentAt(Instant emailSentAt) { this.emailSentAt = emailSentAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
