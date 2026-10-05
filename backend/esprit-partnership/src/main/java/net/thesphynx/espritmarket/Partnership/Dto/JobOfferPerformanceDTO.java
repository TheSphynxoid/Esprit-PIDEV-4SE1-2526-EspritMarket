package net.thesphynx.espritmarket.Partnership.Dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class JobOfferPerformanceDTO {
    private Long jobOfferId;
    private String title;
    private String type;
    private String status;
    private String companyName;
    private Long applicationCount;
    private Double averageMatchingScore;
    private Double interviewCompletionRate;

    public JobOfferPerformanceDTO(Long jobOfferId, String title, String type, String status, String companyName,
                                  Long applicationCount, Double averageMatchingScore,
                                  Long totalInterviews, Long completedInterviews) {
        this.jobOfferId = jobOfferId;
        this.title = title;
        this.type = type;
        this.status = status;
        this.companyName = companyName;
        this.applicationCount = applicationCount != null ? applicationCount : 0L;
        
        // Round to 1 decimal place if exists
        if (averageMatchingScore != null) {
            this.averageMatchingScore = Math.round(averageMatchingScore * 10.0) / 10.0;
        } else {
            this.averageMatchingScore = 0.0;
        }
        
        // Calculate rate based on completed vs total interviews for this job offer
        if (totalInterviews != null && totalInterviews > 0) {
            double rate = (completedInterviews != null ? completedInterviews.doubleValue() : 0.0) / totalInterviews * 100.0;
            this.interviewCompletionRate = Math.round(rate * 10.0) / 10.0;
        } else {
            this.interviewCompletionRate = 0.0;
        }
    }
}
