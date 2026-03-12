package pt.luis.projectaboutanimals.model.dto;

import jakarta.validation.constraints.NotNull;
import pt.luis.projectaboutanimals.model.DonationStatus;

public class AdminDonationUpdateForm {

    @NotNull
    private DonationStatus status;

    private String adminNotes;

    public DonationStatus getStatus() { return status; }
    public void setStatus(DonationStatus status) { this.status = status; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
}
