package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.DeliverableVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IDeliverableVersionRepository extends JpaRepository<DeliverableVersion, Long> {
    List<DeliverableVersion> findByDeliverableIdOrderByVersionNumberDesc(Long deliverableId);
    List<DeliverableVersion> findByDeliverableIdAndVersionNumberLessThanAndCreatedAtBefore(
            Long deliverableId, int versionNumber, LocalDateTime createdAt);
}
