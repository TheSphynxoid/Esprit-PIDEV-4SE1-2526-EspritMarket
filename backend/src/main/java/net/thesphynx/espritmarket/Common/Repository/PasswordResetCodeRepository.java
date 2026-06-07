package net.thesphynx.espritmarket.Common.Repository;

import net.thesphynx.espritmarket.Common.Entity.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {
    List<PasswordResetCode> findByEmailAndUsedFalse(String email);

    Optional<PasswordResetCode> findTopByEmailAndCodeAndUsedFalseOrderByExpiresAtDesc(String email, String code);
}
