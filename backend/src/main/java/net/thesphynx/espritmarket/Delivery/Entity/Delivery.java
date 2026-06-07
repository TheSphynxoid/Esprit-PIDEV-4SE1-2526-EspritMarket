package net.thesphynx.espritmarket.Delivery.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Column;
import lombok.*;
import net.thesphynx.espritmarket.Marketplace.Entity.Order;

import java.util.Date;
import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String deliverytype;
    private String status;
    private Date deliverydate;

    // Legacy address field kept for backward compatibility with existing endpoints.
    private String address ;

    // Detailed delivery address requested by business rules.
    private String deliveryAddress;
    private String city;
    private String postalCode;
    private String phoneNumber;

    @JsonIgnore
    @Column(unique = true)
    private String qrToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_details_id")
    @JsonIgnoreProperties({"deliveries"})
    private DeliveryAddressDetails deliveryAddressDetails;

    @Enumerated(EnumType.STRING)
    private DeliveryMode deliveryMode;

    @Enumerated(EnumType.STRING)
    private PaymentMode paymentMode;

    // Distance in kilometers used to compute home-delivery cost.
    @Column(precision = 10, scale = 3)
    private BigDecimal distanceKm;

    // Package dimensions and weight used for chargeable weight calculation
    @Column(precision = 10, scale = 3)
    private BigDecimal realWeight;

    @Column(precision = 10, scale = 3)
    private BigDecimal length;

    @Column(precision = 10, scale = 3)
    private BigDecimal width;

    @Column(precision = 10, scale = 3)
    private BigDecimal height;

    // Stores the base order amount (products only, before delivery fee) captured at delivery creation
    @Column(precision = 10, scale = 3)
    private BigDecimal baseOrderAmount;

    // Stores the connected user id to keep delivery address history per user.
    private Long connectedUserId;

    @OneToOne(fetch = FetchType.LAZY) //La commande n’est chargée que si nécessaire (optimisation performance)
    @JoinColumn(name = "order_id")
    @JsonIgnoreProperties({"delivery", "orderLines", "user"})
    private Order order;



    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true) //👉 toutes les opérations sont propagées(save, delete, update)
        @JoinColumn(name = "tracking_id")
    private MapTracking tracking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicule_id")
    @JsonIgnoreProperties({"deliveries"})
    private Vehicule vehicule;

    private String cancellationReason;

}
