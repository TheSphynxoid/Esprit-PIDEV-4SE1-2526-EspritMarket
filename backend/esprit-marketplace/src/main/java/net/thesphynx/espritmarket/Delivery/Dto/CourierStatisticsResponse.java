package net.thesphynx.espritmarket.Delivery.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.thesphynx.espritmarket.Delivery.Entity.CourierStatus;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourierStatisticsResponse {
    private Long courierId;
    private Long userId;
    private String courierName;
    private String courierEmail;
    private CourierStatus courierStatus;

    private Long totalVehicles;
    private Long totalDeliveries;
    private Long deliveredDeliveries;
    private Long pendingDeliveries;
    private Long cancelledDeliveries;

    private BigDecimal totalDistanceKm;
    private Double averageDistanceKm;
    private Double completionRate;

    public CourierStatisticsResponse(Long courierId,
                                     Long userId,
                                     String courierName,
                                     String courierEmail,
                                     CourierStatus courierStatus,
                                     Number totalVehicles,
                                     Number totalDeliveries,
                                     Number deliveredDeliveries,
                                     Number pendingDeliveries,
                                     Number cancelledDeliveries,
                                     Number totalDistanceKm,
                                     Number averageDistanceKm) {
        this.courierId = courierId;
        this.userId = userId;
        this.courierName = courierName;
        this.courierEmail = courierEmail;
        this.courierStatus = courierStatus;
        this.totalVehicles = toLong(totalVehicles);
        this.totalDeliveries = toLong(totalDeliveries);
        this.deliveredDeliveries = toLong(deliveredDeliveries);
        this.pendingDeliveries = toLong(pendingDeliveries);
        this.cancelledDeliveries = toLong(cancelledDeliveries);
        this.totalDistanceKm = toBigDecimal(totalDistanceKm);
        this.averageDistanceKm = toDouble(averageDistanceKm);
        this.completionRate = calculateCompletionRate(this.deliveredDeliveries, this.totalDeliveries);
    }

    private static Long toLong(Number value) {
        return value == null ? 0L : value.longValue();
    }

    private static BigDecimal toBigDecimal(Number value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        if (value instanceof BigDecimal decimal) {
            return decimal;
        }

        return BigDecimal.valueOf(value.doubleValue());
    }

    private static Double toDouble(Number value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private static Double calculateCompletionRate(Long delivered, Long total) {
        if (total == null || total == 0L) {
            return 0.0;
        }

        return (delivered.doubleValue() * 100.0) / total.doubleValue();
    }
}
