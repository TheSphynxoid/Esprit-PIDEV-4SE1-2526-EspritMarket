package net.thesphynx.espritmarket.Partnership.Entity;

import jakarta.persistence.*;
import lombok.*;
import net.thesphynx.espritmarket.Common.Entity.User;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Profil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User student;

    @ElementCollection
    private List<String> skills = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private ExperienceLevel experienceLevel;

    private String fieldOfStudy;
    
    private String yearsOfExperience;

    @ElementCollection
    private List<String> languages = new ArrayList<>();
}
