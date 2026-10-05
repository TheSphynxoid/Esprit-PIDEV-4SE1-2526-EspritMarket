package net.thesphynx.espritmarket.Srv.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BookingMessageRequest {
    @NotBlank(message = "Message is required")
    @Size(max = 4000, message = "Message must not exceed 4000 characters")
    private String message;
}
