package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.MilestoneService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IMilestoneServiceRepository extends JpaRepository<MilestoneService, MilestoneService.MilestoneServiceId> {
}
