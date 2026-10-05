package net.thesphynx.espritmarket.EventPlanning.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventStatusResponse {
    private Long id;
    private String name;
    private LocalDate date;
    private String location;
    private Boolean online;
    private String status;  // UPCOMING, ONGOING, FINISHED
    private List<EquipmentStatusDto> equipments;
    private List<StallStatusDto> stalls;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EquipmentStatusDto {
        private Long id;
        private String name;
        private String type;
        private String status;  // AVAILABLE, IN_USE, MAINTENANCE
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StallStatusDto {
        private Long id;
        private String name;
        private Integer number;
        private String location;
        private String status;  // ASSIGNED, AVAILABLE
    }
}
