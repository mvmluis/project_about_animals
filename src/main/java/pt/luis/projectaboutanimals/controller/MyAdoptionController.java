package pt.luis.projectaboutanimals.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pt.luis.projectaboutanimals.model.User;
import pt.luis.projectaboutanimals.service.AdoptionService;
import pt.luis.projectaboutanimals.service.ReportService;

@Controller
@RequestMapping("/my-adoptions")
public class MyAdoptionController {

    private final AdoptionService adoptionService;
    private final ReportService reports;

    public MyAdoptionController(AdoptionService adoptionService, ReportService reports) {
        this.adoptionService = adoptionService;
        this.reports = reports;
    }

    @GetMapping
    public String list(Authentication auth, Model model) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        // ✅ funciona com Form Login e OAuth2
        User me;
        try {
            me = reports.getCurrentUser(auth);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage());
        }

        model.addAttribute("items", adoptionService.myAdoptions(me.getId()));
        return "adoptions/my-adoptions";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id,
                         Authentication auth,
                         Model model) {

        if (id == null || id < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ID inválido.");
        }

        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        // ✅ funciona com Form Login e OAuth2
        User me;
        try {
            me = reports.getCurrentUser(auth);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage());
        }

        try {
            model.addAttribute("adoption", adoptionService.myAdoptionDetail(id, me.getId()));
            model.addAttribute("events", adoptionService.myAdoptionEvents(id, me.getId()));
            return "adoptions/my-adoption-view";

        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());

        } catch (SecurityException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());

        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao abrir o processo.");
        }
    }
}
