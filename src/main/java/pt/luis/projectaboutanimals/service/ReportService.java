package pt.luis.projectaboutanimals.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.luis.projectaboutanimals.dao.AdoptionEventRepository;
import pt.luis.projectaboutanimals.dao.AdoptionRequestRepository;
import pt.luis.projectaboutanimals.dao.ReportRepository;
import pt.luis.projectaboutanimals.dao.UserRepository;
import pt.luis.projectaboutanimals.model.*;

import java.util.List;

@Service
public class ReportService {

    private final ReportRepository reports;
    private final UserRepository users;
    private final AdoptionRequestRepository adoptions;
    private final AdoptionEventRepository adoptionEvents;

    public ReportService(ReportRepository reports,
                         UserRepository users,
                         AdoptionRequestRepository adoptions,
                         AdoptionEventRepository adoptionEvents) {
        this.reports = reports;
        this.users = users;
        this.adoptions = adoptions;
        this.adoptionEvents = adoptionEvents;
    }

    // ✅ Resolve o utilizador autenticado tanto para Form Login como para OAuth2 (Google)
    public User getCurrentUser(Authentication auth) {
        if (auth == null) throw new IllegalArgumentException("Auth não existe.");

        String email;

        if (auth instanceof OAuth2AuthenticationToken token) {
            OAuth2User ou = token.getPrincipal();
            email = ou.getAttribute("email"); // Google
        } else {
            email = auth.getName(); // Form login: username/email
        }

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email não encontrado no principal.");
        }

        return getUserByEmail(email);
    }

    public User getUserByEmail(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User não existe"));
    }

    public List<FoundAnimalReport> allReportsForList() {
        return reports.findAllWithCreatedByOrderByCreatedAtDesc();
    }

    public List<FoundAnimalReport> myReports(User user) {
        return reports.findByCreatedByOrderByCreatedAtDesc(user);
    }

    public FoundAnimalReport get(Long id) {
        return reports.findByIdWithCreatedBy(id)
                .orElseThrow(() -> new IllegalArgumentException("Report não existe"));
    }

    public FoundAnimalReport getForClient(User user, Long id) {
        FoundAnimalReport r = get(id);
        if (r.getCreatedBy() == null || r.getCreatedBy().getId() == null) {
            throw new IllegalStateException("Report sem dono (createdBy null).");
        }
        if (!r.getCreatedBy().getId().equals(user.getId())) {
            throw new SecurityException("Sem permissão (não és o dono).");
        }
        return r;
    }

    @Transactional
    public FoundAnimalReport create(User user, FoundAnimalReport data) {
        data.setCreatedBy(user);
        data.setStatus(ReportStatus.PENDENTE);
        return reports.save(data);
    }

    @Transactional
    public FoundAnimalReport updateByClient(User user, Long id, FoundAnimalReport data) {
        FoundAnimalReport r = getForClient(user, id);

        if (r.getStatus() != ReportStatus.PENDENTE) {
            throw new IllegalStateException("Não podes editar após validação (status != PENDENTE).");
        }

        r.setTitle(data.getTitle());
        r.setSpecies(data.getSpecies());
        r.setBreed(data.getBreed());
        r.setColor(data.getColor());
        r.setSize(data.getSize());
        r.setApproxAge(data.getApproxAge());
        r.setFoundAt(data.getFoundAt());
        r.setLocationText(data.getLocationText());
        r.setNotes(data.getNotes());

        if (data.getPhotoUrl() != null) r.setPhotoUrl(data.getPhotoUrl());
        return r;
    }

    @Transactional
    public void deleteByClient(User user, Long id) {
        FoundAnimalReport r = getForClient(user, id);

        if (r.getStatus() != ReportStatus.PENDENTE) {
            throw new IllegalStateException("Não podes apagar após validação.");
        }

        // aqui pode estourar FK se existirem requests/events/chats ligados
        reports.delete(r);
    }

    // ---------- ADOÇÃO ----------

    @Transactional
    public AdoptionRequest createAdoptionRequest(User applicant, Long reportId, AdoptionForm form) {
        FoundAnimalReport report = get(reportId);

        if (report.getStatus() != ReportStatus.APROVADO) {
            throw new IllegalStateException("Este report não está disponível para adoção.");
        }

        if (report.getCreatedBy() != null
                && report.getCreatedBy().getId() != null
                && report.getCreatedBy().getId().equals(applicant.getId())) {
            throw new IllegalStateException("Não podes pedir adoção do teu próprio report.");
        }

        AdoptionRequest req = new AdoptionRequest();
        req.setApplicant(applicant);
        req.setReport(report);
        req.setFullName(form.getFullName());
        req.setPhone(form.getPhone());
        req.setEmail(form.getEmail());
        req.setMessage(form.getMessage());

        return adoptions.save(req);
    }

    // ---------- ADMIN ----------

    public List<FoundAnimalReport> allReports() {
        return reports.findAllWithCreatedByOrderByCreatedAtDesc();
    }

    @Transactional
    public FoundAnimalReport adminUpdate(Long id, FoundAnimalReport data) {
        FoundAnimalReport r = get(id);

        r.setTitle(data.getTitle());
        r.setSpecies(data.getSpecies());
        r.setBreed(data.getBreed());
        r.setColor(data.getColor());
        r.setSize(data.getSize());
        r.setApproxAge(data.getApproxAge());
        r.setFoundAt(data.getFoundAt());
        r.setLocationText(data.getLocationText());
        r.setNotes(data.getNotes());

        if (data.getPhotoUrl() != null) r.setPhotoUrl(data.getPhotoUrl());
        return r;
    }

    @Transactional
    public FoundAnimalReport adminChangeStatus(Long id, ReportStatus status) {
        FoundAnimalReport r = get(id);
        r.setStatus(status);
        return r;
    }

    @Transactional
    public void adminDelete(Long reportId) {
        // ✅ apagar em cadeia para respeitar FK:
        // 1) events -> 2) requests -> 3) report
        adoptionEvents.deleteByReportId(reportId);
        adoptions.deleteByReportId(reportId);
        reports.deleteById(reportId);
    }
}