package net.thesphynx.espritmarket.Delivery.Repository;

import net.thesphynx.espritmarket.Delivery.Entity.DeliveryAddressDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IDeliveryAddressDetailsRepository extends JpaRepository<DeliveryAddressDetails, Long> {
	List<DeliveryAddressDetails> findByConnectedUserIdOrderByIdDesc(Long connectedUserId);
	Optional<DeliveryAddressDetails> findByIdAndConnectedUserId(Long id, Long connectedUserId);
}
