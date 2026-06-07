package net.thesphynx.espritmarket.Delivery.Service;

import net.thesphynx.espritmarket.Delivery.Dto.AdminDeliveryOverviewResponse;
import net.thesphynx.espritmarket.Delivery.Entity.Courier;
import net.thesphynx.espritmarket.Delivery.Entity.CourierStatus;
import net.thesphynx.espritmarket.Delivery.Entity.Delivery;
import net.thesphynx.espritmarket.Delivery.Repository.AdminDeliveryStatsRepository;
import net.thesphynx.espritmarket.Delivery.Repository.IDeliveryRepository;
import net.thesphynx.espritmarket.Delivery.Repository.IVehiculeRepository;
import net.thesphynx.espritmarket.Delivery.Repository.ICourierRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminDeliveryService {

    private final AdminDeliveryStatsRepository statsRepository;
    private final IDeliveryRepository deliveryRepository;
    private final IVehiculeRepository vehiculeRepository;
    private final ICourierRepository courierRepository;

    public AdminDeliveryService(AdminDeliveryStatsRepository statsRepository,
                                IDeliveryRepository deliveryRepository,
                                IVehiculeRepository vehiculeRepository,
                                ICourierRepository courierRepository) {
        this.statsRepository = statsRepository;
        this.deliveryRepository = deliveryRepository;
        this.vehiculeRepository = vehiculeRepository;
        this.courierRepository = courierRepository;
    }

    public AdminDeliveryOverviewResponse getOverview(int days) {
        int safeDays = Math.max(1, Math.min(days, 365));
        LocalDate from = LocalDate.now().minusDays((long) safeDays - 1);

        AdminDeliveryOverviewResponse resp = new AdminDeliveryOverviewResponse();

        // orders per day
        List<Object[]> ordersRaw = statsRepository.ordersPerDay(from);
        List<AdminDeliveryOverviewResponse.DateCount> orders = new ArrayList<>();
        for (Object[] row : ordersRaw) {
            Object d = row[0];
            Object c = row[1];
            orders.add(new AdminDeliveryOverviewResponse.DateCount(String.valueOf(d), ((Number) c).longValue()));
        }
        resp.setOrdersPerDay(orders);

        // deliveries per day
        List<Object[]> deliveriesRaw = statsRepository.deliveriesPerDay(from);
        List<AdminDeliveryOverviewResponse.DateCount> deliveries = new ArrayList<>();
        for (Object[] row : deliveriesRaw) {
            Object d = row[0];
            Object c = row[1];
            deliveries.add(new AdminDeliveryOverviewResponse.DateCount(String.valueOf(d), ((Number) c).longValue()));
        }
        resp.setDeliveriesPerDay(deliveries);

        // interviews per day
        List<Object[]> interviewsRaw = statsRepository.interviewsPerDay(from);
        List<AdminDeliveryOverviewResponse.DateCount> interviews = new ArrayList<>();
        for (Object[] row : interviewsRaw) {
            Object d = row[0];
            Object c = row[1];
            interviews.add(new AdminDeliveryOverviewResponse.DateCount(String.valueOf(d), ((Number) c).longValue()));
        }
        resp.setInterviewsPerDay(interviews);

        List<Delivery> allDeliveries = deliveryRepository.findAll();
        resp.setPendingDeliveries(countDeliveriesByStatus(allDeliveries, "PENDING", "EN_ATTENTE"));
        resp.setAcceptedDeliveries(countDeliveriesByStatus(allDeliveries, "ACCEPTED", "IN_PROGRESS", "IN PROGRESS"));
        resp.setDeliveredDeliveries(countDeliveriesByStatus(allDeliveries, "DELIVERED", "LIVREE", "LIVRE"));
        resp.setRefusedDeliveries(countDeliveriesByStatus(allDeliveries, "REFUSED", "REJECTED", "DECLINED", "ANNULEE", "ANNULE"));
        resp.setCancelledDeliveries(countDeliveriesByStatus(allDeliveries, "CANCELLED", "CANCELED", "ANNULEE", "ANNULE"));

        // vehicles
        resp.setTotalVehicles(vehiculeRepository.count());

        // couriers per month
        List<Object[]> monthsRaw = statsRepository.couriersPerMonth(from);
        List<AdminDeliveryOverviewResponse.MonthCount> months = new ArrayList<>();
        long totalCouriers = 0L;
        long accepted = 0L, pending = 0L, refused = 0L;
        for (Object[] row : monthsRaw) {
            Number y = (Number) row[0];
            Number m = (Number) row[1];
            Number cnt = (Number) row[2];
            String label = String.format("%04d-%02d", y.intValue(), m.intValue());
            months.add(new AdminDeliveryOverviewResponse.MonthCount(label, cnt.longValue()));
            totalCouriers += cnt.longValue();
        }
        resp.setCouriersPerMonth(months);

        // global courier counts (quick scan)
        List<Courier> allCouriers = courierRepository.findAll();
        resp.setTotalCouriers((long) allCouriers.size());
        resp.setAcceptedCouriers(countCouriersByStatus(allCouriers, CourierStatus.ACCEPTED));
        resp.setPendingCouriers(countCouriersByStatus(allCouriers, CourierStatus.PENDING));
        resp.setRefusedCouriers(countCouriersByStatus(allCouriers, CourierStatus.REFUSED));

        // totals for orders/deliveries
        long totalOrders = orders.stream().mapToLong(d -> d.getCount()).sum();
        long totalDeliveries = deliveries.stream().mapToLong(d -> d.getCount()).sum();
        resp.setTotalOrders(totalOrders);
        resp.setTotalDeliveries(totalDeliveries);

        return resp;
    }

    private long countDeliveriesByStatus(List<Delivery> deliveries, String... statuses) {
        java.util.Set<String> allowed = new java.util.HashSet<>();
        for (String status : statuses) {
            allowed.add(status == null ? "" : status.trim().toUpperCase());
        }

        return deliveries.stream()
                .map(Delivery::getStatus)
                .filter(status -> allowed.contains(status == null ? "" : status.trim().toUpperCase()))
                .count();
    }

    private long countCouriersByStatus(List<Courier> couriers, CourierStatus status) {
        return couriers.stream().filter(courier -> courier.getStatus() == status).count();
    }
}
