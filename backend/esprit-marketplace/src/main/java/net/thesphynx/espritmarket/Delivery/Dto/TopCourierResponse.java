package net.thesphynx.espritmarket.Delivery.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TopCourierResponse {
    private Long courierId;
    private Long userId;
    private String courierName;
    private String courierEmail;
    private Long deliveredDeliveries;

    public TopCourierResponse(Long courierId,
                              Long userId,
                              String courierName,
                              String courierEmail,
                              Number deliveredDeliveries) {
        this.courierId = courierId;
        this.userId = userId;
        this.courierName = courierName;
        this.courierEmail = courierEmail;
        this.deliveredDeliveries = deliveredDeliveries == null ? 0L : deliveredDeliveries.longValue();
    }
}
