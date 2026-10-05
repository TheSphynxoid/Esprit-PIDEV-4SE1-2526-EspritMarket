package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.SlotAllocationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ISlotAllocationAuditRepository extends JpaRepository<SlotAllocationAudit, Long> {
    List<SlotAllocationAudit> findTop50ByProjectIdOrderByCreatedAtDesc(Long projectId);
    List<SlotAllocationAudit> findTop50ByServiceIdOrderByCreatedAtDesc(Long serviceId);
}
