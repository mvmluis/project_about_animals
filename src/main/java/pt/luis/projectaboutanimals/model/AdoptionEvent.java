package pt.luis.projectaboutanimals.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "adoption_events", indexes = {
        @Index(name = "ix_adoption_events_adoption_id", columnList = "adoption_request_id"),
        @Index(name = "ix_adoption_events_created_at", columnList = "created_at")
})
public class AdoptionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "adoption_request_id", nullable = false)
    private AdoptionRequest adoptionRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private AdoptionEventType type;

    @Column(name = "note", length = 1200)
    private String note;

    @Column(name = "visible_to_applicant", nullable = false)
    private boolean visibleToApplicant = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected AdoptionEvent() {}

    public AdoptionEvent(AdoptionRequest adoptionRequest, AdoptionEventType type, String note, boolean visibleToApplicant) {
        this.adoptionRequest = adoptionRequest;
        this.type = type;
        this.note = note;
        this.visibleToApplicant = visibleToApplicant;
    }

    public Long getId() { return id; }
    public AdoptionRequest getAdoptionRequest() { return adoptionRequest; }
    public AdoptionEventType getType() { return type; }
    public String getNote() { return note; }
    public boolean isVisibleToApplicant() { return visibleToApplicant; }
    public Instant getCreatedAt() { return createdAt; }

    public void setAdoptionRequest(AdoptionRequest adoptionRequest) { this.adoptionRequest = adoptionRequest; }
    public void setType(AdoptionEventType type) { this.type = type; }
    public void setNote(String note) { this.note = note; }
    public void setVisibleToApplicant(boolean visibleToApplicant) { this.visibleToApplicant = visibleToApplicant; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
