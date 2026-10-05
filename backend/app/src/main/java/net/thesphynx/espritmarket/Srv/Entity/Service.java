package net.thesphynx.espritmarket.Srv.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import net.thesphynx.espritmarket.Common.Entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    @Enumerated(EnumType.STRING)
    private ServiceCategory category;

    @Enumerated(EnumType.STRING)
    private PricingType pricingType;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private ServiceStatus status;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    private String location;
    private String imageUrl;
    
    @Column(nullable = false)
    private boolean allowProjectParticipation = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    @JsonIgnoreProperties({"services"})
    private User provider;

    @ManyToMany(mappedBy = "services")
    @JsonIgnoreProperties({"services", "members", "partners"})
    private Set<Project> projects = new HashSet<>();

    @ManyToMany(mappedBy = "services")
    @JsonIgnoreProperties({"services", "projects", "users", "bookings"})
    private Set<Partner> partners = new HashSet<>();

    @OneToMany(mappedBy = "service")
    @JsonIgnoreProperties({"service", "user", "partner"})
    private Set<Booking> bookings = new HashSet<>();

    @OneToMany(mappedBy = "service", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"service"})
    private Set<ServiceTag> tags = new HashSet<>();

    @OneToMany(mappedBy = "service", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"service"})
    @OrderBy("sortOrder ASC")
    private List<ServicePackage> packages = new ArrayList<>();

    private LocalDateTime deletedAt;
}
