    package net.thesphynx.espritmarket.Partnership.Entity;

    import com.fasterxml.jackson.annotation.JsonIgnore;
    import jakarta.persistence.*;
    import lombok.*;
    import java.util.List;

    @Entity
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public class PartnerCompany {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false)
        private String name;

        private String sector;

        @Column(nullable = false, unique = true)
        private String contactEmail;

        @Column(nullable = false)
        private String partnershipStatus;  // Example: "PENDING", "APPROVED"

        @OneToMany(mappedBy = "company",
                cascade = CascadeType.ALL,
                orphanRemoval = true)
        @JsonIgnore
        private List<JobOffer> jobOffers;
    }