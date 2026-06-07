package net.thesphynx.espritmarket.Delivery.Repository;

import net.thesphynx.espritmarket.Delivery.Entity.Vehicule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IVehiculeRepository extends JpaRepository<Vehicule, Long> {
    List<Vehicule> findByUserId(Long userId);
    List<Vehicule> findByUserEmail(String email);
    Optional<Vehicule> findByIdAndUserEmail(Long id, String email);
    boolean existsByRegistrationnumbers(String registrationnumbers);
    boolean existsByRegistrationnumbersAndIdNot(String registrationnumbers, Long id);
}
