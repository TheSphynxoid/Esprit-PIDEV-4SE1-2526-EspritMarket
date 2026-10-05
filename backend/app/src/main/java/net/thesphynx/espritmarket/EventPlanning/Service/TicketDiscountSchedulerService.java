package net.thesphynx.espritmarket.EventPlanning.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketDiscountSchedulerService {

	private final TunisiaTicketDiscountService tunisiaTicketDiscountService;

	@Scheduled(cron = "0 0 1 * * *")
	public void refreshHolidayDates() {
		log.info("Refreshing Tunisian holiday dates cache");
		tunisiaTicketDiscountService.refreshHolidayDates();
	}
}
