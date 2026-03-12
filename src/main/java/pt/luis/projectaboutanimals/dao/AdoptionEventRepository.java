package pt.luis.projectaboutanimals.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.luis.projectaboutanimals.model.AdoptionEvent;

import java.util.List;

public interface AdoptionEventRepository extends JpaRepository<AdoptionEvent, Long> {

    @Query("""
        select e
        from AdoptionEvent e
        join e.adoptionRequest a
        where a.id = :adoptionId
          and a.applicant.id = :applicantId
          and e.visibleToApplicant = true
        order by e.createdAt asc
    """)
    List<AdoptionEvent> findVisibleTimelineForApplicant(@Param("adoptionId") Long adoptionId,
                                                        @Param("applicantId") Long applicantId);
}
