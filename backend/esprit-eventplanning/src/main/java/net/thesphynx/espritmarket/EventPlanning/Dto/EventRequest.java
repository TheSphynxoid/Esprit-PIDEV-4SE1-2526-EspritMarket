package net.thesphynx.espritmarket.EventPlanning.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "date is required")
    @FutureOrPresent(message = "date must be today or later")
    private LocalDate date;

    @NotBlank(message = "location is required")
    @Pattern(
            regexp = "^(M|IJK|G|A|B|C|D|E|H)$",
            message = "location must be one of M, IJK, G, A, B, C, D, E, H"
    )
    private String location;

    private boolean online;

    private String meetingLink;

    private int nbTickets = 0;
}