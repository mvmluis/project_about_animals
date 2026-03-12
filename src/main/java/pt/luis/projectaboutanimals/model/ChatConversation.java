package pt.luis.projectaboutanimals.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "chat_conversations",
        indexes = {
                @Index(name = "ix_chat_conv_client", columnList = "client_id"),
                @Index(name = "ix_chat_conv_admin", columnList = "admin_id"),
                @Index(name = "ix_chat_conv_adoption", columnList = "adoption_request_id"),
                @Index(name = "ix_chat_conv_last_msg", columnList = "last_message_at")
        }
)
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // conversa associada a processo de adoção (pode ser null para "suporte")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adoption_request_id")
    private AdoptionRequest adoptionRequest;

    // dono da conversa (CLIENT)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    // admin atribuído (opcional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ChatConversationStatus status = ChatConversationStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    // --- getters/setters

    public Long getId() { return id; }

    public AdoptionRequest getAdoptionRequest() { return adoptionRequest; }
    public void setAdoptionRequest(AdoptionRequest adoptionRequest) { this.adoptionRequest = adoptionRequest; }

    public User getClient() { return client; }
    public void setClient(User client) { this.client = client; }

    public User getAdmin() { return admin; }
    public void setAdmin(User admin) { this.admin = admin; }

    public ChatConversationStatus getStatus() { return status; }
    public void setStatus(ChatConversationStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Instant lastMessageAt) { this.lastMessageAt = lastMessageAt; }
}
