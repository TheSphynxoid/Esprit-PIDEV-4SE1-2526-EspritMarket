package net.thesphynx.espritmarket.Partnership.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import net.thesphynx.espritmarket.Partnership.Entity.JobOffer;
import net.thesphynx.espritmarket.Partnership.Dto.JobOfferPerformanceDTO;

import java.util.List;

public interface JobOfferRepository extends JpaRepository<JobOffer, Long>, JpaSpecificationExecutor<JobOffer> {

    @Query("SELECT new net.thesphynx.espritmarket.Partnership.Dto.JobOfferPerformanceDTO(" +
           "j.id, j.title, j.type, j.status, c.name, " +
           "COUNT(DISTINCT a.id), " +
           "AVG(a.matchingScore), " +
           "COUNT(DISTINCT i.id), " +
           "COUNT(CASE WHEN i.status = 'COMPLETED' THEN 1 END)) " +
           "FROM JobOffer j " +
           "JOIN j.company c " +
           "LEFT JOIN j.applications a " +
           "LEFT JOIN a.interviews i " +
           "GROUP BY j.id, j.title, j.type, j.status, c.name")
    List<JobOfferPerformanceDTO> getJobOfferPerformanceReport();
}