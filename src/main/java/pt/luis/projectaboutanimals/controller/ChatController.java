package pt.luis.projectaboutanimals.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pt.luis.projectaboutanimals.service.ChatService;

@Controller
public class ChatController {

    private final ChatService chat;

    public ChatController(ChatService chat) {
        this.chat = chat;
    }

    @GetMapping("/my-adoptions/{adoptionId}/chat")
    public String chatPage(@PathVariable Long adoptionId, Authentication auth, Model model) {
        var me = chat.getAuthUser(auth.getName());
        var conv = chat.getOrCreateConversationForAdoption(adoptionId, me);

        model.addAttribute("conversationId", conv.getId());
        model.addAttribute("adoptionId", adoptionId);
        return "chat/chat-view";
    }
}
