package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.ProviderException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IProviderExceptionRepository extends JpaRepository<ProviderException, Long> {
    List<ProviderException> findByProviderId(Long providerId);
    List<ProviderException> findByProviderIdAndDate(Long providerId, LocalDate date);
    List<ProviderException> findByProviderIdAndDateBetween(Long providerId, LocalDate startDate, LocalDate endDate);
    boolean existsByProviderIdAndDate(Long providerId, LocalDate date);
}
