package net.thesphynx.espritmarket.Partnership.Dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.thesphynx.espritmarket.Partnership.Entity.InterviewResult;
import net.thesphynx.espritmarket.Partnership.Entity.InterviewStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewResponse {
    private Long id;
    private LocalDateTime interviewDate;
    private String type;
    private String location;
    private InterviewStatus status;
    private InterviewResult result;
    private String resultNotes;
    private Long applicationId;
    private String studentFirstName;
    private String studentLastName;
    private String jobTitle;
}