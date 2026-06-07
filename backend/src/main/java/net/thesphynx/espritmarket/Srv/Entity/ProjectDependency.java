package net.thesphynx.espritmarket.Srv.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "project_dependency", uniqueConstraints = {
        @UniqueConstraint(name = "uk_project_dependency_pair", columnNames = {"project_id", "predecessor_milestone_id", "successor_milestone_id"})
})
public class ProjectDependency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "predecessor_milestone_id", nullable = false)
    private ProjectMilestone predecessorMilestone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "successor_milestone_id", nullable = false)
    private ProjectMilestone successorMilestone;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
