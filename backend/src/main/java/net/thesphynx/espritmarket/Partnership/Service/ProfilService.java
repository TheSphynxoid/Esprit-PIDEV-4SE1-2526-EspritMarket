package net.thesphynx.espritmarket.Partnership.Service;

import lombok.RequiredArgsConstructor;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Partnership.Dto.ProfilRequest;
import net.thesphynx.espritmarket.Partnership.Entity.ExperienceLevel;
import net.thesphynx.espritmarket.Partnership.Entity.Profil;
import net.thesphynx.espritmarket.Partnership.Repository.ProfilRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfilService {

    private final ProfilRepository profilRepository;

    @Transactional(readOnly = true)
    public Optional<Profil> getByStudentId(Long studentId) {
        return profilRepository.findByStudentId(studentId);
    }

    @Transactional
    public Profil saveOrUpdate(Long studentId, ProfilRequest request) {
        Profil profil = profilRepository.findByStudentId(studentId)
                .orElseGet(() -> {
                    Profil p = new Profil();
                    User student = new User();
                    student.setId(studentId);
                    p.setStudent(student);
                    return p;
                });

        profil.setSkills(request.getSkills());
        if (request.getExperienceLevel() != null && !request.getExperienceLevel().isBlank()) {
            try {
                profil.setExperienceLevel(ExperienceLevel.valueOf(request.getExperienceLevel().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid enum
            }
        }
        profil.setFieldOfStudy(request.getFieldOfStudy());
        profil.setYearsOfExperience(request.getYearsOfExperience());
        profil.setLanguages(request.getLanguages());

        return profilRepository.save(profil);
    }
}
