package net.thesphynx.espritmarket.Marketplace.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.thesphynx.espritmarket.Marketplace.Entity.Store;

@Repository
public interface IStoreRepository extends JpaRepository<Store, Long> {

    // ── Recherche par propriétaire ─────────────────────────────────────────

    /** Retourne la première boutique d'un propriétaire (usage legacy). */
    Optional<Store> findByOwnerId(Long ownerId);

    /** Retourne la première boutique d'un propriétaire triée par ID croissant. */
    Optional<Store> findFirstByOwnerIdOrderByIdAsc(Long ownerId);

    /** Retourne toutes les boutiques d'un propriétaire, triées par ID croissant. */
    List<Store> findAllByOwnerIdOrderByIdAsc(Long ownerId);

    /** Retourne une boutique identifiée par son ID ET son propriétaire. */
    Optional<Store> findByIdAndOwnerId(Long id, Long ownerId);

    // ── Vérifications ─────────────────────────────────────────────────────

    /** Vérifie qu'un propriétaire possède au moins une boutique. */
    boolean existsByOwnerId(Long ownerId);

    /**
     * Compte le nombre de boutiques d'un propriétaire.
     * Utilisé pour appliquer la limite de MAX_STORES_PER_OWNER (3).
     */
    long countByOwnerId(Long ownerId);

    // ── Recherche par nom ──────────────────────────────────────────────────

    /** Recherche insensible à la casse par nom exact. */
    Optional<Store> findByNameIgnoreCase(String name);

    /** Vérifie qu'un nom de boutique est déjà utilisé (insensible à la casse). */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Vérifie qu'un nom de boutique est déjà utilisé par un autre propriétaire.
     * Utile pour valider l'unicité du nom sans bloquer le même propriétaire
     * qui modifie sa boutique.
     */
    @Query("SELECT COUNT(s) > 0 FROM Store s WHERE LOWER(s.name) = LOWER(:name) AND s.owner.id <> :ownerId")
    boolean existsByNameIgnoreCaseAndOwnerIdNot(@Param("name") String name,
                                                 @Param("ownerId") Long ownerId);

        @Query("""
                        SELECT s FROM Store s
                        WHERE (s.active IS NULL OR s.active = true)
                            AND NOT EXISTS (
                                    SELECT p.id FROM Product p
                                    WHERE p.store = s
                                        AND LOWER(COALESCE(p.status, 'ACTIVE')) = 'active'
                                        AND p.updatedAt >= :cutoff
                            )
                        """)
        List<Store> findActiveStoresWithoutRecentActiveProducts(@Param("cutoff") LocalDateTime cutoff);
}