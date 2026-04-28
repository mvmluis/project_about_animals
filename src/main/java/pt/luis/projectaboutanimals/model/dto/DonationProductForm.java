package pt.luis.projectaboutanimals.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pt.luis.projectaboutanimals.model.ProductCategory;

public class DonationProductForm {

    @NotNull
    private ProductCategory category;

    @NotBlank
    @Size(max = 500)
    private String description;

    @NotBlank
    @Size(max = 80)
    private String quantity;

    @Size(max = 700)
    private String deliveryNotes;

    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getQuantity() { return quantity; }
    public void setQuantity(String quantity) { this.quantity = quantity; }

    public String getDeliveryNotes() { return deliveryNotes; }
    public void setDeliveryNotes(String deliveryNotes) { this.deliveryNotes = deliveryNotes; }
}
