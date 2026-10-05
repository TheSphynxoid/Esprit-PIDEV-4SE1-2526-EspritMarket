package net.thesphynx.espritmarket.Srv.Repository;

import net.thesphynx.espritmarket.Srv.Entity.ProjectDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IProjectDependencyRepository extends JpaRepository<ProjectDependency, Long> {
    List<ProjectDependency> findByProjectIdOrderByIdAsc(Long projectId);

    List<ProjectDependency> findByProjectIdAndSuccessorMilestoneId(Long projectId, Long successorMilestoneId);

    List<ProjectDependency> findByProjectIdAndPredecessorMilestoneId(Long projectId, Long predecessorMilestoneId);
}
