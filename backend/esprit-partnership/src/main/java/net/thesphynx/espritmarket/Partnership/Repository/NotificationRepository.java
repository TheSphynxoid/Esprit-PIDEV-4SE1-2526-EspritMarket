package net.thesphynx.espritmarket.Partnership.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import net.thesphynx.espritmarket.Partnership.Entity.Notification;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);
}
