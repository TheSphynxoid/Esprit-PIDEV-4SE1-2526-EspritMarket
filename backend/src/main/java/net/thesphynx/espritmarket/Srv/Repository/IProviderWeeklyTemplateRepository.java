package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.ProviderWeeklyTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface IProviderWeeklyTemplateRepository extends JpaRepository<ProviderWeeklyTemplate, Long> {
    List<ProviderWeeklyTemplate> findByProviderId(Long providerId);
    List<ProviderWeeklyTemplate> findByProviderIdAndServiceId(Long providerId, Long serviceId);
    List<ProviderWeeklyTemplate> findByProviderIdAndDayOfWeek(Long providerId, DayOfWeek dayOfWeek);
    List<ProviderWeeklyTemplate> findByProviderIdAndServiceIdAndDayOfWeek(Long providerId, Long serviceId, DayOfWeek dayOfWeek);

    @Query("SELECT t FROM ProviderWeeklyTemplate t WHERE t.provider.id = :providerId AND t.service.id = :serviceId")
    List<ProviderWeeklyTemplate> findTemplatesForProviderService(Long providerId, Long serviceId);

    void deleteByProviderIdAndServiceId(Long providerId, Long serviceId);

    List<ProviderWeeklyTemplate> findByProviderIdAndServiceIdIsNull(Long providerId);

    void deleteByProviderIdAndServiceIdIsNull(Long providerId);
}
