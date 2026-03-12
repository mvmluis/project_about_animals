package pt.luis.projectaboutanimals.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import pt.luis.projectaboutanimals.model.AdoptionRequest;
import pt.luis.projectaboutanimals.model.AdoptionStatus;

import java.util.List;
import java.util.Optional;

public interface AdoptionRequestRepository extends JpaRepository<AdoptionRequest, Long> {

    @Query("""
        select ar
        from AdoptionRequest ar
        join fetch ar.report r
        join fetch ar.applicant a
        order by ar.createdAt desc
    """)
    List<AdoptionRequest> findAllWithReportAndApplicantOrderByCreatedAtDesc();

    @Query("""
        select ar
        from AdoptionRequest ar
        join fetch ar.report r
        join fetch ar.applicant a
        where a.id = :applicantId
        order by ar.createdAt desc
    """)
    List<AdoptionRequest> findMineWithReportAndApplicant(@Param("applicantId") Long applicantId);

    @Query("""
        select ar
        from AdoptionRequest ar
        join fetch ar.report r
        join fetch ar.applicant a
        where ar.id = :adoptionId and a.id = :applicantId
    """)
    Optional<AdoptionRequest> findMineByIdWithReportAndApplicant(@Param("adoptionId") Long adoptionId,
                                                                 @Param("applicantId") Long applicantId);

    @Query("""
        select ar
        from AdoptionRequest ar
        join fetch ar.report r
        join fetch ar.applicant a
        where ar.id = :id
    """)
    Optional<AdoptionRequest> findByIdWithReportAndApplicant(@Param("id") Long id);

    // ✅ OPÇÃO A: report ids com processo ativo (status NOT IN excluídos)
    @Query("""
        select distinct r.id
        from AdoptionRequest ar
        join ar.report r
        where ar.status not in :excluded
    """)
    List<Long> findReportIdsWithActiveProcess(@Param("excluded") List<AdoptionStatus> excluded);

    // ✅ Necessário para permitir apagar um report que tem pedidos associados (evita FK 1451)
    @Modifying
    @Transactional
    @Query("""
        delete from AdoptionRequest ar
        where ar.report.id = :reportId
    """)

    int deleteByReportId(@Param("reportId") Long reportId);
}
