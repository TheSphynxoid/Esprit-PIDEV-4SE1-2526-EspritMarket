package net.thesphynx.espritmarket.EventPlanning.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import net.thesphynx.espritmarket.Common.Entity.User;

import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private boolean online;

    @Column(name = "meeting_link")
    private String meetingLink;

    @Column(nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'UPCOMING'")
    private String status = "UPCOMING"; // UPCOMING, ONGOING, FINISHED

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private int nbTickets = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"events", "role", "password"})
    private User creator;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("event")
    private List<Stall> stalls = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("event")
    private List<Collaboration> collaborations = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("event")
    private List<Reservation> reservations = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("event")
    private List<Equipment> equipments = new ArrayList<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("event")
    private List<Ticket> tickets = new ArrayList<>();
}
