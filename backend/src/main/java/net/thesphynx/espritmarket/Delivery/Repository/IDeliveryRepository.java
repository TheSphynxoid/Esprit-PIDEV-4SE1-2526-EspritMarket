package net.thesphynx.espritmarket.Delivery.Repository;

import net.thesphynx.espritmarket.Delivery.Entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IDeliveryRepository extends JpaRepository<Delivery, Long> {
	List<Delivery> findByConnectedUserIdOrderByIdDesc(Long connectedUserId);
	List<Delivery> findDistinctByOrder_User_IdOrderByIdDesc(Long userId);
	Optional<Delivery> findByQrToken(String qrToken);
	Optional<Delivery> findByOrder_Id(Long orderId);
}
