package pt.luis.projectaboutanimals.model.dto;

import jakarta.validation.constraints.NotNull;
import pt.luis.projectaboutanimals.model.DonationType;

public class DonationChooseForm {

    @NotNull
    private DonationType type;

    public DonationType getType() { return type; }
    public void setType(DonationType type) { this.type = type; }
}
