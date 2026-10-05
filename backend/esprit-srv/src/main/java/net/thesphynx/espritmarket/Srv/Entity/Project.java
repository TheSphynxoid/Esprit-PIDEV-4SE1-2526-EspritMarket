package net.thesphynx.espritmarket.Srv.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import net.thesphynx.espritmarket.Common.Entity.User;

import java.math.BigDecimal;
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
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String details;
    private Date startDate;
    private Date estimatedEndDate;
    private Date endDate;

    @Column(precision = 12, scale = 2)
    private BigDecimal budget;

    @Enumerated(EnumType.STRING)
    private ProjectStatus status;

    private String priority;

    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    @JsonIgnoreProperties({"projects", "services", "partner", "bookings", "serviceReviews"})
    private User creator;

    @ManyToMany
    @JoinTable(
            name = "project_users",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnoreProperties({"projects", "services", "partner", "bookings", "serviceReviews"})
    private Set<User> members = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "project_services",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    @JsonIgnoreProperties({"projects", "partners", "bookings", "serviceReviews", "provider"})
    private Set<Service> services = new HashSet<>();

    @ManyToMany(mappedBy = "projects")
    @JsonIgnoreProperties({"projects", "services", "users", "bookings"})
    private Set<Partner> partners = new HashSet<>();
}
