package pt.luis.projectaboutanimals.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pt.luis.projectaboutanimals.model.*;
import pt.luis.projectaboutanimals.dao.AdoptionRequestRepository;
import pt.luis.projectaboutanimals.service.ReportService;
import pt.luis.projectaboutanimals.service.UploadService;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;

@Controller
public class ReportController {

    private final ReportService reports;
    private final UploadService uploadService;

    // ✅ bloquear "Adotar" quando já existe processo ativo
    private final AdoptionRequestRepository pedidosAdocao;

    public ReportController(ReportService reports,
                            UploadService uploadService,
                            AdoptionRequestRepository pedidosAdocao) {
        this.reports = reports;
        this.uploadService = uploadService;
        this.pedidosAdocao = pedidosAdocao;
    }

    // ✅ EMAIL CERTO (Form login vs Google OAuth2)
    private String currentEmail(Authentication auth) {
        if (auth instanceof OAuth2AuthenticationToken oat) {
            OAuth2User u = oat.getPrincipal();
            String email = u.getAttribute("email");
            if (email != null && !email.isBlank()) return email;
        }
        return auth.getName(); // fallback para form-login
    }

    @InitBinder("form")
    public void initBinder(WebDataBinder binder) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        ZoneId zone = ZoneId.of("Europe/Lisbon");

