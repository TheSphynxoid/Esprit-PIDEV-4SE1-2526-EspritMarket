package net.thesphynx.espritmarket.EventPlanning.Repository;

import net.thesphynx.espritmarket.EventPlanning.Entity.Collaboration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ICollaborationRepository extends JpaRepository<Collaboration, Long> {
}