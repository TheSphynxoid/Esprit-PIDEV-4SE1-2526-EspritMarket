package net.thesphynx.espritmarket.Marketplace.Repository;

import net.thesphynx.espritmarket.Marketplace.Entity.RequestStatus;
import net.thesphynx.espritmarket.Marketplace.Entity.SellerRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ISellerRequestRepository extends JpaRepository<SellerRequest, Long> {
    Optional<SellerRequest> findTopByUserIdOrderByDateDemandeDesc(Long userId);

    boolean existsByEmailIgnoreCaseAndStatut(String email, RequestStatus statut);

    boolean existsByUserIdAndStatut(Long userId, RequestStatus statut);
}
