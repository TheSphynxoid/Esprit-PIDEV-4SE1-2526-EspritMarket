package net.thesphynx.espritmarket.Partnership.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import net.thesphynx.espritmarket.Partnership.Entity.Application;
import net.thesphynx.espritmarket.Partnership.Entity.ApplicationActivityStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
	List<Application> findByStatus(String status);
	
	@Query("SELECT DISTINCT a FROM Application a " +
	       "LEFT JOIN FETCH a.applicant " +
	       "LEFT JOIN FETCH a.jobOffer jo " +
	       "LEFT JOIN FETCH jo.company " +
	       "LEFT JOIN FETCH a.interviews " +
	       "WHERE a.applicant.id = :applicantId")
	List<Application> findByApplicantIdWithDetails(@Param("applicantId") Long applicantId);

	@Query("SELECT DISTINCT a FROM Application a " +
	       "LEFT JOIN FETCH a.applicant " +
	       "LEFT JOIN FETCH a.jobOffer jo " +
	       "LEFT JOIN FETCH jo.company " +
	       "LEFT JOIN FETCH a.interviews")
	List<Application> findAllWithDetails();

	// ── Inactivity Detection Queries ─────────────────────

	/** Find applications where last candidate action is older than the given threshold */
	@Query("SELECT a FROM Application a " +
	       "WHERE a.lastCandidateActionAt IS NOT NULL " +
	       "AND a.lastCandidateActionAt < :threshold " +
	       "AND a.status = 'PENDING'")
	List<Application> findInactiveCandidates(@Param("threshold") LocalDateTime threshold);

	/** Find applications that have at least one SCHEDULED interview */
	@Query("SELECT DISTINCT a FROM Application a " +
	       "JOIN a.interviews i " +
	       "WHERE i.status = net.thesphynx.espritmarket.Partnership.Entity.InterviewStatus.SCHEDULED " +
	       "AND a.lastCandidateActionAt IS NOT NULL " +
	       "AND a.lastCandidateActionAt < :threshold " +
	       "AND a.status = 'PENDING'")
	List<Application> findAtRiskCandidates(@Param("threshold") LocalDateTime threshold);

	/** Find all flagged (INACTIVE or AT_RISK) applications with full details */
	@Query("SELECT DISTINCT a FROM Application a " +
	       "LEFT JOIN FETCH a.applicant " +
	       "LEFT JOIN FETCH a.jobOffer jo " +
	       "LEFT JOIN FETCH jo.company " +
	       "WHERE a.flagged = true " +
	       "ORDER BY CASE a.activityStatus " +
	       "  WHEN net.thesphynx.espritmarket.Partnership.Entity.ApplicationActivityStatus.AT_RISK THEN 0 " +
	       "  WHEN net.thesphynx.espritmarket.Partnership.Entity.ApplicationActivityStatus.INACTIVE THEN 1 " +
	       "  ELSE 2 END")
	List<Application> findFlaggedApplications();
}