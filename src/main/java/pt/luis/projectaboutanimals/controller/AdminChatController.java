package pt.luis.projectaboutanimals.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pt.luis.projectaboutanimals.dao.AdoptionRequestRepository;
import pt.luis.projectaboutanimals.model.AdoptionStatus;
import pt.luis.projectaboutanimals.model.ChatAttachment;
import pt.luis.projectaboutanimals.model.ChatConversation;
import pt.luis.projectaboutanimals.model.ChatMessage;
import pt.luis.projectaboutanimals.service.ChatService;
import pt.luis.projectaboutanimals.service.ReportService;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/admin/chats")
public class AdminChatController {

    private final ChatService chat;
    private final NamedParameterJdbcTemplate jdbc;

    // ✅ para manter reports + adoptions SEMPRE na view
    private final ReportService reports;
    private final AdoptionRequestRepository adoptions;

    public AdminChatController(ChatService chat,
                               NamedParameterJdbcTemplate jdbc,
                               ReportService reports,
                               AdoptionRequestRepository adoptions) {
        this.chat = chat;
        this.jdbc = jdbc;
        this.reports = reports;
        this.adoptions = adoptions;
    }

    // ---------- VIEW MODELS ----------
    public record ThreadVm(Long id, String userName, String userEmail, String lastText, String lastAt, Integer unreadCount) {}
    public record MsgVm(String fromRole, String text, String createdAt, String attachmentUrl, String attachmentName) {}

    // Resolve email consistente para form login e OAuth2
    private String resolveEmail(Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken token) {
            String email = token.getPrincipal().getAttribute("email");
            if (email == null || email.isBlank()) throw new IllegalArgumentException("Email não disponível no OAuth2.");
            return email.trim();
        }
        return auth.getName();
    }

    // ---------- VIEW ----------
    @GetMapping
    public String inbox(@RequestParam(name = "chatId", required = false) Long chatId,
                        Authentication auth,
                        Model model) {

        // ✅ mantém reports + adoptions SEMPRE (para não desaparecerem após POST/redirect)
        model.addAttribute("items", reports.allReports());
        model.addAttribute("adoptionItems", adoptions.findAllWithReportAndApplicantOrderByCreatedAtDesc());
        model.addAttribute("adoptionStatuses", AdoptionStatus.values());

        var me = chat.getAuthUser(resolveEmail(auth));

        // 1) threads (lista esquerda)
        List<ChatConversation> conversations = chat.adminInbox(me);

        // unread por conversa (1 query)
        Map<Long, Integer> unreadMap = new HashMap<>();
        List<Long> ids = conversations.stream().map(ChatConversation::getId).toList();

        if (!ids.isEmpty()) {
            String sql = """
                SELECT conversation_id, COUNT(*) AS cnt
                FROM chat_messages
                WHERE conversation_id IN (:ids)
                  AND sender_role = 'CLIENT'
                  AND read_at_admin IS NULL
                GROUP BY conversation_id
            """;

            var params = new MapSqlParameterSource().addValue("ids", ids);

            jdbc.query(sql, params, rs -> {
                long convId = rs.getLong("conversation_id");
                int cnt = rs.getInt("cnt");
                unreadMap.put(convId, cnt);
            });
        }

        List<ThreadVm> threads = new ArrayList<>();
        for (ChatConversation c : conversations) {
            String userName  = (c.getClient() != null) ? safe(c.getClient().getName()) : "Utilizador";
            String userEmail = (c.getClient() != null) ? safe(c.getClient().getEmail()) : "";

            String lastAt = formatInstantOrBlank(
                    c.getLastMessageAt() != null ? c.getLastMessageAt() : c.getCreatedAt()
            );

            Integer unread = unreadMap.getOrDefault(c.getId(), 0);

            threads.add(new ThreadVm(
                    c.getId(),
                    userName.isBlank() ? "Utilizador" : userName,
                    userEmail,
                    "—",
                    lastAt,
                    unread
            ));
        }

        model.addAttribute("chatThreads", threads);

        // 2) chat ativo
        if (chatId != null) {
            ChatConversation conv = chat.getConversationById(chatId, me);

            // marcar como lidas ao abrir (BD)
            String markSql = """
                UPDATE chat_messages
                SET read_at_admin = :now
                WHERE conversation_id = :chatId
                  AND sender_role = 'CLIENT'
                  AND read_at_admin IS NULL
            """;
            var markParams = new MapSqlParameterSource()
                    .addValue("now", Timestamp.from(Instant.now()))
                    .addValue("chatId", conv.getId());
            jdbc.update(markSql, markParams);

            model.addAttribute("activeChatId", conv.getId());
            model.addAttribute("activeChatUserName",
                    conv.getClient() != null ? safe(conv.getClient().getName()) : "Utilizador");
            model.addAttribute("activeChatUserEmail",
                    conv.getClient() != null ? safe(conv.getClient().getEmail()) : "");

            Long adoptionId = (conv.getAdoptionRequest() != null) ? conv.getAdoptionRequest().getId() : null;
            model.addAttribute("activeChatAdoptionId", adoptionId);

            List<ChatMessage> msgs = chat.listMessages(conv.getId(), null, me);

            List<MsgVm> vm = new ArrayList<>();
            for (ChatMessage m : msgs) {
                String role = (m.getSenderRole() != null) ? m.getSenderRole().name() : "CLIENT";
                String text = safe(m.getBody());
                String when = formatInstantOrBlank(m.getCreatedAt());

                String attUrl = null;
                String attName = null;

                var atts = m.getAttachments();
                if (atts != null && !atts.isEmpty()) {
                    ChatAttachment a = atts.get(0);
                    attUrl = "/api/chat/attachments/" + a.getId() + "/download";
                    attName = safe(a.getOriginalName());
                }

                vm.add(new MsgVm(role, text, when, attUrl, attName));
            }

            model.addAttribute("chatMessages", vm);
        }

        // ✅ continua a reutilizar a view
        return "admin/reports";
    }

    // ---------- ACTIONS ----------
    @PostMapping("/{chatId}/messages")
    public String send(@PathVariable Long chatId,
                       @RequestParam(name = "text", required = false) String text,
                       @RequestParam(name = "file", required = false) MultipartFile file,
                       Authentication auth) {

        var me = chat.getAuthUser(resolveEmail(auth));

        List<MultipartFile> files = (file != null && !file.isEmpty()) ? List.of(file) : List.of();
        chat.sendMessage(chatId, text, files, me);

        // ✅ Opção 1: volta sempre à inbox /admin/chats (e a view terá reports/adoptions carregados)
        return "redirect:/admin/chats?chatId=" + chatId;
    }

    @PostMapping("/{chatId}/assign")
    public String assign(@PathVariable Long chatId, Authentication auth) {
        var me = chat.getAuthUser(resolveEmail(auth));
        chat.adminAssign(chatId, me);
        return "redirect:/admin/chats?chatId=" + chatId;
    }

    // ---------- UNREAD TOTAL ----------
    @GetMapping(value = "/unread-total", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Long>> unreadTotal(Authentication auth) {
        String sql = """
            SELECT COUNT(*) AS cnt
            FROM chat_messages
            WHERE sender_role = 'CLIENT'
              AND read_at_admin IS NULL
        """;

        Long total = jdbc.queryForObject(sql, new MapSqlParameterSource(), Long.class);
        if (total == null) total = 0L;

        Map<String, Long> body = new HashMap<>();
        body.put("count", total);
        return ResponseEntity.ok(body);
    }

    // ---------- helpers ----------
    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    private static String formatInstantOrBlank(Instant i) {
        if (i == null) return "";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(i);
    }
}