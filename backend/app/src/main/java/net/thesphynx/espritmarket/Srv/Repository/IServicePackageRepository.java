package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.ServicePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IServicePackageRepository extends JpaRepository<ServicePackage, Long> {
    List<ServicePackage> findByServiceId(Long serviceId);
}
