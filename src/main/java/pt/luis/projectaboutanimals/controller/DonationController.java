package pt.luis.projectaboutanimals.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pt.luis.projectaboutanimals.model.DonationType;
import pt.luis.projectaboutanimals.model.ProductCategory;
import pt.luis.projectaboutanimals.model.dto.DonationChooseForm;
import pt.luis.projectaboutanimals.model.dto.DonationProductForm;
import pt.luis.projectaboutanimals.service.CurrentUserService;
import pt.luis.projectaboutanimals.service.DonationService;

@Controller
@RequestMapping("/donations")
public class DonationController {

    private final DonationService donationService;
    private final CurrentUserService currentUser;

    public DonationController(DonationService donationService, CurrentUserService currentUser) {
        this.donationService = donationService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String choose(Model model) {
        model.addAttribute("form", new DonationChooseForm());
        return "donations/choose";
    }

    @PostMapping
    public String choosePost(@ModelAttribute("form") @Valid DonationChooseForm form,
                             BindingResult br) {
        if (br.hasErrors()) return "donations/choose";

        // ✅ enums PT (sem strings)
        return form.getType() == DonationType.PRODUTOS
                ? "redirect:/donations/products"
                : "redirect:/donations/money";
    }

    @GetMapping("/products")
    public String productsForm(Model model) {
        model.addAttribute("form", new DonationProductForm());
        model.addAttribute("categories", ProductCategory.values());
        return "donations/products";
    }

    @PostMapping("/products")
    public String productsSubmit(@ModelAttribute("form") @Valid DonationProductForm form,
                                 BindingResult br,
                                 Model model) {
        if (br.hasErrors()) {
            model.addAttribute("categories", ProductCategory.values());
            return "donations/products";
        }

        var me = currentUser.requireUser();
        donationService.createProductDonation(
                me,
                form.getCategory(),
                form.getDescription(),
                form.getQuantity(),
                form.getDeliveryNotes()
        );

        return "redirect:/donations/thanks?type=products";
    }

    @GetMapping("/history")
    public String history(Model model) {
        Long meId = currentUser.requireUserId();
        model.addAttribute("items", donationService.myDonations(meId));
        return "donations/history";
    }

    @GetMapping("/thanks")
    public String thanks(@RequestParam(required = false) String type, Model model) {
        model.addAttribute("type", type);
        return "donations/thanks";
    }
}
