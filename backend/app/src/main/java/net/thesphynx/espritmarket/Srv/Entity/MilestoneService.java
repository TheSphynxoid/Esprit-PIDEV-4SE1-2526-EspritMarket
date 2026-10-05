package net.thesphynx.espritmarket.Srv.Entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Entity
@Table(name = "milestone_services")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@IdClass(MilestoneService.MilestoneServiceId.class)
public class MilestoneService {

    @Id
    @Column(name = "milestone_id")
    private Long milestoneId;

    @Id
    @Column(name = "service_id")
    private Long serviceId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "milestone_id", insertable = false, updatable = false)
    private ProjectMilestone milestone;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "service_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"projects", "partners", "bookings", "tags", "provider", "milestoneServices"})
    private Service service;

    @Column(name = "estimated_hours")
    private double estimatedHours = 2.0;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class MilestoneServiceId implements java.io.Serializable {
        private Long milestoneId;
        private Long serviceId;
    }
}
