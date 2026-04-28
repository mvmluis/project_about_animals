package pt.luis.projectaboutanimals.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

public class ReportForm {

    @NotBlank
    private String title;

    @NotBlank
    private String species;

    private String breed;
    private String color;
    private String size;
    private String approxAge;

    @NotBlank
    private String locationText;

    @NotNull
    private Instant foundAt;

    private String notes;

    // ✅ upload opcional
    private MultipartFile photo;

    // getters/setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getApproxAge() { return approxAge; }
    public void setApproxAge(String approxAge) { this.approxAge = approxAge; }
    public String getLocationText() { return locationText; }
    public void setLocationText(String locationText) { this.locationText = locationText; }
    public Instant getFoundAt() { return foundAt; }
    public void setFoundAt(Instant foundAt) { this.foundAt = foundAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public MultipartFile getPhoto() { return photo; }
    public void setPhoto(MultipartFile photo) { this.photo = photo; }
}
