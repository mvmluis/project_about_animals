package pt.luis.projectaboutanimals.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pt.luis.projectaboutanimals.model.dto.DonationMoneyForm;
import pt.luis.projectaboutanimals.service.CurrentUserService;
import pt.luis.projectaboutanimals.service.DonationService;

@Controller
@RequestMapping("/donations/money")
public class DonationMoneyController {

    private final DonationService donationService;
    private final CurrentUserService currentUser;

    public DonationMoneyController(DonationService donationService, CurrentUserService currentUser) {
        this.donationService = donationService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String moneyForm(Model model) {
        model.addAttribute("form", new DonationMoneyForm());
        return "donations/money";
    }

    @PostMapping
    public String start(@ModelAttribute("form") @Valid DonationMoneyForm form,
                        BindingResult br,
                        Model model) {
        if (br.hasErrors()) return "donations/money";

        var me = currentUser.requireUser();
        String approveUrl = donationService.startPaypalDonation(me, form.getAmount());
        return "redirect:" + approveUrl;
    }

    @GetMapping("/return")
    public String paypalReturn(@RequestParam(name = "token", required = false) String orderId,
                               @RequestParam(name = "PayerID", required = false) String payerId) {

        // No PayPal Checkout, "token" costuma ser o orderId.
        donationService.handlePaypalReturn(orderId, payerId, orderId);

        return "redirect:/donations/thanks?type=money";
    }

    @GetMapping("/cancel")
    public String paypalCancel(@RequestParam(name = "token", required = false) String orderId) {
        donationService.handlePaypalCancel(orderId);
        return "redirect:/donations?canceled=1";
    }
}
