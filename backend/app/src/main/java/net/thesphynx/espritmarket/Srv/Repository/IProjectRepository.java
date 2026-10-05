package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.Booking;
import net.thesphynx.espritmarket.Srv.Entity.Project;
import net.thesphynx.espritmarket.Srv.Entity.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IProjectRepository extends JpaRepository<Project, Long> {
    @Query("SELECT p FROM Project p WHERE p.deletedAt IS NULL")
    Page<Project> findAllActive(Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p " +
           "LEFT JOIN p.members m " +
           "LEFT JOIN Booking b ON b.project.id = p.id " +
           "WHERE p.deletedAt IS NULL AND (m.id = :userId OR b.provider.id = :userId OR b.user.id = :userId)")
    Page<Project> findParticipatingByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Project p " +
           "WHERE p.deletedAt IS NULL " +
           "AND p.status IN :openStatuses " +
           "AND p.id NOT IN (" +
           "  SELECT DISTINCT p2.id FROM Project p2 " +
           "  LEFT JOIN p2.members m " +
           "  LEFT JOIN Booking b ON b.project.id = p2.id " +
           "  WHERE m.id = :userId OR b.provider.id = :userId OR b.user.id = :userId" +
           ")")
    Page<Project> findOpenPositionsByUserId(@Param("userId") Long userId,
                                            @Param("openStatuses") List<ProjectStatus> openStatuses,
                                            Pageable pageable);
}
