package net.thesphynx.espritmarket.Delivery.Entity;

import jakarta.persistence.*;
import lombok.*;
import net.thesphynx.espritmarket.Common.Entity.User;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "courier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Courier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String permitFileName;

    private String permitContentType;

    @JdbcTypeCode(SqlTypes.VARBINARY) //permet à Hibernate de comprendre le type
    @Column(name = "permit_image", columnDefinition = "BYTEA") //VARBINARY / BYTEA type PostgreSQL pour données binaires
    private byte[] permitImage;  //stocke l’image en binaire

    @Column(name = "interview_date")
    private LocalDateTime interviewDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CourierStatus status = CourierStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile_status")
    @Builder.Default
    private CourierProfileStatus profileStatus = CourierProfileStatus.INCOMPLETE;

    @PrePersist
    private void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (profileStatus == null) {
            profileStatus = CourierProfileStatus.INCOMPLETE;
        }
    }
}
