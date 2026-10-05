package net.thesphynx.espritmarket.Srv.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import net.thesphynx.espritmarket.Common.Entity.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "time_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    @JsonIgnoreProperties({"bookings", "messages", "attachments", "project", "deliverables"})
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"bookings", "projects", "services", "partners"})
    private User user;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Transient
    public boolean isActive() {
        return endTime == null;
    }
}
