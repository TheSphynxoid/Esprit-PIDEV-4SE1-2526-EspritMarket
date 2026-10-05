package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.Deliverable;
import net.thesphynx.espritmarket.Srv.Entity.DeliverableAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IDeliverableAttachmentRepository extends JpaRepository<DeliverableAttachment, Long> {
    List<DeliverableAttachment> findByDeliverableOrderByUploadedAtDesc(Deliverable deliverable);
    Optional<DeliverableAttachment> findByFileUrl(String fileUrl);
}
