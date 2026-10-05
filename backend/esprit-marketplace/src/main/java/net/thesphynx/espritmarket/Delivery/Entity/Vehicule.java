package net.thesphynx.espritmarket.Delivery.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import net.thesphynx.espritmarket.Common.Entity.User;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String type;
    private String registrationnumbers;
    private Double capacity;
    private String status;

    private String vehiclePhotoFileName;
    private String vehiclePhotoContentType;

    @JsonIgnore
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "vehicle_photo", columnDefinition = "BYTEA")
    private byte[] vehiclePhoto;

    private String registrationCardFrontFileName;
    private String registrationCardFrontContentType;

    @JsonIgnore
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "registration_card_front", columnDefinition = "BYTEA")
    private byte[] registrationCardFront;

    private String registrationCardBackFileName;
    private String registrationCardBackContentType;

    @JsonIgnore
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "registration_card_back", columnDefinition = "BYTEA")
    private byte[] registrationCardBack;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;



    @OneToMany(mappedBy = "vehicule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"vehicule"})
    @Builder.Default
    private List<Delivery> deliveries = new ArrayList<>();

}
