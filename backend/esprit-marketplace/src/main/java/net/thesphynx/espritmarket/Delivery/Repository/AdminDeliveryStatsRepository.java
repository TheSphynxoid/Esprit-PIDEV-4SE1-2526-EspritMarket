package net.thesphynx.espritmarket.Delivery.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class AdminDeliveryStatsRepository {

    @PersistenceContext
    private EntityManager em;

    public List<Object[]> ordersPerDay(LocalDate fromDate) {
        Query q = em.createNativeQuery(
                "SELECT CAST(date AS DATE) AS d, COUNT(*) FROM orders WHERE date >= :from GROUP BY CAST(date AS DATE) ORDER BY d"
        );
        q.setParameter("from", java.sql.Date.valueOf(fromDate));
        return q.getResultList();
    }

    public List<Object[]> deliveriesPerDay(LocalDate fromDate) {
        Query q = em.createNativeQuery(
                "SELECT CAST(deliverydate AS DATE) AS d, COUNT(*) FROM delivery WHERE deliverydate IS NOT NULL AND deliverydate >= :from GROUP BY CAST(deliverydate AS DATE) ORDER BY d"
        );
        q.setParameter("from", java.sql.Date.valueOf(fromDate));
        return q.getResultList();
    }

    public List<Object[]> interviewsPerDay(LocalDate fromDate) {
        Query q = em.createNativeQuery(
                "SELECT CAST(interview_scheduled_at AS DATE) AS d, COUNT(*) FROM quiz_result WHERE interview_scheduled_at IS NOT NULL AND interview_scheduled_at >= :from GROUP BY CAST(interview_scheduled_at AS DATE) ORDER BY d"
        );
        q.setParameter("from", java.sql.Date.valueOf(fromDate));
        return q.getResultList();
    }

    public Object[] deliveriesByStatus() {
        Query q = em.createNativeQuery(
                "SELECT SUM(CASE WHEN UPPER(status) LIKE 'PENDING%' THEN 1 ELSE 0 END) AS pending, "
                        + "SUM(CASE WHEN UPPER(status) LIKE 'DELIVERED%' THEN 1 ELSE 0 END) AS delivered, "
                        + "SUM(CASE WHEN UPPER(status) LIKE '%REFUS%' OR UPPER(status) LIKE '%REJECT%' OR UPPER(status) LIKE '%CANCEL%' THEN 1 ELSE 0 END) AS refused_cancelled "
                        + "FROM delivery"
        );
        Object single = q.getSingleResult();
        return single instanceof Object[] ? (Object[]) single : new Object[]{single};
    }

    public List<Object[]> couriersPerMonth(LocalDate fromDate) {
        Query q = em.createNativeQuery(
                "SELECT EXTRACT(YEAR FROM created_at) AS y, EXTRACT(MONTH FROM created_at) AS m, COUNT(*) "
                        + "FROM courier WHERE created_at >= :from GROUP BY y,m ORDER BY y,m"
        );
        q.setParameter("from", java.sql.Timestamp.valueOf(fromDate.atStartOfDay()));
        return q.getResultList();
    }
}
