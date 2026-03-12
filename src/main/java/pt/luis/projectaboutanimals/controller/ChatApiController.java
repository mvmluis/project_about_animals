package pt.luis.projectaboutanimals.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pt.luis.projectaboutanimals.controller.dto.ChatDtos;
import pt.luis.projectaboutanimals.service.ChatAttachmentStorageService;
import pt.luis.projectaboutanimals.service.ChatService;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    private final ChatService chat;
    private final ChatAttachmentStorageService storage;

    public ChatApiController(ChatService chat, ChatAttachmentStorageService storage) {
        this.chat = chat;
        this.storage = storage;
    }

    // ✅ resolve o email correto para ambos os logins:
    // - form login: auth.getName() (normalmente o teu username/email)
    // - google oauth2: claim "email" do principal
    private String resolveEmail(Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken token) {
            String email = token.getPrincipal().getAttribute("email");
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("Email não disponível no principal OAuth2.");
            }
            return email.trim();
        }
        return auth.getName();
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public List<ChatDtos.MessageDto> list(@PathVariable Long conversationId,
                                          @RequestParam(name = "afterId", required = false) Long afterId,
                                          Authentication auth) {

        var me = chat.getAuthUser(resolveEmail(auth));
        var msgs = chat.listMessages(conversationId, afterId, me);
        return msgs.stream().map(m -> ChatDtos.toDto(m, me.id())).toList();
    }

    @PostMapping(value = "/conversations/{conversationId}/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatDtos.MessageDto send(@PathVariable Long conversationId,
                                    @RequestParam(name = "body", required = false) String body,
                                    @RequestParam(name = "files", required = false) List<MultipartFile> files,
                                    Authentication auth) {

        var me = chat.getAuthUser(resolveEmail(auth));
        var saved = chat.sendMessage(conversationId, body, files, me);
        return ChatDtos.toDto(saved, me.id());
    }

    @GetMapping("/attachments/{attachmentId}/download")
    public ResponseEntity<FileSystemResource> download(@PathVariable Long attachmentId, Authentication auth) {
        var me = chat.getAuthUser(resolveEmail(auth));
        var a = chat.getAttachmentForDownload(attachmentId, me);

        Path file = storage.resolve(a.getStorageKey());
        FileSystemResource res = new FileSystemResource(file.toFile());

        if (!res.exists()) return ResponseEntity.notFound().build();

        String safeName = (a.getOriginalName() == null) ? "anexo" : a.getOriginalName().replace("\"", "");
        MediaType ct = (a.getContentType() == null || a.getContentType().isBlank())
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(a.getContentType());

        return ResponseEntity.ok()
                .contentType(ct)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeName + "\"")
                .contentLength(a.getSizeBytes())
                .body(res);
    }

    // conversa de suporte individual por utilizador autenticado
    @GetMapping("/global")
    public Map<String, Long> global(Authentication auth) {
        var me = chat.getAuthUser(resolveEmail(auth));
        var conv = chat.getOrCreateSupportConversation(me);
        return Map.of("id", conv.getId());
    }

    @PostMapping("/conversations/{conversationId}/clear")
    public ResponseEntity<Void> clearPost(@PathVariable Long conversationId, Authentication auth) {
        var me = chat.getAuthUser(resolveEmail(auth));
        chat.clearConversation(conversationId, me);
        return ResponseEntity.noContent().build();
    }
}
