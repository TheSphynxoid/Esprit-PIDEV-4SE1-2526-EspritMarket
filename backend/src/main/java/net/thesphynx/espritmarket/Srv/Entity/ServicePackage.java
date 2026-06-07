package net.thesphynx.espritmarket.Srv.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "service_packages", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"service_id", "tier"})
})
public class ServicePackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    @JsonIgnoreProperties({"packages", "bookings", "tags", "projects", "partners"})
    private Service service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PackageTier tier;

    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    private Integer deliveryDays = 7;

    private Integer revisions = 1;

    @Column(columnDefinition = "TEXT")
    private String features;

    @Column(nullable = false)
    private int sortOrder = 0;
}
