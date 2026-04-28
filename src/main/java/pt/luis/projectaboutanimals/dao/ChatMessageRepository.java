package pt.luis.projectaboutanimals.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import pt.luis.projectaboutanimals.model.ChatMessage;
import pt.luis.projectaboutanimals.model.Role;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("""
        select distinct m
        from ChatMessage m
        join fetch m.sender s
        left join fetch m.attachments a
        where m.conversation.id = :conversationId
        order by m.id asc
    """)
    List<ChatMessage> findAllByConversationWithSenderAndAttachments(@Param("conversationId") Long conversationId);

    @Query("""
        select distinct m
        from ChatMessage m
        join fetch m.sender s
        left join fetch m.attachments a
        where m.conversation.id = :conversationId
          and m.id > :afterId
        order by m.id asc
    """)
    List<ChatMessage> findAfterId(@Param("conversationId") Long conversationId,
                                  @Param("afterId") Long afterId);

    @Query("""
        select distinct m
        from ChatMessage m
        join fetch m.sender s
        left join fetch m.attachments a
        where m.id = :id
    """)
    Optional<ChatMessage> findByIdWithSenderAndAttachments(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        delete from ChatMessage m
        where m.conversation.id = :conversationId
    """)
    int deleteAllByConversationId(@Param("conversationId") Long conversationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        update ChatMessage m
           set m.readAtAdmin = CURRENT_TIMESTAMP
         where m.conversation.id = :conversationId
           and m.senderRole = 'CLIENT'
           and m.readAtAdmin is null
    """)
    int markReadByAdmin(@Param("conversationId") Long conversationId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        update ChatMessage m
           set m.readAtClient = CURRENT_TIMESTAMP
         where m.conversation.id = :conversationId
           and m.senderRole = 'ADMIN'
           and m.readAtClient is null
    """)
    int markReadByClient(@Param("conversationId") Long conversationId);

    @Query("""
        select count(m)
          from ChatMessage m
         where m.conversation.id = :conversationId
           and m.senderRole = :fromRole
           and (
                (:viewerRole = 'ADMIN'  and m.readAtAdmin  is null) or
                (:viewerRole = 'CLIENT' and m.readAtClient is null)
           )
    """)
    long countUnread(@Param("conversationId") Long conversationId,
                     @Param("fromRole") Role fromRole,
                     @Param("viewerRole") Role viewerRole);

    @Query("""
    select count(m)
    from ChatMessage m
    join m.conversation c
    where c.admin.id = :adminId
      and m.senderRole = pt.luis.projectaboutanimals.model.Role.CLIENT
      and m.readAtAdmin is null
""")
    long countUnreadForAdmin(@Param("adminId") Long adminId);

}
