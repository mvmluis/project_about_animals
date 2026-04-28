package pt.luis.projectaboutanimals.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.luis.projectaboutanimals.model.*;
import pt.luis.projectaboutanimals.dao.DonationRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class DonationService {

    private final DonationRepository donations;
    private final PaypalClient paypal;

    @Value("${paypal.returnUrl}") private String returnUrl;
    @Value("${paypal.cancelUrl}") private String cancelUrl;
    @Value("${paypal.currency:EUR}") private String currency;

    public DonationService(DonationRepository donations, PaypalClient paypal) {
        this.donations = donations;
        this.paypal = paypal;
    }

    // ===================== CLIENT =====================

    @Transactional(readOnly = true)
    public List<Donation> myDonations(Long meId) {
        return donations.findByDonorIdOrderByCreatedAtDesc(meId);
    }

    @Transactional
    public Donation createProductDonation(User me, ProductCategory cat, String desc, String qty, String notes) {
        Donation d = new Donation();
        d.setDonor(me);

        // ✅ enums em PT
        d.setType(DonationType.PRODUTOS);
        d.setStatus(DonationStatus.SUBMETIDA);

        d.setProductCategory(cat);
        d.setProductDescription(desc);
        d.setQuantity(qty);
        d.setDeliveryNotes(notes);

        return donations.save(d);
    }

    @Transactional
    public String startPaypalDonation(User me, BigDecimal amount) {
        Donation d = new Donation();
        d.setDonor(me);

        // ✅ enums em PT
        d.setType(DonationType.DINHEIRO);
        d.setStatus(DonationStatus.PAYPAL_CRIADA);

        d.setAmount(amount);
        d.setCurrency(currency);

        d = donations.save(d);

        String token = paypal.getAccessToken();
        if (token == null) throw new IllegalStateException("PayPal token inválido.");

        Map payload = Map.of(
                "intent", "CAPTURE",
                "purchase_units", new Object[] {
                        Map.of("amount", Map.of(
                                "currency_code", currency,
                                "value", amount.setScale(2).toPlainString()
                        ))
                },
                "application_context", Map.of(
                        "return_url", returnUrl,
                        "cancel_url", cancelUrl
                )
        );

        Map res = paypal.createOrder(token, payload);
        String orderId = (String) res.get("id");
        d.setPaypalOrderId(orderId);
        donations.save(d);

        // encontrar link "approve"
        var links = (java.util.List<Map>) res.get("links");
        String approve = null;
        if (links != null) {
            for (Map l : links) {
                if ("approve".equals(l.get("rel"))) {
                    approve = (String) l.get("href");
                    break;
                }
            }
        }
        if (approve == null) throw new IllegalStateException("Link approve não encontrado no PayPal.");
        return approve;
    }

    @Transactional
    public void handlePaypalReturn(String tokenParam, String payerIdParam, String orderId) {

        Donation d = donations.findByPaypalOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("OrderId PayPal desconhecido."));

        // ✅ enum em PT
        d.setStatus(DonationStatus.PAYPAL_APROVADA);
        donations.save(d);

        String token = paypal.getAccessToken();
        Map capture = paypal.captureOrder(token, orderId);

        String status = capture.get("status") == null ? null : capture.get("status").toString();
        if (!"COMPLETED".equalsIgnoreCase(status)) {
            // ✅ enum em PT
            d.setStatus(DonationStatus.FALHADA);
            donations.save(d);
            throw new IllegalStateException("Pagamento não completado (status=" + status + ").");
        }

        // captura ID (best effort)
        try {
            var purchaseUnits = (java.util.List<Map>) capture.get("purchase_units");
            var payments = (Map) purchaseUnits.get(0).get("payments");
            var captures = (java.util.List<Map>) payments.get("captures");
            d.setPaypalCaptureId((String) captures.get(0).get("id"));
        } catch (Exception ignore) { }

        // ✅ enum em PT
        d.setStatus(DonationStatus.PAGA);
        donations.save(d);
    }

    @Transactional
    public void handlePaypalCancel(String orderId) {
        if (orderId == null || orderId.isBlank()) return;

        donations.findByPaypalOrderId(orderId).ifPresent(d -> {
            // ✅ enum em PT
            d.setStatus(DonationStatus.CANCELADA);
            donations.save(d);
        });
    }

    // ===================== ADMIN =====================
    // Nota: isto pressupõe DonationStatus ter estados para produtos (ex.: EM_TRIAGEM/RECEBIDA)
    // e Donation ter campos adminNotes + handledAt.

    @Transactional(readOnly = true)
    public List<Donation> adminListAll() {
        return donations.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Donation adminGet(Long id) {
        return donations.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Doação não encontrada."));
    }

    @Transactional(readOnly = true)
    public List<Donation> adminList(DonationType type, DonationStatus status) {
        if (type != null && status != null) {
            return donations.findByTypeAndStatusOrderByCreatedAtDesc(type, status);
        }
        if (type != null) {
            return donations.findByTypeOrderByCreatedAtDesc(type);
        }
        if (status != null) {
            return donations.findByStatusOrderByCreatedAtDesc(status);
        }
        return donations.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public void adminUpdateProductDonation(Long donationId, DonationStatus newStatus, String adminNotes) {
        Donation d = adminGet(donationId);

        if (d.getType() != DonationType.PRODUTOS) {
            throw new IllegalArgumentException("Esta ação é apenas para doações de PRODUTOS.");
        }

        // ✅ só permitir estados “de produtos”
        if (!(newStatus == DonationStatus.SUBMETIDA
                || newStatus == DonationStatus.EM_TRIAGEM
                || newStatus == DonationStatus.RECEBIDA
                || newStatus == DonationStatus.CANCELADA)) {
            throw new IllegalArgumentException("Estado inválido para PRODUTOS.");
        }

        d.setStatus(newStatus);
        d.setAdminNotes(adminNotes);

        // marca a data de tratamento quando entra em triagem/recebida
        if (newStatus == DonationStatus.EM_TRIAGEM || newStatus == DonationStatus.RECEBIDA) {
            d.setHandledAt(Instant.now());
        }

        donations.save(d);
    }
}
