package pt.luis.projectaboutanimals.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.luis.projectaboutanimals.model.ChatConversation;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    @Query("""
        select c
        from ChatConversation c
        left join fetch c.adoptionRequest ar
        join fetch c.client cl
        left join fetch c.admin ad
        where c.id = :id
    """)
    Optional<ChatConversation> findByIdWithRefs(@Param("id") Long id);

    @Query("""
        select c
        from ChatConversation c
        left join fetch c.adoptionRequest ar
        join fetch c.client cl
        left join fetch c.admin ad
        where ar.id = :adoptionId
    """)
    Optional<ChatConversation> findByAdoptionIdWithRefs(@Param("adoptionId") Long adoptionId);

    @Query("""
        select c
        from ChatConversation c
        left join fetch c.adoptionRequest ar
        join fetch c.client cl
        left join fetch c.admin ad
        where c.adoptionRequest is null
          and cl.id = :clientId
    """)
    Optional<ChatConversation> findSupportByClientIdWithRefs(@Param("clientId") Long clientId);

    // inbox admin: todas as conversas (podes filtrar status se quiseres)
    @Query("""
        select c
        from ChatConversation c
        join fetch c.client cl
        left join fetch c.admin ad
        left join fetch c.adoptionRequest ar
        order by coalesce(c.lastMessageAt, c.createdAt) desc
    """)
    List<ChatConversation> findAllForAdminInbox();
}
