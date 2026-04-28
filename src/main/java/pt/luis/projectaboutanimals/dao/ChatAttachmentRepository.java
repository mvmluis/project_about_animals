package pt.luis.projectaboutanimals.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import pt.luis.projectaboutanimals.model.ChatAttachment;

import java.util.List;
import java.util.Optional;

public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, Long> {

    @Query("""
        select a
        from ChatAttachment a
        join fetch a.message m
        join fetch m.conversation c
        join fetch m.sender s
        where a.id = :id
    """)
    Optional<ChatAttachment> findByIdWithMessageAndConversation(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        delete from ChatAttachment a
        where a.message.conversation.id = :conversationId
    """)
    int deleteAllByConversationId(@Param("conversationId") Long conversationId);

    @Query("""
        select a
        from ChatAttachment a
        join fetch a.message m
        where m.conversation.id = :conversationId
    """)
    List<ChatAttachment> findAllByConversationId(@Param("conversationId") Long conversationId);
}
