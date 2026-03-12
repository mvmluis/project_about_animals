package pt.luis.projectaboutanimals.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import pt.luis.projectaboutanimals.model.Donation;
import pt.luis.projectaboutanimals.model.DonationStatus;
import pt.luis.projectaboutanimals.model.DonationType;
import pt.luis.projectaboutanimals.model.dto.AdminDonationUpdateForm;
import pt.luis.projectaboutanimals.service.DonationAdminService;

@Controller
@RequestMapping("/admin/donations")
public class AdminDonationsController {

    private final DonationAdminService adminService;

    public AdminDonationsController(DonationAdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) DonationType type,
                        @RequestParam(required = false) DonationStatus status,
                        Model model) {

        model.addAttribute("items", adminService.list(type, status));
        model.addAttribute("types", DonationType.values());
        model.addAttribute("statuses", DonationStatus.values());
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedStatus", status);

        return "admin/donations/index";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        Donation d = adminService.get(id);
        model.addAttribute("d", d);

        AdminDonationUpdateForm form = new AdminDonationUpdateForm();
        form.setStatus(d.getStatus());
        form.setAdminNotes(d.getAdminNotes());
        model.addAttribute("form", form);

        // ⚠️ Atenção: estes estados têm de existir no teu enum DonationStatus.
        model.addAttribute("productStatuses", new DonationStatus[]{
                DonationStatus.SUBMETIDA,
                DonationStatus.CANCELADA
                // DonationStatus.EM_TRIAGEM,
                // DonationStatus.RECEBIDA
        });

        return "admin/donations/show";
    }

    @PostMapping("/{id}/products/update")
    public String updateProducts(@PathVariable Long id,
                                 @ModelAttribute("form") @Valid AdminDonationUpdateForm form,
                                 BindingResult br,
                                 Model model) {

        Donation d = adminService.get(id);

        if (br.hasErrors()) {
            model.addAttribute("d", d);
            model.addAttribute("productStatuses", new DonationStatus[]{
                    DonationStatus.SUBMETIDA,
                    DonationStatus.CANCELADA
                    // DonationStatus.EM_TRIAGEM,
                    // DonationStatus.RECEBIDA
            });
            return "admin/donations/show";
        }

        adminService.updateProductHandling(id, form.getStatus(), form.getAdminNotes());
        return "redirect:/admin/donations/" + id + "?updated=1";
    }
}
