package net.thesphynx.espritmarket.Partnership.Repository;

import net.thesphynx.espritmarket.Partnership.Entity.Profil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfilRepository extends JpaRepository<Profil, Long> {
    Optional<Profil> findByStudentId(Long studentId);
}
