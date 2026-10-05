package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.ServiceTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IServiceTagRepository extends JpaRepository<ServiceTag, Long> {
    List<ServiceTag> findByServiceId(Long serviceId);
    List<ServiceTag> findByTagIn(List<String> tags);

    @Modifying
    void deleteByServiceId(Long serviceId);
}
