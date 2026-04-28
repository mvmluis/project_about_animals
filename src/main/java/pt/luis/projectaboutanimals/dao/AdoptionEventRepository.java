package pt.luis.projectaboutanimals.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
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

    // ✅ para permitir apagar um report (apaga eventos das requests desse report)
    @Modifying
    @Transactional
    @Query("""
        delete from AdoptionEvent e
        where e.adoptionRequest.report.id = :reportId
    """)
    int deleteByReportId(@Param("reportId") Long reportId);
}