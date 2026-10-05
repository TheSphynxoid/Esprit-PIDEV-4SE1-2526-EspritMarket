package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.DeliverableVersionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IDeliverableVersionAttachmentRepository extends JpaRepository<DeliverableVersionAttachment, Long> {
}
