package pt.luis.projectaboutanimals.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pt.luis.projectaboutanimals.dao.*;
import pt.luis.projectaboutanimals.model.*;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class ChatService {

    private final ChatConversationRepository conversations;
    private final ChatMessageRepository messages;
    private final ChatAttachmentRepository attachments;
    private final AdoptionRequestRepository adoptions;
    private final UserRepository users;
    private final ChatAttachmentStorageService storage;

    public ChatService(ChatConversationRepository conversations,
                       ChatMessageRepository messages,
                       ChatAttachmentRepository attachments,
                       AdoptionRequestRepository adoptions,
                       UserRepository users,
                       ChatAttachmentStorageService storage) {
        this.conversations = conversations;
        this.messages = messages;
        this.attachments = attachments;
        this.adoptions = adoptions;
        this.users = users;
        this.storage = storage;
    }

    // ---------- helpers ----------
    public record AuthUser(Long id, Role role, String email) {}

    public AuthUser getAuthUser(String email) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email inválido.");
        User u = users.findByEmail(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("User não existe."));
        if (u.getRole() == null) throw new IllegalStateException("User sem role.");
        return new AuthUser(u.getId(), u.getRole(), u.getEmail());
    }

    private void assertAdmin(AuthUser me) {
        if (me == null || me.role() != Role.ADMIN) throw new SecurityException("Sem permissão.");
    }

    private void assertClientOrAdmin(AuthUser me) {
        if (me == null || (me.role() != Role.CLIENT && me.role() != Role.ADMIN)) {
            throw new SecurityException("Sem permissão.");
        }
    }

    private void assertCanAccessConversation(AuthUser me, ChatConversation c) {
        assertClientOrAdmin(me);

        if (me.role() == Role.ADMIN) {
            Long adminId = (c.getAdmin() != null) ? c.getAdmin().getId() : null;
            if (adminId != null && !Objects.equals(adminId, me.id())) {
                throw new SecurityException("Sem permissão.");
            }
            return;
        }

        Long clientId = (c.getClient() != null) ? c.getClient().getId() : null;
        if (clientId == null || !Objects.equals(clientId, me.id())) {
            throw new SecurityException("Sem permissão.");
        }
    }

    // ---------- conversation ----------
    @Transactional
    public ChatConversation getOrCreateConversationForAdoption(Long adoptionId, AuthUser me) {
        assertClientOrAdmin(me);
        if (adoptionId == null) throw new IllegalArgumentException("adoptionId obrigatório.");

        AdoptionRequest ar = adoptions.findByIdWithReportAndApplicant(adoptionId)
                .orElseThrow(() -> new IllegalArgumentException("Processo de adoção não existe."));

        if (me.role() == Role.CLIENT) {
            if (ar.getApplicant() == null || !Objects.equals(ar.getApplicant().getId(), me.id())) {
                throw new SecurityException("Sem permissão.");
            }
        }

        return conversations.findByAdoptionIdWithRefs(adoptionId).orElseGet(() -> {
            ChatConversation c = new ChatConversation();
            c.setAdoptionRequest(ar);
            c.setClient(ar.getApplicant());
            c.setStatus(ChatConversationStatus.OPEN);
            c.setCreatedAt(Instant.now());
            c.setLastMessageAt(null);
            return conversations.save(c);
        });
    }

    @Transactional
    public ChatConversation getOrCreateSupportConversation(AuthUser me) {
        assertClientOrAdmin(me);

        if (me.role() != Role.CLIENT) {
            throw new IllegalStateException(
                    "Endpoint/admin a chamar método de suporte do CLIENT. Use getOrCreateSupportConversationForClient(clientId, me)."
            );
        }

        User client = users.findById(me.id())
                .orElseThrow(() -> new IllegalArgumentException("User não existe."));

        return conversations.findSupportByClientIdWithRefs(client.getId()).orElseGet(() -> {
            ChatConversation c = new ChatConversation();
            c.setAdoptionRequest(null);
            c.setClient(client);
            c.setAdmin(null);
            c.setStatus(ChatConversationStatus.OPEN);
            c.setCreatedAt(Instant.now());
            c.setLastMessageAt(null);
            return conversations.save(c);
        });
    }

    @Transactional
    public ChatConversation getOrCreateSupportConversationForClient(Long clientId, AuthUser me) {
        assertAdmin(me);
        if (clientId == null) throw new IllegalArgumentException("clientId obrigatório.");

        User client = users.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não existe."));

        ChatConversation c = conversations.findSupportByClientIdWithRefs(client.getId()).orElseGet(() -> {
            ChatConversation nc = new ChatConversation();
            nc.setAdoptionRequest(null);
            nc.setClient(client);
            nc.setAdmin(null);
            nc.setStatus(ChatConversationStatus.OPEN);
            nc.setCreatedAt(Instant.now());
            nc.setLastMessageAt(null);
            return conversations.save(nc);
        });

        if (c.getAdmin() == null) {
            User admin = users.findById(me.id()).orElseThrow(() -> new IllegalArgumentException("Admin não existe."));
            c.setAdmin(admin);
            c = conversations.save(c);
        } else if (!Objects.equals(c.getAdmin().getId(), me.id())) {
            throw new SecurityException("Sem permissão.");
        }

        return c;
    }

    @Transactional(readOnly = true)
    public ChatConversation getConversationById(Long conversationId, AuthUser me) {
        if (conversationId == null) throw new IllegalArgumentException("conversationId obrigatório.");

        ChatConversation c = conversations.findByIdWithRefs(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não existe."));
        assertCanAccessConversation(me, c);
        return c;
    }

    @Transactional(readOnly = true)
    public List<ChatConversation> adminInbox(AuthUser me) {
        assertAdmin(me);
        return conversations.findAllForAdminInbox();
    }

    @Transactional
    public void adminAssign(Long conversationId, AuthUser me) {
        assertAdmin(me);

        ChatConversation c = conversations.findByIdWithRefs(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversa não existe."));

        User admin = users.findById(me.id())
                .orElseThrow(() -> new IllegalArgumentException("User não existe"));

        if (c.getAdmin() != null && !Objects.equals(c.getAdmin().getId(), admin.getId())) {
            throw new SecurityException("Sem permissão.");
        }

        c.setAdmin(admin);
        conversations.save(c);
    }

    // ---------- messages ----------
    @Transactional(readOnly = true)
    public List<ChatMessage> listMessages(Long conversationId, Long afterId, AuthUser me) {
        ChatConversation c = getConversationById(conversationId, me);

        if (afterId == null || afterId < 1) {
            return messages.findAllByConversationWithSenderAndAttachments(c.getId());
        }
        return messages.findAfterId(c.getId(), afterId);
    }

    @Transactional
    public ChatMessage sendMessage(Long conversationId, String body, List<MultipartFile> files, AuthUser me) {
        ChatConversation c = getConversationById(conversationId, me);

        if (c.getStatus() != ChatConversationStatus.OPEN) {
            throw new IllegalStateException("Conversa fechada.");
        }

        String text = (body == null) ? "" : body.trim();
        boolean hasFiles = files != null && files.stream().anyMatch(f -> f != null && !f.isEmpty());
        if (text.isBlank() && !hasFiles) {
            throw new IllegalArgumentException("Mensagem vazia.");
        }

        if (me.role() == Role.ADMIN) {
            if (c.getAdmin() == null) {
                User admin = users.findById(me.id()).orElseThrow(() -> new IllegalArgumentException("User não existe"));
                c.setAdmin(admin);
                conversations.save(c);
            } else if (!Objects.equals(c.getAdmin().getId(), me.id())) {
                throw new SecurityException("Sem permissão.");
            }
        }

        User sender = users.findById(me.id())
                .orElseThrow(() -> new IllegalArgumentException("User não existe"));

        ChatMessage m = new ChatMessage();
        m.setConversation(c);
        m.setSender(sender);
        m.setSenderRole(sender.getRole());
        m.setBody(text.isBlank() ? "(anexo)" : text);

        if (me.role() == Role.CLIENT) m.setReadAtClient(Instant.now());
        if (me.role() == Role.ADMIN)  m.setReadAtAdmin(Instant.now());

        ChatMessage saved = messages.save(m);

        if (hasFiles) {
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;

                try {
                    var stored = storage.store(f);

                    ChatAttachment a = new ChatAttachment();
                    a.setMessage(saved);
                    a.setOriginalName(stored.originalName());
                    a.setContentType(stored.contentType());
                    a.setSizeBytes(stored.sizeBytes());
                    a.setStorageKey(stored.storageKey());

                    attachments.save(a);

                } catch (IOException io) {
                    throw new RuntimeException("Falha ao guardar anexo: " + io.getMessage(), io);
                }
            }
        }

        c.setLastMessageAt(Instant.now());
        conversations.save(c);

        return messages.findByIdWithSenderAndAttachments(saved.getId())
                .orElseThrow(() -> new IllegalStateException("Mensagem acabou de ser criada mas não foi encontrada."));
    }

    /**
     * Compilável com os teus repositories atuais.
     * Nota: apaga anexos primeiro (FK), depois mensagens.
     */
    @Transactional
    public void clearConversation(Long conversationId, AuthUser me) {
        ChatConversation c = getConversationById(conversationId, me);

        attachments.deleteAllByConversationId(c.getId());
        messages.deleteAllByConversationId(c.getId());

        c.setLastMessageAt(null);
        conversations.save(c);
    }

    @Transactional(readOnly = true)
    public ChatAttachment getAttachmentForDownload(Long attachmentId, AuthUser me) {
        ChatAttachment a = attachments.findByIdWithMessageAndConversation(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Anexo não existe."));
        ChatConversation c = a.getMessage().getConversation();
        assertCanAccessConversation(me, c);
        return a;
    }

    @Transactional
    public int markConversationAsRead(Long conversationId, AuthUser me) {
        var c = getConversationById(conversationId, me);

        if (me.role() == Role.ADMIN) {
            return messages.markReadByAdmin(c.getId());
        }
        if (me.role() == Role.CLIENT) {
            return messages.markReadByClient(c.getId());
        }
        throw new SecurityException("Sem permissão.");
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long conversationId, AuthUser me) {
        var c = getConversationById(conversationId, me);

        Role fromRole = (me.role() == Role.ADMIN) ? Role.CLIENT : Role.ADMIN;
        return messages.countUnread(c.getId(), fromRole, me.role());
    }

    @Transactional(readOnly = true)
    public long adminUnreadTotal(AuthUser me) {
        assertAdmin(me);
        return messages.countUnreadForAdmin(me.id());
    }
}
