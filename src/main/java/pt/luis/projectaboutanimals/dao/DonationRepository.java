package pt.luis.projectaboutanimals.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import pt.luis.projectaboutanimals.model.*;

import java.util.List;
import java.util.Optional;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    List<Donation> findByDonorIdOrderByCreatedAtDesc(Long donorId);

    Optional<Donation> findByPaypalOrderId(String paypalOrderId);

    // ✅ admin
    List<Donation> findAllByOrderByCreatedAtDesc();

    List<Donation> findByTypeOrderByCreatedAtDesc(DonationType type);

    List<Donation> findByStatusOrderByCreatedAtDesc(DonationStatus status);

    List<Donation> findByTypeAndStatusOrderByCreatedAtDesc(DonationType type, DonationStatus status);
}
