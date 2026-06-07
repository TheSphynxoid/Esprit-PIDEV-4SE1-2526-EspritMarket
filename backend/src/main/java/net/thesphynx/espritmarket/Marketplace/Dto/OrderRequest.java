package net.thesphynx.espritmarket.Marketplace.Dto;

import java.util.Date;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderRequest {

    @NotNull(message = "Order date is required")
    private Date date;

    @NotBlank(message = "Status is required")
    @Size(max = 50, message = "Status must not exceed 50 characters")
    private String status;

    @NotNull(message = "Total amount is required")
    private Double totalAmount;

    

    private Long userId;
    private List<OrderLineRequest> orderLines;
}