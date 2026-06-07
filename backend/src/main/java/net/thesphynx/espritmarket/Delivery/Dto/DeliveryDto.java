package net.thesphynx.espritmarket.Delivery.Dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.thesphynx.espritmarket.Delivery.Entity.DeliveryMode;
import net.thesphynx.espritmarket.Delivery.Entity.PaymentMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryDto {
        @jakarta.validation.constraints.NotBlank(message = "deliverytype is required")
        @Pattern(
            regexp = "^(STANDARD|EXPRESS|SAME_DAY)$",
            message = "deliverytype must be STANDARD, EXPRESS, or SAME_DAY"
        )
    private String deliverytype;

        @jakarta.validation.constraints.NotBlank(message = "status is required")
        @Pattern(
            regexp = "(?i)^(Pending|In progress|Delivered|Cancelled|Validated|Valide|Confirmed|Paid|Completed|Livree)$",
            message = "status must be one of: Pending, In progress, Delivered, Cancelled, Validated, Confirmed, Paid, Completed, or Livree"
        )
    private String status;

        private String cancellationReason;

        @NotNull(message = "deliverydate is required")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date deliverydate;

    private String address;

    private String deliveryAddress;

    private String city;

    private String postalCode;

    @Pattern(regexp = "^[0-9+ ]{8,20}$", message = "phoneNumber format is invalid")
    private String phoneNumber;

    private Long addressDetailsId;

    @NotNull(message = "deliveryMode is required")
    private DeliveryMode deliveryMode;

    @NotNull(message = "paymentMode is required")
    private PaymentMode paymentMode;

    private BigDecimal distanceKm;

    // Package weight and dimensions
    private BigDecimal realWeight;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;

    // Base order amount (products only, captured at delivery creation)
    private BigDecimal baseOrderAmount;

    private Long orderId;
    private Long vehiculeId;

    @Valid
    private MapTrackingDto tracking;

    @AssertTrue(message = "deliverydate must be between today and end of current year")
    public boolean isDeliveryDateInAllowedRange() {
        if (deliverydate == null) {
            return true;
        }

        LocalDate requestedDate = deliverydate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate today = LocalDate.now();
        LocalDate endOfYear = LocalDate.of(today.getYear(), 12, 31);

        return !requestedDate.isBefore(today) && !requestedDate.isAfter(endOfYear);
    }

    @AssertTrue(message = "Provide addressDetailsId, or provide deliveryAddress/city/postalCode/phoneNumber")
    public boolean isAddressSelectionValid() {
        if (addressDetailsId != null) {
            return true;
        }

        return hasText(deliveryAddress)
                && hasText(city)
                && hasText(postalCode)
                && hasText(phoneNumber);
    }

    @AssertTrue(message = "Provide a cancellationReason when status is Cancelled")
    public boolean isCancellationReasonValid() {
        if (!"Cancelled".equals(status)) {
            return true;
        }

        return hasText(cancellationReason);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
