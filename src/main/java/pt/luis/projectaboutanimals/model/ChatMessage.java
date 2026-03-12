package pt.luis.projectaboutanimals.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "chat_messages",
        indexes = {
                @Index(name = "ix_chat_msg_conv_id", columnList = "conversation_id"),
                @Index(name = "ix_chat_msg_created", columnList = "created_at"),
                @Index(name = "ix_chat_msg_sender", columnList = "sender_id")
        }
)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_msg_conversation"))
    private ChatConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_msg_sender"))
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_role", nullable = false, length = 16)
    private Role senderRole;

    @Column(name = "body", nullable = false, length = 4000)
    private String body;

    // “lidos”
    @Column(name = "read_at_client")
    private Instant readAtClient;

    @Column(name = "read_at_admin")
    private Instant readAtAdmin;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatAttachment> attachments = new ArrayList<>();

    // --- getters/setters

    public Long getId() { return id; }

    public ChatConversation getConversation() { return conversation; }
    public void setConversation(ChatConversation conversation) { this.conversation = conversation; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public Role getSenderRole() { return senderRole; }
    public void setSenderRole(Role senderRole) { this.senderRole = senderRole; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Instant getReadAtClient() { return readAtClient; }
    public void setReadAtClient(Instant readAtClient) { this.readAtClient = readAtClient; }

    public Instant getReadAtAdmin() { return readAtAdmin; }
    public void setReadAtAdmin(Instant readAtAdmin) { this.readAtAdmin = readAtAdmin; }

    public Instant getCreatedAt() { return createdAt; }

    public List<ChatAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<ChatAttachment> attachments) { this.attachments = attachments; }
}
