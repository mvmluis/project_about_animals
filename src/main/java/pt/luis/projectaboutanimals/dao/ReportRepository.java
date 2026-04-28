package pt.luis.projectaboutanimals.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.luis.projectaboutanimals.model.FoundAnimalReport;
import pt.luis.projectaboutanimals.model.User;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<FoundAnimalReport, Long> {

    // lista "meus" (simples)
    List<FoundAnimalReport> findByCreatedByOrderByCreatedAtDesc(User user);

    // lista global com createdBy carregado (evita Lazy na view)
    @Query("""
        select r from FoundAnimalReport r
        join fetch r.createdBy
        order by r.createdAt desc
    """)
    List<FoundAnimalReport> findAllWithCreatedByOrderByCreatedAtDesc();

    // detalhe com createdBy carregado
    @Query("""
        select r from FoundAnimalReport r
        join fetch r.createdBy
        where r.id = :id
    """)

    Optional<FoundAnimalReport> findByIdWithCreatedBy(@Param("id") Long id);
}