        binder.registerCustomEditor(Instant.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                if (text == null || text.isBlank()) { setValue(null); return; }
                LocalDateTime ldt = LocalDateTime.parse(text.trim(), fmt);
                setValue(ldt.atZone(zone).toInstant());
            }
        });
    }

    @GetMapping("/")
    public String raiz() {
        return "redirect:/reports";
    }

    // ✅ LISTA GLOBAL: todos veem tudo
    @GetMapping("/reports")
    public String listarTodosReports(Authentication auth, Model model) {
        User eu = reports.getUserByEmail(currentEmail(auth));

        model.addAttribute("items", reports.allReportsForList());
        model.addAttribute("meId", eu.getId());

        // ✅ IDs de report com processo ativo (NOT IN nos estados finais/excluídos)
        var ids = pedidosAdocao.findReportIdsWithActiveProcess(
                List.of(AdoptionStatus.REJEITADO, AdoptionStatus.CANCELADO)
        );
        model.addAttribute("blockedReportIds", new HashSet<>(ids));

        return "reports/my-reports";
    }

    @GetMapping("/my-reports")
    public String listarMeusReports(Authentication auth, Model model) {
        User eu = reports.getUserByEmail(currentEmail(auth));
        model.addAttribute("items", reports.myReports(eu));
        model.addAttribute("meId", eu.getId());
        return "reports/my-reports";
    }

    @GetMapping("/reports/new")
    public String novoFormulario(Model model) {
        model.addAttribute("form", new ReportForm());
        model.addAttribute("isEdit", false);
        return "reports/report-form";
    }

    @PostMapping("/reports")
    public String criarReport(Authentication auth,
                              @ModelAttribute("form") @Valid ReportForm form,
                              BindingResult br,
                              @RequestParam(name = "photo", required = false) MultipartFile photo,
                              Model model) {

        if (br.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "reports/report-form";
        }

        User eu = reports.getUserByEmail(currentEmail(auth));

        FoundAnimalReport r = new FoundAnimalReport();
        r.setTitle(form.getTitle());
        r.setSpecies(form.getSpecies());
        r.setBreed(form.getBreed());
        r.setColor(form.getColor());
        r.setSize(form.getSize());
        r.setApproxAge(form.getApproxAge());
        r.setFoundAt(form.getFoundAt());
        r.setLocationText(form.getLocationText());
        r.setNotes(form.getNotes());

        try {
            String photoUrl = uploadService.saveImage(photo);
            if (photoUrl != null) r.setPhotoUrl(photoUrl);
        } catch (IOException e) {
            model.addAttribute("isEdit", false);
            model.addAttribute("error", "Falha ao guardar a foto: " + e.getMessage());
            return "reports/report-form";
        }

        reports.create(eu, r);
        return "redirect:/reports";
    }

    @GetMapping("/reports/{id}")
    public String ver(@PathVariable Long id, Authentication auth, Model model) {
        User eu = reports.getUserByEmail(currentEmail(auth));
        model.addAttribute("report", reports.get(id));
        model.addAttribute("meId", eu.getId());
        return "reports/report-view";
    }

    @GetMapping("/reports/{id}/edit")
    public String editarFormulario(@PathVariable Long id, Authentication auth, Model model) {
        User eu = reports.getUserByEmail(currentEmail(auth));
        FoundAnimalReport r = reports.getForClient(eu, id);

        // ✅ só edita enquanto está PENDENTE
        if (r.getStatus() != ReportStatus.PENDENTE) return "redirect:/reports";

        ReportForm f = new ReportForm();
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

    @PostMapping("/reports/{id}")
    public String atualizar(@PathVariable Long id,
                            Authentication auth,
                            @ModelAttribute("form") @Valid ReportForm form,
                            BindingResult br,
                            @RequestParam(name = "photo", required = false) MultipartFile photo,
                            Model model) {

        if (br.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("id", id);
            return "reports/report-form";
        }

        User eu = reports.getUserByEmail(currentEmail(auth));

        FoundAnimalReport data = new FoundAnimalReport();
        data.setTitle(form.getTitle());
        data.setSpecies(form.getSpecies());
        data.setBreed(form.getBreed());
        data.setColor(form.getColor());
        data.setSize(form.getSize());
        data.setApproxAge(form.getApproxAge());
        data.setFoundAt(form.getFoundAt());
        data.setLocationText(form.getLocationText());
        data.setNotes(form.getNotes());

        try {
            String photoUrl = uploadService.saveImage(photo);
            if (photoUrl != null) data.setPhotoUrl(photoUrl);
        } catch (IOException e) {
            model.addAttribute("isEdit", true);
            model.addAttribute("id", id);
            model.addAttribute("error", "Falha ao guardar a foto: " + e.getMessage());
            return "reports/report-form";
        }

        reports.updateByClient(eu, id, data);
        return "redirect:/reports";
    }

    @PostMapping("/reports/{id}/delete")
    public String apagar(@PathVariable Long id, Authentication auth) {
        User eu = reports.getUserByEmail(currentEmail(auth));
        reports.deleteByClient(eu, id);
        return "redirect:/reports";
    }

    // ----------------- ADOÇÃO -----------------

    @GetMapping("/reports/{id}/adopt")
    public String formularioAdocao(@PathVariable Long id,
                                   Authentication auth,
                                   Model model,
                                   org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {

        User eu = reports.getUserByEmail(currentEmail(auth));
        FoundAnimalReport r = reports.get(id);

        // ✅ só pode adotar se estiver APROVADO
        if (r.getStatus() == null || r.getStatus() != ReportStatus.APROVADO) {
            ra.addFlashAttribute("error", "Este registo não está disponível para adoção.");
            return "redirect:/reports/" + id;
        }

        // ✅ autor não pode adotar o próprio report
        if (r.getCreatedBy() != null && r.getCreatedBy().getId() != null
                && r.getCreatedBy().getId().equals(eu.getId())) {
            ra.addFlashAttribute("error", "Não podes pedir adoção do teu próprio registo.");
            return "redirect:/reports/" + id;
        }

        // ✅ bloquear se já há processo ativo no report
        var bloqueados = pedidosAdocao.findReportIdsWithActiveProcess(
                List.of(AdoptionStatus.REJEITADO, AdoptionStatus.CANCELADO, AdoptionStatus.ADOTADO)
        );
        if (bloqueados != null && bloqueados.contains(id)) {
            ra.addFlashAttribute("error", "Este report já tem um processo de adoção em curso.");
            return "redirect:/reports/" + id;
        }

        AdoptionForm f = new AdoptionForm();
        f.setFullName(eu.getName());
        f.setEmail(eu.getEmail());

        model.addAttribute("report", r);
        model.addAttribute("form", f);
        return "reports/adoption-form";
    }

    @PostMapping("/reports/{id}/adopt")
    public String submeterAdocao(@PathVariable Long id,
                                 Authentication auth,
                                 @ModelAttribute("form") @Valid AdoptionForm form,
                                 BindingResult br,
                                 Model model,
                                 org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {

        User eu = reports.getUserByEmail(currentEmail(auth));
        FoundAnimalReport r = reports.get(id);

        // ✅ validações de segurança também no POST
        if (r.getStatus() == null || r.getStatus() != ReportStatus.APROVADO) {
            ra.addFlashAttribute("error", "Este registo não está disponível para adoção.");
            return "redirect:/reports/" + id;
        }

        if (r.getCreatedBy() != null && r.getCreatedBy().getId() != null
                && r.getCreatedBy().getId().equals(eu.getId())) {
            ra.addFlashAttribute("error", "Não podes pedir adoção do teu próprio registo.");
            return "redirect:/reports/" + id;
        }

        var bloqueados = pedidosAdocao.findReportIdsWithActiveProcess(
                List.of(AdoptionStatus.REJEITADO, AdoptionStatus.CANCELADO, AdoptionStatus.ADOTADO)
        );
        if (bloqueados != null && bloqueados.contains(id)) {
            ra.addFlashAttribute("error", "Este report já tem um processo de adoção em curso.");
            return "redirect:/reports/" + id;
        }

        if (br.hasErrors()) {
            model.addAttribute("report", r);
            return "reports/adoption-form";
        }

        try {
            reports.createAdoptionRequest(eu, id, form);
        } catch (RuntimeException ex) {
            model.addAttribute("report", r);
            model.addAttribute("error", ex.getMessage());
            return "reports/adoption-form";
        }

        ra.addFlashAttribute("success",
                "Pedido de adoção enviado. Vamos analisar e entraremos em contacto em breve.");

        return "redirect:/reports/" + id;
    }
}
