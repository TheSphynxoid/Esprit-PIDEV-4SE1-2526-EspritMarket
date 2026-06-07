package net.thesphynx.espritmarket.Delivery.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.thesphynx.espritmarket.Delivery.Entity.QuizStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizResultResponse {

    private Long id;
    private Long userId;
    private String userName;
    private Integer score;
    private QuizStatus status;
    private String message;
    private String meetingLink;
    private LocalDateTime submittedAt;
    private LocalDateTime interviewScheduledAt;
    private Boolean canRetake;
    private LocalDateTime nextAllowedAttemptAt;
    private Long remainingMinutesBeforeRetake;

    @JsonProperty("isPassed")
    private Boolean passed;
}
