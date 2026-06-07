package net.thesphynx.espritmarket.EventPlanning.Service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.thesphynx.espritmarket.EventPlanning.Dto.TicketPromoOfferResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class TunisiaTicketDiscountService {

	private static final String CALENDAR_URL = "https://timesles.com/fr/calendar/years/2026/tunisia-212/";
	private static final String CALCULATORIAN_URL = "https://calculatorian.com/fr/time-and-date/year-calendar/tn/all/2026";
	private static final Pattern HOLIDAY_SECTION_PATTERN = Pattern.compile(
			"Calendrier des jours fériés et week-end de 2026 pour la Tunisie(?<section>.*?)Téléchargez et imprimez le calendrier PDF",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.DOTALL
	);
	private static final Pattern CALCULATORIAN_HOLIDAY_SECTION_PATTERN = Pattern.compile(
			"Jours fériés et fêtes en Tunisie 2026(?<section>.*?)Années avec le même calendrier que 2026",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.DOTALL
	);
	private static final Pattern PROMO_ENTRY_PATTERN = Pattern.compile(
			"(?iu)(?:lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)?\\s*(\\d{1,2})\\s+([A-Za-zÀ-ÿ.]+)\\s+(\\d{4})\\s+(.+?)\\s*\\((Jour Férié|Jour D'observation)\\)"
	);
	private static final Pattern DATE_PATTERN = Pattern.compile("\\b(\\d{1,2})\\s+([A-Za-zÀ-ÿ.]+)\\s+(\\d{4})\\b");
	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
			.followRedirects(HttpClient.Redirect.NORMAL)
			.connectTimeout(java.time.Duration.ofSeconds(5))
			.build();
	private static final List<String> FALLBACK_HOLIDAY_DATES = List.of(
			"2026-01-01",
			"2026-01-14",
			"2026-03-20",
			"2026-03-21",
			"2026-03-22",
			"2026-04-09",
			"2026-05-01",
			"2026-05-27",
			"2026-05-28",
			"2026-06-17"
	);

	private static final Set<String> MONTHS = Set.of(
			"janv", "janvier",
			"fevr", "fevrier", "févr", "février",
			"mars",
			"avr", "avril",
			"mai",
			"juin",
			"juil", "juillet",
			"aout", "août",
			"sept", "septembre",
			"oct", "octobre",
			"nov", "novembre",
			"dec", "déc", "decembre", "décembre"
	);
	private static final Set<String> AID_DATE_OVERRIDES = Set.of(
			"2026-03-20",
			"2026-03-21",
			"2026-03-22",
			"2026-05-27",
			"2026-05-28"
	);

	private static final List<TicketPromoOfferResponse> FALLBACK_PROMO_OFFERS = List.of(
			promo("2026-01-01", "Nouvel An", 0.10),
			promo("2026-01-14", "Fête de la Révolution et de la Jeunesse", 0.10),
			promo("2026-03-20", "Aïd al-Fitr", 0.20),
			promo("2026-03-21", "Vacances de l'Aïd Al Fitr", 0.20),
			promo("2026-03-22", "Vacances de l'Aïd Al Fitr", 0.20),
			promo("2026-04-09", "Journée des Martyrs", 0.10),
			promo("2026-05-01", "Fête du Travail", 0.10),
			promo("2026-05-27", "Aïd al-Adha", 0.20),
			promo("2026-05-28", "Vacances de l'Aïd al-Adha", 0.20),
			promo("2026-06-17", "Muharram", 0.10),
			promo("2026-07-25", "Fête de la République", 0.10),
			promo("2026-08-13", "Fête de la Femme et de la Famille", 0.10),
			promo("2026-10-15", "Fête de l'Évacuation", 0.10),
			promo("2026-12-17", "Revolution and Youth Day", 0.10)
	);

	private volatile List<TicketPromoOfferResponse> cachedPromoOffers = Collections.emptyList();

	@PostConstruct
	public void initialize() {
		refreshHolidayDates();
	}

	public synchronized void refreshHolidayDates() {
		try {
			cachedPromoOffers = Collections.unmodifiableList(loadPromoOffers());
			log.info("Loaded {} Tunisian holiday promo offers from fallback chain", cachedPromoOffers.size());
		} catch (Exception exception) {
			cachedPromoOffers = Collections.unmodifiableList(new ArrayList<>(FALLBACK_PROMO_OFFERS));
			log.warn("Failed to refresh Tunisian holiday dates from external sources; using fallback dates", exception);
		}
	}

	public List<String> getHolidayDates() {
		if (cachedPromoOffers.isEmpty()) {
			refreshHolidayDates();
		}
		return cachedPromoOffers.stream()
				.map(TicketPromoOfferResponse::getDate)
				.toList();
	}

	public List<TicketPromoOfferResponse> getPromoOffers() {
		if (cachedPromoOffers.isEmpty()) {
			refreshHolidayDates();
		}
		return cachedPromoOffers;
	}

	public boolean isHolidayDate(LocalDate date) {
		return getHolidayDates().contains(date.toString());
	}

	private List<TicketPromoOfferResponse> loadPromoOffers() throws IOException, InterruptedException {
		List<TicketPromoOfferResponse> promoOffers = extractPromoOffers(extractHolidaySection(fetchCalendarPage(CALENDAR_URL)));
		if (!promoOffers.isEmpty()) {
			return mergeWithFallbackOffers(promoOffers);
		}

		promoOffers = extractPromoOffers(extractCalculatorianHolidaySection(fetchCalendarPage(CALCULATORIAN_URL)));
		if (!promoOffers.isEmpty()) {
			return mergeWithFallbackOffers(promoOffers);
		}

		return new ArrayList<>(FALLBACK_PROMO_OFFERS);
	}

	private List<TicketPromoOfferResponse> mergeWithFallbackOffers(List<TicketPromoOfferResponse> offers) {
		Map<String, TicketPromoOfferResponse> mergedOffers = new LinkedHashMap<>();
		for (TicketPromoOfferResponse offer : offers) {
			mergedOffers.put(offer.getDate(), offer);
		}
		for (TicketPromoOfferResponse fallbackOffer : FALLBACK_PROMO_OFFERS) {
			mergedOffers.putIfAbsent(fallbackOffer.getDate(), fallbackOffer);
		}
		return new ArrayList<>(mergedOffers.values());
	}

	private String fetchCalendarPage(String url) throws IOException, InterruptedException {
		URI uri = URI.create(url);
		String host = uri.getHost();
		if (host == null || !(host.equals("timesles.com") || host.equals("calculatorian.com"))) {
			throw new IOException("Blocked external URL: " + host);
		}

		HttpRequest request = HttpRequest.newBuilder(uri)
				.header("User-Agent", "Mozilla/5.0")
				.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
				.GET()
				.build();

		HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (response.statusCode() < 200 || response.statusCode() >= 300) {
			throw new IOException("Unexpected response status: " + response.statusCode());
		}

		return response.body();
	}

	private String extractCalculatorianHolidaySection(String calendarPage) {
		Matcher sectionMatcher = CALCULATORIAN_HOLIDAY_SECTION_PATTERN.matcher(calendarPage);
		if (sectionMatcher.find()) {
			return sectionMatcher.group("section");
		}

		int start = calendarPage.indexOf("Jours fériés et fêtes en Tunisie 2026");
		int end = calendarPage.indexOf("Années avec le même calendrier que 2026");
		if (start >= 0 && end > start) {
			return calendarPage.substring(start, end);
		}

		return calendarPage;
	}

	private String extractHolidaySection(String calendarPage) {
		Matcher sectionMatcher = HOLIDAY_SECTION_PATTERN.matcher(calendarPage);
		if (sectionMatcher.find()) {
			return sectionMatcher.group("section");
		}

		int start = calendarPage.indexOf("Calendrier des jours fériés et week-end de 2026 pour la Tunisie");
		int end = calendarPage.indexOf("Téléchargez et imprimez le calendrier PDF");
		if (start >= 0 && end > start) {
			return calendarPage.substring(start, end);
		}

		return calendarPage;
	}

	private List<TicketPromoOfferResponse> extractPromoOffers(String section) {
		String normalizedSection = stripHtml(section);
		Map<String, TicketPromoOfferResponse> offers = new LinkedHashMap<>();
		Matcher matcher = PROMO_ENTRY_PATTERN.matcher(normalizedSection);

		while (matcher.find()) {
			LocalDate parsedDate = parseFrenchDate(matcher.group(1), matcher.group(2), matcher.group(3));
			if (parsedDate == null) {
				continue;
			}

			String label = normalizeLabel(matcher.group(4));
			double discountRate = resolveDiscountRate(label, parsedDate);
			offers.put(parsedDate.toString(), promo(parsedDate.toString(), label, discountRate));
		}

		return new ArrayList<>(offers.values());
	}

	private String stripHtml(String content) {
		return content
				.replaceAll("<script[\\s\\S]*?</script>", " ")
				.replaceAll("<style[\\s\\S]*?</style>", " ")
				.replaceAll("<[^>]+>", " ")
				.replace("&nbsp;", " ")
				.replace("&amp;", "&")
				.replace("&#39;", "'")
				.replace("&apos;", "'")
				.replace("&eacute;", "é")
				.replace("&Eacute;", "É")
				.replaceAll("\\s+", " ")
				.trim();
	}

	private String normalizeLabel(String label) {
		return label
				.replaceAll("\\[.*?\\]", " ")
				.replaceAll("\\([^\\)]*\\)", " ")
				.replaceAll("\\s+", " ")
				.trim();
	}

	private double resolveDiscountRate(String label, LocalDate date) {
		String normalized = label.toLowerCase(Locale.ROOT)
				.replace("’", "'")
				.replace("'", "")
				.replace("é", "e")
				.replace("è", "e")
				.replace("ê", "e")
				.replace("à", "a")
				.replace("â", "a")
				.replace("ô", "o")
				.replace("ï", "i")
				.replace("î", "i");

		if (normalized.contains("aid") || normalized.contains("eid") || normalized.contains("aid al") || AID_DATE_OVERRIDES.contains(date.toString())) {
			return 0.20;
		}

		return 0.10;
	}

	private static TicketPromoOfferResponse promo(String date, String label, double discountRate) {
		return TicketPromoOfferResponse.builder()
				.date(date)
				.label(label)
				.discountRate(discountRate)
				.discountLabel(discountRate >= 0.20 ? "Aid" : "Fête")
				.build();
	}

	private List<String> extractHolidayDates(String section) {
		Set<LocalDate> dates = new LinkedHashSet<>();
		Matcher matcher = DATE_PATTERN.matcher(section);

		while (matcher.find()) {
			LocalDate parsedDate = parseFrenchDate(matcher.group(1), matcher.group(2), matcher.group(3));
			if (parsedDate != null) {
				dates.add(parsedDate);
			}
		}

		return dates.stream()
				.map(LocalDate::toString)
				.sorted()
				.toList();
	}

	private LocalDate parseFrenchDate(String dayValue, String monthValue, String yearValue) {
		try {
			int day = Integer.parseInt(dayValue);
			int year = Integer.parseInt(yearValue);
			Integer month = resolveMonth(monthValue);
			if (month == null) {
				return null;
			}

			return LocalDate.of(year, month, day);
		} catch (IllegalArgumentException exception) {
			log.debug("Ignoring unsupported holiday date token: {} {} {}", dayValue, monthValue, yearValue);
			return null;
		}
	}

	private Integer resolveMonth(String monthValue) {
		String normalized = monthValue.toLowerCase(Locale.ROOT)
				.replace(".", "")
				.replace("é", "e")
				.replace("è", "e")
				.replace("ê", "e")
				.replace("à", "a")
				.replace("â", "a")
				.replace("ù", "u")
				.replace("û", "u")
				.replace("î", "i")
				.replace("ô", "o");

		if (normalized.startsWith("janv") || normalized.equals("janvier")) {
			return 1;
		}
		if (normalized.startsWith("fevr") || normalized.equals("fevrier")) {
			return 2;
		}
		if (normalized.startsWith("mars")) {
			return 3;
		}
		if (normalized.startsWith("avr")) {
			return 4;
		}
		if (normalized.startsWith("mai")) {
			return 5;
		}
		if (normalized.startsWith("juin")) {
			return 6;
		}
		if (normalized.startsWith("juil")) {
			return 7;
		}
		if (normalized.startsWith("aout")) {
			return 8;
		}
		if (normalized.startsWith("sept")) {
			return 9;
		}
		if (normalized.startsWith("oct")) {
			return 10;
		}
		if (normalized.startsWith("nov")) {
			return 11;
		}
		if (normalized.startsWith("dec")) {
			return 12;
		}

		return null;
	}
}
