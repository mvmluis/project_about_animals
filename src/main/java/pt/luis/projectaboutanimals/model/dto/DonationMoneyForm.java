package pt.luis.projectaboutanimals.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class DonationMoneyForm {

    @NotNull
    @DecimalMin(value = "1.00")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
