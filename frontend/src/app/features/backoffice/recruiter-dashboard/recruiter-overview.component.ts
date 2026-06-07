import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PartnershipApiService } from '../../../services/api/partnership-api.service';
import { JobOfferPerformanceDTO } from '../../../services/api/models/api-resource.model';

@Component({
  selector: 'app-recruiter-overview',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './recruiter-overview.component.html',
  styleUrls: ['./recruiter-overview.component.css']
})
export class RecruiterOverviewComponent implements OnInit {
  private readonly partnershipApi = inject(PartnershipApiService);

  performanceReports: JobOfferPerformanceDTO[] = [];
  loading = true;
  errorMessage = '';

  // Aggregated metrics
  totalJobOffers = 0;
  totalApplications = 0;
  avgMatchingScoreOverall = 0;
  avgCompletionRateOverall = 0;

  ngOnInit(): void {
    this.loadPerformanceData();
  }

  private loadPerformanceData(): void {
    this.loading = true;
    this.errorMessage = '';
    
    this.partnershipApi.getJobOfferPerformanceReport().subscribe({
      next: (data) => {
        this.performanceReports = data;
        this.calculateAggregates(data);
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = 'Failed to load performance report.';
        this.loading = false;
        console.error('Performance report error:', err);
      }
    });
  }

  private calculateAggregates(data: JobOfferPerformanceDTO[]): void {
    if (!data || data.length === 0) {
      this.totalJobOffers = 0;
      this.totalApplications = 0;
      this.avgMatchingScoreOverall = 0;
      this.avgCompletionRateOverall = 0;
      return;
    }

    this.totalJobOffers = data.length;
    
    let sumApps = 0;
    let sumScore = 0;
    let sumRate = 0;

    data.forEach(item => {
      sumApps += item.applicationCount || 0;
      sumScore += item.averageMatchingScore || 0;
      sumRate += item.interviewCompletionRate || 0;
    });

    this.totalApplications = sumApps;
    this.avgMatchingScoreOverall = Math.round((sumScore / data.length) * 10) / 10;
    this.avgCompletionRateOverall = Math.round((sumRate / data.length) * 10) / 10;
  }
}
