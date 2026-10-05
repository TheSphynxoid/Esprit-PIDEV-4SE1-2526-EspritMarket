package net.thesphynx.espritmarket.Delivery.Repository;

import net.thesphynx.espritmarket.Delivery.Entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("""
            select m
            from Message m
            where (m.senderId = :user1 and m.receiverId = :user2)
               or (m.senderId = :user2 and m.receiverId = :user1)
            order by m.sentAt asc
            """)
    List<Message> findConversation(@Param("user1") String user1, @Param("user2") String user2);
}