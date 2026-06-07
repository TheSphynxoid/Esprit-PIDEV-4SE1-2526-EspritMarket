package net.thesphynx.espritmarket.Srv.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "project_milestone")
public class ProjectMilestone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    @JsonIgnoreProperties({"members", "services", "partners"})
    private Project project;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Temporal(TemporalType.TIMESTAMP)
    private Date plannedStartDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date plannedEndDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date actualStartDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date actualEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectMilestoneStatus status = ProjectMilestoneStatus.PLANNED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MilestoneType milestoneType = MilestoneType.MILESTONE;

    @Column(length = 500)
    private String conditionExpression;

    @Column(length = 150)
    private String originalTitle;

    @Column(length = 150)
    private String originalDetails;

    @Column(name = "assigned_provider_id")
    private Long assignedProviderId;

    @Column(name = "handoff_notes", length = 500)
    private String handoffNotes;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "milestone_bookings",
            joinColumns = @JoinColumn(name = "milestone_id"),
            inverseJoinColumns = @JoinColumn(name = "booking_id")
    )
    @JsonIgnoreProperties({"user", "provider", "partner", "project", "service"})
    private Set<Booking> bookings = new HashSet<>();

    @OneToMany(mappedBy = "milestone", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"milestone", "service"})
    private Set<MilestoneService> milestoneServices = new HashSet<>();

    @Transient
    public Set<Service> getServices() {
        Set<Service> svcs = new HashSet<>();
        if (milestoneServices != null) {
            for (MilestoneService ms : milestoneServices) {
                if (ms.getService() != null) svcs.add(ms.getService());
            }
        }
        return svcs;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
