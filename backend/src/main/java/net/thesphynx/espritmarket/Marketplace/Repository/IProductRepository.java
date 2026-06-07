package net.thesphynx.espritmarket.Marketplace.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.thesphynx.espritmarket.Marketplace.Entity.Product;

public interface IProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByOrderByIdDesc();

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.store s LEFT JOIN FETCH p.category WHERE p.id = :id")
    Optional<Product> findWithStoreAndCategoryById(@Param("id") Long id);

    // ✅ NOUVEAU : filtrer les produits par boutique
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.store s LEFT JOIN FETCH p.category WHERE s.id = :storeId ORDER BY p.id DESC")
    List<Product> findAllByStoreIdOrderByIdDesc(@Param("storeId") Long storeId);

    @Query("""
            SELECT DISTINCT p
            FROM Product p
            LEFT JOIN FETCH p.store s
            LEFT JOIN FETCH p.category c
            WHERE LOWER(COALESCE(p.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY p.id DESC
            """)
    List<Product> searchByKeyword(@Param("keyword") String keyword);

    @Query(value = """
            SELECT p.*
            FROM product p
            LEFT JOIN category c ON p.category_id = c.id
            WHERE similarity(COALESCE(p.name, ''), :query) > 0.15
               OR similarity(COALESCE(p.description, ''), :query) > 0.1
               OR similarity(COALESCE(c.name, ''), :query) > 0.2
            ORDER BY similarity(p.name, :query) DESC
            LIMIT 20
            """, nativeQuery = true)
    List<Product> semanticSearch(@Param("query") String query);
}