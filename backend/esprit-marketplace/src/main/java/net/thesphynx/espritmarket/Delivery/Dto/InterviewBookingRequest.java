package net.thesphynx.espritmarket.Delivery.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewBookingRequest {
    @NotNull(message = "interviewDate is required")
    private LocalDateTime interviewDate;
}
