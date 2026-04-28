package pt.luis.projectaboutanimals.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.luis.projectaboutanimals.model.Donation;
import pt.luis.projectaboutanimals.model.DonationStatus;
import pt.luis.projectaboutanimals.model.DonationType;
import pt.luis.projectaboutanimals.dao.DonationRepository;

import java.util.List;

@Service
public class DonationAdminService {

    private final DonationRepository donations;

    public DonationAdminService(DonationRepository donations) {
        this.donations = donations;
    }

    // ===================== LISTAGEM ADMIN =====================

    @Transactional(readOnly = true)
    public List<Donation> list(DonationType type, DonationStatus status) {

        // Preferência: filtros no repositório (mais eficiente)
        if (type != null && status != null) {
            return donations.findByTypeAndStatusOrderByCreatedAtDesc(type, status);
        }
        if (type != null) {
            return donations.findByTypeOrderByCreatedAtDesc(type);
        }
        if (status != null) {
            return donations.findByStatusOrderByCreatedAtDesc(status);
        }

        // fallback
        return donations.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Donation get(Long id) {
        if (id == null) throw new IllegalArgumentException("id obrigatório.");
        return donations.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doação não encontrada (id=" + id + ")."));
    }

    // ===================== UPDATE ADMIN (PRODUTOS) =====================

    /**
     * Admin trata doações de PRODUTOS:
     * - muda estado (fluxo manual)
     * - guarda notas do admin
     *
     * Segurança lógica: não permite mexer em doações DINHEIRO por aqui.
     */
    @Transactional
    public Donation updateProductHandling(Long id, DonationStatus newStatus, String adminNotes) {
        if (id == null) throw new IllegalArgumentException("id obrigatório.");
        if (newStatus == null) throw new IllegalArgumentException("newStatus obrigatório.");

        Donation d = get(id);

        if (d.getType() != DonationType.PRODUTOS) {
            throw new IllegalStateException("Esta operação é apenas para doações de PRODUTOS.");
        }

        // Validação de estados permitidos para produtos (ajusta ao teu enum real)
        if (!isAllowedProductStatus(newStatus)) {
            throw new IllegalArgumentException("Estado inválido para PRODUTOS: " + newStatus);
        }

        d.setStatus(newStatus);
        d.setAdminNotes(normalizeNotes(adminNotes));

        return donations.save(d);
    }

    // ===================== UPDATE ADMIN (NOTAS ONLY) =====================

    /**
     * Permite o admin atualizar apenas notas (qualquer tipo),
     * sem mexer no estado (útil para DINHEIRO sem “forçar” o PayPal).
     */
    @Transactional
    public Donation updateAdminNotes(Long id, String adminNotes) {
        Donation d = get(id);
        d.setAdminNotes(normalizeNotes(adminNotes));
        return donations.save(d);
    }

    // ===================== HELPERS =====================

    private boolean isAllowedProductStatus(DonationStatus s) {
        // Se o teu enum tiver mais estados para produtos (ex: EM_TRIAGEM/RECEBIDA),
        // adiciona aqui. Se NÃO existirem, NÃO metas.
        return s == DonationStatus.SUBMETIDA
                || s == DonationStatus.RASCUNHO
                || s == DonationStatus.CANCELADA
                || s == DonationStatus.FALHADA;
    }

    private String normalizeNotes(String notes) {
        if (notes == null) return null;
        String t = notes.trim();
        return t.isEmpty() ? null : t;
    }
}
