package pt.luis.projectaboutanimals.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import pt.luis.projectaboutanimals.model.AdoptionStatus;
import pt.luis.projectaboutanimals.service.AdoptionService;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Controller
@RequestMapping("/admin/adoptions")
public class AdminAdoptionController {

    private final AdoptionService adoptionService;

    public AdminAdoptionController(AdoptionService adoptionService) {
        this.adoptionService = adoptionService;
    }

    @GetMapping("/{id}")
    public String view(@PathVariable("id") Long id, Model model) {
        var adoption = adoptionService.adminAdoptionDetail(id);

        model.addAttribute("adoption", adoption);

        // ✅ lista para a dropdown
        model.addAttribute("adoptionStatuses", Arrays.asList(AdoptionStatus.values()));

        return "admin/adoption-view";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id,
                               @RequestParam("status") AdoptionStatus status,
                               @RequestParam(value = "note", required = false) String note,
                               @RequestParam(value = "visibleToApplicant", defaultValue = "true") boolean visibleToApplicant) {

        adoptionService.adminChangeStatus(id, status, note, visibleToApplicant);
        return "redirect:/admin/adoptions/" + id;
    }

    @PostMapping("/{id}/schedule")
    public String schedule(@PathVariable Long id,
                           @RequestParam("start")
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                           LocalDateTime start,
                           @RequestParam("durationMinutes") int durationMinutes,
                           @RequestParam(value = "location", required = false) String location,
                           @RequestParam(value = "note", required = false) String note) {

        Instant startInstant = start.atZone(ZoneId.of("Europe/Lisbon")).toInstant();

        adoptionService.scheduleVisitAndEmail(id, startInstant, durationMinutes, location, note);
        return "redirect:/admin/adoptions/" + id;
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(value = "note", required = false) String note) {

        adoptionService.reject(id, note);
        return "redirect:/admin/adoptions/" + id;
    }

}
