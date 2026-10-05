package net.thesphynx.espritmarket.Marketplace.Repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import net.thesphynx.espritmarket.Marketplace.Entity.Store;

@Repository
public interface StoreStatsRepository extends JpaRepository<Store, Long> {

    @Query("""
            SELECT COALESCE(SUM(ol.subtotal), 0)
            FROM Store s
            JOIN s.products p
            JOIN p.orderLines ol
            JOIN ol.order o
            WHERE s.id = :storeId
            """)
    Double findTotalRevenueByStoreId(@Param("storeId") Long storeId);

    @Query("""
            SELECT COUNT(DISTINCT o.id)
            FROM Store s
            JOIN s.products p
            JOIN p.orderLines ol
            JOIN ol.order o
            WHERE s.id = :storeId
            """)
    Long countDistinctOrdersByStoreId(@Param("storeId") Long storeId);

    @Query("""
            SELECT p.id, p.name, COALESCE(SUM(ol.quantity), 0), COALESCE(SUM(ol.subtotal), 0)
            FROM Store s
            JOIN s.products p
            JOIN p.orderLines ol
            JOIN ol.order o
            WHERE s.id = :storeId
            GROUP BY p.id, p.name
            ORDER BY SUM(ol.quantity) DESC, SUM(ol.subtotal) DESC
            """)
    List<Object[]> findBestSellerByStoreId(@Param("storeId") Long storeId, Pageable pageable);

    @Query("""
            SELECT YEAR(o.date), MONTH(o.date), COALESCE(SUM(ol.subtotal), 0)
            FROM Store s
            JOIN s.products p
            JOIN p.orderLines ol
            JOIN ol.order o
            WHERE s.id = :storeId
            GROUP BY YEAR(o.date), MONTH(o.date)
            ORDER BY YEAR(o.date), MONTH(o.date)
            """)
    List<Object[]> findMonthlyRevenueByStoreId(@Param("storeId") Long storeId);
}