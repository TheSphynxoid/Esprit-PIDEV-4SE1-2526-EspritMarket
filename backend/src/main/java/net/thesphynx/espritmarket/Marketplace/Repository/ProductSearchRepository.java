package net.thesphynx.espritmarket.Marketplace.Repository;

import net.thesphynx.espritmarket.Marketplace.Entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSearchRepository extends JpaRepository<Product, Long> {

    @Query(value = """
            SELECT p.id,
                   p.name,
                   p.price,
                   COALESCE(s.name, ''),
                   COALESCE(c.name, ''),
                   (
                       (CASE WHEN LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) THEN 4 ELSE 0 END)
                       + (CASE WHEN LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :q, '%')) THEN 2 ELSE 0 END)
                       + (CASE WHEN LOWER(COALESCE(s.name, '')) LIKE LOWER(CONCAT('%', :q, '%')) THEN 3 ELSE 0 END)
                       + (CASE WHEN LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :q, '%')) THEN 1 ELSE 0 END)
                   )
            FROM Product p
            LEFT JOIN p.store s
            LEFT JOIN p.category c
            WHERE (:q IS NULL OR :q = ''
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(s.name, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:category IS NULL OR :category = '' OR LOWER(COALESCE(c.name, '')) = LOWER(:category))
            ORDER BY (
                       (CASE WHEN LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) THEN 4 ELSE 0 END)
                       + (CASE WHEN LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :q, '%')) THEN 2 ELSE 0 END)
                       + (CASE WHEN LOWER(COALESCE(s.name, '')) LIKE LOWER(CONCAT('%', :q, '%')) THEN 3 ELSE 0 END)
                       + (CASE WHEN LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :q, '%')) THEN 1 ELSE 0 END)
                     ) DESC,
                     p.id DESC
            """,
            countQuery = """
            SELECT COUNT(p.id)
            FROM Product p
            LEFT JOIN p.store s
            LEFT JOIN p.category c
            WHERE (:q IS NULL OR :q = ''
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(s.name, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:category IS NULL OR :category = '' OR LOWER(COALESCE(c.name, '')) = LOWER(:category))
            """)
    Page<Object[]> searchProducts(
            @Param("q") String q,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice,
            @Param("category") String category,
            Pageable pageable
    );
}
