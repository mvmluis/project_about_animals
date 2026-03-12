package pt.luis.projectaboutanimals.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "donations")
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // quem doou
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", nullable = false)
    private User donor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DonationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DonationStatus status;

    // -------- PRODUTOS --------
    @Enumerated(EnumType.STRING)
    @Column(name = "product_category", length = 20)
    private ProductCategory productCategory;

    @Column(name = "product_description", length = 1200)
    private String productDescription;

    @Column(length = 120)
    private String quantity;

    @Column(name = "delivery_notes", length = 1200)
    private String deliveryNotes;

    // ✅ campos úteis para o admin tratar
    @Column(name = "admin_notes", length = 1200)
    private String adminNotes;

    @Column(name = "handled_at")
    private Instant handledAt;

    // -------- DINHEIRO (PAYPAL) --------
    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency;

    @Column(name = "paypal_order_id", length = 80)
    private String paypalOrderId;

    @Column(name = "paypal_capture_id", length = 80)
    private String paypalCaptureId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    // ===== getters/setters =====
    public Long getId() { return id; }

    public User getDonor() { return donor; }
    public void setDonor(User donor) { this.donor = donor; }

    public DonationType getType() { return type; }
    public void setType(DonationType type) { this.type = type; }

    public DonationStatus getStatus() { return status; }
    public void setStatus(DonationStatus status) { this.status = status; }

    public ProductCategory getProductCategory() { return productCategory; }
    public void setProductCategory(ProductCategory productCategory) { this.productCategory = productCategory; }

    public String getProductDescription() { return productDescription; }
    public void setProductDescription(String productDescription) { this.productDescription = productDescription; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }

    public String getDeliveryNotes() { return deliveryNotes; }
    public void setDeliveryNotes(String deliveryNotes) { this.deliveryNotes = deliveryNotes; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public Instant getHandledAt() { return handledAt; }
    public void setHandledAt(Instant handledAt) { this.handledAt = handledAt; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPaypalOrderId() { return paypalOrderId; }
    public void setPaypalOrderId(String paypalOrderId) { this.paypalOrderId = paypalOrderId; }

    public String getPaypalCaptureId() { return paypalCaptureId; }
    public void setPaypalCaptureId(String paypalCaptureId) { this.paypalCaptureId = paypalCaptureId; }

    public Instant getCreatedAt() { return createdAt; }
}
