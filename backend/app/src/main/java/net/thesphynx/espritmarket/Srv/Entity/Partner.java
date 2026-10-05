package net.thesphynx.espritmarket.Srv.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import net.thesphynx.espritmarket.Common.Entity.Role;
import net.thesphynx.espritmarket.Common.Entity.User;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Partner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String contactInfo;

    @Enumerated(EnumType.STRING)
    private Role role;

    private LocalDateTime deletedAt;

    @OneToMany
    @JoinColumn(name = "partner_id")
    @JsonIgnoreProperties({"projects", "services", "bookings", "serviceReviews"})
    private Set<User> users = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "partner_projects",
            joinColumns = @JoinColumn(name = "partner_id"),
            inverseJoinColumns = @JoinColumn(name = "project_id")
    )
    @JsonIgnoreProperties({"partners", "members", "services"})
    private Set<Project> projects = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "partner_services",
            joinColumns = @JoinColumn(name = "partner_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    @JsonIgnoreProperties({"partners", "projects", "bookings", "serviceReviews", "provider"})
    private Set<Service> services = new HashSet<>();

    @OneToMany(mappedBy = "partner")
    @JsonIgnoreProperties({"partner", "user", "service"})
    private Set<Booking> bookings = new HashSet<>();
}
