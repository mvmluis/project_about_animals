package pt.luis.projectaboutanimals.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "chat_attachments",
        indexes = { @Index(name = "ix_chat_att_msg", columnList = "message_id") }
)
public class ChatAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @Column(name = "original_name", nullable = false, length = 240)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_key", nullable = false, length = 300)
    private String storageKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public ChatMessage getMessage() { return message; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getStorageKey() { return storageKey; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setMessage(ChatMessage message) { this.message = message; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
