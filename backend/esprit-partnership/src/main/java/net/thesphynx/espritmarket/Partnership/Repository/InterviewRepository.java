package net.thesphynx.espritmarket.Partnership.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import net.thesphynx.espritmarket.Partnership.Entity.Interview;

import java.time.LocalDateTime;
import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
	List<Interview> findByInterviewDateBefore(LocalDateTime date);
	
	@Query("SELECT DISTINCT i FROM Interview i " +
	       "LEFT JOIN FETCH i.application a " +
	       "LEFT JOIN FETCH a.applicant " +
	       "LEFT JOIN FETCH a.jobOffer")
	List<Interview> findAllWithApplicationDetails();

	@Query("SELECT DISTINCT i FROM Interview i " +
	       "LEFT JOIN FETCH i.application a " +
	       "LEFT JOIN FETCH a.applicant " +
	       "LEFT JOIN FETCH a.jobOffer " +
	       "WHERE i.id = :id")
	java.util.Optional<Interview> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT i FROM Interview i " +
           "JOIN i.application a " +
           "JOIN a.jobOffer j " +
           "WHERE j.company.id = :companyId " +
           "AND i.status = 'SCHEDULED' " +
           "AND i.interviewDate >= :fromDate " +
           "ORDER BY i.interviewDate ASC")
    List<Interview> findUpcomingScheduledInterviewsByCompany(
            @Param("companyId") Long companyId,
            @Param("fromDate") LocalDateTime fromDate);

    /** Find SCHEDULED interviews happening between now and a given time */
    @Query("SELECT DISTINCT i FROM Interview i " +
           "LEFT JOIN FETCH i.application a " +
           "LEFT JOIN FETCH a.applicant " +
           "LEFT JOIN FETCH a.jobOffer " +
           "WHERE i.status = net.thesphynx.espritmarket.Partnership.Entity.InterviewStatus.SCHEDULED " +
           "AND i.interviewDate BETWEEN :from AND :to")
    List<Interview> findUpcomingInterviewsBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}