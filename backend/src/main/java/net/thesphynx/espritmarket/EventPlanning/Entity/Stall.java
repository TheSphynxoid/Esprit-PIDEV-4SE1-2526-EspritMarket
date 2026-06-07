package net.thesphynx.espritmarket.EventPlanning.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import net.thesphynx.espritmarket.Common.Entity.User;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stall")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Stall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int number;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'ASSIGNED'")
    private String status = "ASSIGNED"; // ASSIGNED, AVAILABLE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    @JsonIgnoreProperties({"stalls", "collaborations", "reservations", "equipments", "tickets"})
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"password"})
    private User user;

    @OneToMany(mappedBy = "stall", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("stall")
    @Builder.Default
    private List<EquipmentReservation> equipmentReservations = new ArrayList<>();
}