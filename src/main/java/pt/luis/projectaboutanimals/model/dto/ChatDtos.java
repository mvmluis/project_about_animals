package pt.luis.projectaboutanimals.controller.dto;

import pt.luis.projectaboutanimals.model.ChatMessage;

import java.time.Instant;
import java.util.List;

public class ChatDtos {

    public record AttachmentDto(Long id, String name, String contentType, long sizeBytes, String downloadUrl) {}

    public record MessageDto(
            Long id,
            String senderName,
            String senderRole,
            boolean mine,
            String body,
            Instant createdAt,
            List<AttachmentDto> attachments
    ) {}

    public static MessageDto toDto(ChatMessage m, Long myUserId) {
        boolean mine = m.getSender() != null
                && m.getSender().getId() != null
                && m.getSender().getId().equals(myUserId);

        List<AttachmentDto> att = (m.getAttachments() == null) ? List.of() :
                m.getAttachments().stream().map(a ->
                        new AttachmentDto(
                                a.getId(),
                                safe(a.getOriginalName()),
                                safe(a.getContentType()),
                                a.getSizeBytes(),
                                "/api/chat/attachments/" + a.getId() + "/download"
                        )
                ).toList();

        String senderName = (m.getSender() == null) ? "Utilizador" : safe(m.getSender().getName());
        String senderRole = (m.getSenderRole() == null) ? "CLIENT" : m.getSenderRole().name();

        return new MessageDto(
                m.getId(),
                senderName,
                senderRole,
                mine,
                safe(m.getBody()),
                m.getCreatedAt(),
                att
        );
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }
}
