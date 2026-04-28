package pt.luis.projectaboutanimals.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pt.luis.projectaboutanimals.dao.AdoptionRequestRepository;
import pt.luis.projectaboutanimals.model.ReportStatus;
import pt.luis.projectaboutanimals.service.ChatService;
import pt.luis.projectaboutanimals.service.ReportService;

@Controller
@RequestMapping("/admin")
public class AdminReportController {

    private final ReportService reports;
    private final AdoptionRequestRepository adoptions;
    private final ChatService chat;

    public AdminReportController(ReportService reports,
                                 AdoptionRequestRepository adoptions,
                                 ChatService chat) {
        this.reports = reports;
        this.adoptions = adoptions;
        this.chat = chat;
    }

    private String resolveEmail(Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken token) {
            String email = token.getPrincipal().getAttribute("email");
            if (email == null || email.isBlank()) throw new IllegalArgumentException("Email não disponível no OAuth2.");
            return email.trim();
        }
        return auth.getName();
    }

    // ✅ compatibilidade: quem ainda abrir /admin/reports vai para a inbox /admin/chats
    @GetMapping("/reports")
    public String reportsHomeCompat(@RequestParam(name = "chatId", required = false) Long chatId) {
        if (chatId != null) return "redirect:/admin/chats?chatId=" + chatId;
        return "redirect:/admin/chats";
    }

    @GetMapping("/reports/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("report", reports.get(id));
        model.addAttribute("statuses", ReportStatus.values());
        return "admin/report-view";
    }

    @PostMapping("/reports/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam ReportStatus status) {
        reports.adminChangeStatus(id, status);
        return "redirect:/admin/reports/" + id;
    }

    @GetMapping("/reports/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var r = reports.get(id);

        var f = new pt.luis.projectaboutanimals.model.ReportForm();
        f.setTitle(r.getTitle());
        f.setSpecies(r.getSpecies());
        f.setBreed(r.getBreed());
        f.setColor(r.getColor());
        f.setSize(r.getSize());
        f.setApproxAge(r.getApproxAge());
        f.setFoundAt(r.getFoundAt());
        f.setLocationText(r.getLocationText());
        f.setNotes(r.getNotes());

        model.addAttribute("form", f);
        model.addAttribute("isEdit", true);
        model.addAttribute("id", id);

        return "reports/report-form";
    }

    @PostMapping("/reports/{id}/edit")
    public String update(@PathVariable Long id,
                         @ModelAttribute("form") @jakarta.validation.Valid pt.luis.projectaboutanimals.model.ReportForm form,
                         org.springframework.validation.BindingResult br,
                         Model model) {

        if (br.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("id", id);
            return "reports/report-form";
        }

        var data = new pt.luis.projectaboutanimals.model.FoundAnimalReport();
        data.setTitle(form.getTitle());
        data.setSpecies(form.getSpecies());
        data.setBreed(form.getBreed());
        data.setColor(form.getColor());
        data.setSize(form.getSize());
        data.setApproxAge(form.getApproxAge());
        data.setFoundAt(form.getFoundAt());
        data.setLocationText(form.getLocationText());
        data.setNotes(form.getNotes());

        reports.adminUpdate(id, data);

        return "redirect:/admin/reports/" + id;
    }

    @PostMapping("/reports/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            reports.adminDelete(id);
            ra.addFlashAttribute("success", "Report e pedidos associados apagados com sucesso.");
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Falha ao apagar o report.");
        }
        return "redirect:/admin/chats";
    }
}