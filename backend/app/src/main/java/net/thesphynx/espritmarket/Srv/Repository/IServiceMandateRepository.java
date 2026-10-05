package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.ServiceMandate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IServiceMandateRepository extends JpaRepository<ServiceMandate, Long> {
    List<ServiceMandate> findByProviderId(Long providerId);
    Optional<ServiceMandate> findByProviderIdAndServiceId(Long providerId, Long serviceId);
}
