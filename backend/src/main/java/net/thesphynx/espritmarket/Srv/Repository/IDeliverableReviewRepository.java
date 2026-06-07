package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.Deliverable;
import net.thesphynx.espritmarket.Srv.Entity.DeliverableReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IDeliverableReviewRepository extends JpaRepository<DeliverableReview, Long> {
    List<DeliverableReview> findByDeliverableOrderByReviewedAtDesc(Deliverable deliverable);
}
