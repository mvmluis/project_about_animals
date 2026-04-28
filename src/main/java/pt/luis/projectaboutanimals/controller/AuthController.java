package pt.luis.projectaboutanimals.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pt.luis.projectaboutanimals.service.UserService;

@Controller
public class AuthController {

    private final UserService users;

    public AuthController(UserService users) {
        this.users = users;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    // DTO de registo com JavaBeans getters/setters (necessário para binding robusto)
    public static class RegisterForm {

        @NotBlank
        @Size(max = 120)
        private String name;

        @NotBlank
        @Email
        private String email;

        @NotBlank
        @Size(min = 6, max = 72)
        private String password;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String registerPost(@ModelAttribute("form") @Valid RegisterForm form,
                               BindingResult br,
                               Model model) {
        if (br.hasErrors()) return "register";

        try {
            users.registerClient(form.getName(), form.getEmail(), form.getPassword());
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "register";
        }

        return "redirect:/login?registered";
    }
}
