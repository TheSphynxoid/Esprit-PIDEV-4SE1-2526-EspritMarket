package net.thesphynx.espritmarket.Srv.Dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RescheduleDecisionDto {
    @Size(max = 500, message = "Response message must not exceed 500 characters")
    private String responseMessage;
}
