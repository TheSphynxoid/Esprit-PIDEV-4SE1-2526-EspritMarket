package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.ProviderMandate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IProviderMandateRepository extends JpaRepository<ProviderMandate, Long> {
    Optional<ProviderMandate> findByProviderId(Long providerId);
}
