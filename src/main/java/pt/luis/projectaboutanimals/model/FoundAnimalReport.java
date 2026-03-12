package pt.luis.projectaboutanimals.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Entity
@Table(
        name = "found_animal_reports",
        indexes = {
                @Index(name = "ix_report_status", columnList = "status"),
                @Index(name = "ix_report_created_by", columnList = "created_by_id")
        }
)
public class FoundAnimalReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(length = 200, nullable = false)
    private String title;

    @NotBlank
    @Size(max = 80)
    @Column(length = 80, nullable = false)
    private String species;

    @Size(max = 120)
    @Column(length = 120)
    private String breed;

    @Size(max = 80)
    @Column(length = 80)
    private String color;

    @Size(max = 50)
    @Column(length = 50)
    private String size;

    @Size(max = 150)
    @Column(length = 150)
    private String approxAge;

    @NotBlank
    @Size(max = 300)
    @Column(length = 300, nullable = false)
    private String locationText;

    @NotNull
    @Column(nullable = false)
    private Instant foundAt;

    @Size(max = 2000)
    @Column(length = 2000)
    private String notes;

    // caminho relativo público (ex: /uploads/abc.jpg)
    @Size(max = 500)
    @Column(length = 500)
    private String photoUrl;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportStatus status = ReportStatus. PENDENTE;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @NotNull
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = ReportStatus. PENDENTE;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    // getters/setters
    public Long getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }

    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getApproxAge() { return approxAge; }
    public void setApproxAge(String approxAge) { this.approxAge = approxAge; }

    public String getLocationText() { return locationText; }
    public void setLocationText(String locationText) { this.locationText = locationText; }

    public Instant getFoundAt() { return foundAt; }
    public void setFoundAt(Instant foundAt) { this.foundAt = foundAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
