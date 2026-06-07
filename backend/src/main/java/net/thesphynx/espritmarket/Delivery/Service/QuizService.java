package net.thesphynx.espritmarket.Delivery.Service;

import net.thesphynx.espritmarket.Common.Entity.Role;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Delivery.Dto.InterviewBookingRequest;
import net.thesphynx.espritmarket.Delivery.Dto.InterviewCalendarResponse;
import net.thesphynx.espritmarket.Delivery.Dto.InterviewDayScheduleResponse;
import net.thesphynx.espritmarket.Delivery.Dto.InterviewSlotResponse;
import net.thesphynx.espritmarket.Delivery.Dto.AdminInterviewDayResponse;
import net.thesphynx.espritmarket.Delivery.Dto.AdminInterviewWeekResponse;
import net.thesphynx.espritmarket.Delivery.Dto.QuizResultResponse;
import net.thesphynx.espritmarket.Delivery.Dto.QuizSubmitRequest;
import net.thesphynx.espritmarket.Delivery.Entity.QuizResult;
import net.thesphynx.espritmarket.Delivery.Entity.QuizStatus;
import net.thesphynx.espritmarket.Delivery.Exception.QuizAccessException;
import net.thesphynx.espritmarket.Delivery.Repository.IQuizResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    private static final Integer PASSING_SCORE = 80;
    private static final long REJECTED_RETRY_DELAY_HOURS = 24;
    private static final int INTERVIEW_DURATION_MINUTES = 30;
    private static final int BREAK_DURATION_MINUTES = 5;
    private static final LocalTime DAY_START = LocalTime.of(9, 30);
    private static final LocalTime LUNCH_START = LocalTime.of(12, 20);
    private static final LocalTime LUNCH_END = LocalTime.of(13, 0);
    private static final LocalTime DAY_LAST_START = LocalTime.of(17, 40);
    private static final int MAX_WEEK_LOOKAHEAD = 12;

    private final IQuizResultRepository quizResultRepository;
    private final UserRepository userRepository;
    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username:no-reply@espritmarket.local}")
    private String quizMailFrom;

    public QuizService(IQuizResultRepository quizResultRepository,
                       UserRepository userRepository,
                       JavaMailSender javaMailSender) {
        this.quizResultRepository = quizResultRepository;
        this.userRepository = userRepository;
        this.javaMailSender = javaMailSender;
    }

    /**
     * Submit the quiz and calculate the score
     * If score >= 80%: create meeting link and status = ACCEPTED_QUIZ
     * Otherwise: status = REJECTED
     * Restricted to users with the COURIER role only
     */
    public QuizResultResponse submitQuiz(QuizSubmitRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new QuizAccessException(HttpStatus.NOT_FOUND, "User not found"));

        // Verify that the user has the COURIER role
        if (user.getRole() != Role.COURIER) {
            throw new QuizAccessException(HttpStatus.FORBIDDEN,
                    "Only users with the COURIER role can take the quiz");
        }

        Optional<QuizResult> latestResultOpt = quizResultRepository.findTopByUserIdOrderBySubmittedAtDesc(user.getId());
        if (latestResultOpt.isPresent()) {
            QuizResult latestResult = latestResultOpt.get();

            if (latestResult.getStatus() == QuizStatus.ACCEPTED_QUIZ) {
                throw new QuizAccessException(HttpStatus.CONFLICT,
                        "Quiz already approved. You cannot retake it.");
            }

            if (latestResult.getStatus() == QuizStatus.REJECTED) {
                LocalDateTime nextAllowedAttemptAt = latestResult.getSubmittedAt().plusHours(REJECTED_RETRY_DELAY_HOURS);
                if (LocalDateTime.now().isBefore(nextAllowedAttemptAt)) {
                    Duration remaining = Duration.between(LocalDateTime.now(), nextAllowedAttemptAt);
                    long remainingMinutes = Math.max(1, remaining.toMinutes());
                    throw new QuizAccessException(HttpStatus.TOO_MANY_REQUESTS,
                            "Quiz rejected. You can retry after 24h. Remaining time: "
                                    + remainingMinutes + " minutes.");
                }
            }
        }

        QuizResult result = new QuizResult();
        result.setUser(user);
        result.setScore(request.getScore());

        if (request.getScore() >= PASSING_SCORE) {
            result.setStatus(QuizStatus.ACCEPTED_QUIZ);
            // meeting link (example Jitsi)
            String meetingLink = generateMeetingLink(user.getId());
            result.setMeetingLink(meetingLink);
        } else {
            result.setStatus(QuizStatus.REJECTED);
        }

        QuizResult saved = quizResultRepository.save(result);
        sendQuizResultEmail(saved);
        return mapToResponse(saved, request.getScore() >= PASSING_SCORE);
    }

    /**
     * Generate a unique meeting link
     */
    private String generateMeetingLink(Long userId) {
        return "https://meet.jit.si/livreur-" + userId + "-" + System.currentTimeMillis();
    }

    /**
     * Get the quiz result for a user
     */
    public Optional<QuizResultResponse> getQuizResult(Long userId) {
        return quizResultRepository.findTopByUserIdOrderBySubmittedAtDesc(userId)
                .map(result -> mapToResponse(result, result.getStatus() == QuizStatus.ACCEPTED_QUIZ));
    }

    /**
     * Get the most recent quiz result for a user
     */
    public Optional<QuizResultResponse> getMostRecentQuizResult(Long userId) {
        return quizResultRepository.findTopByUserIdOrderBySubmittedAtDesc(userId)
                .map(result -> mapToResponse(result, result.getStatus() == QuizStatus.ACCEPTED_QUIZ));
    }

    /**
     * Get all quiz results with a given status
     */
    public List<QuizResultResponse> getResultsByStatus(QuizStatus status) {
        return quizResultRepository.findByStatus(status)
                .stream()
                .map(result -> mapToResponse(result, result.getStatus() == QuizStatus.ACCEPTED_QUIZ))
                .collect(Collectors.toList());
    }

    /**
     * Get all users who passed the quiz
     */
    public List<QuizResultResponse> getAcceptedCandidates() {
        return getResultsByStatus(QuizStatus.ACCEPTED_QUIZ);
    }

    /**
     * Get all users who were rejected
     */
    public List<QuizResultResponse> getRejectedCandidates() {
        return getResultsByStatus(QuizStatus.REJECTED);
    }

    @Transactional(readOnly = true)
    public AdminInterviewWeekResponse getAdminInterviewWeek(LocalDate referenceDate) {
        LocalDate effectiveDate = referenceDate != null ? referenceDate : LocalDate.now();
        LocalDate weekStart = resolveAdminWeekStart(effectiveDate);
        return buildAdminWeekResponse(weekStart);
    }

    @Transactional(readOnly = true)
    public AdminInterviewDayResponse getAdminInterviewDay(LocalDate date) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        return buildAdminDayResponse(effectiveDate);
    }

    @Transactional(readOnly = true)
    public AdminInterviewDayResponse getAdminTodayInterviews() {
        return getAdminInterviewDay(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public InterviewCalendarResponse getInterviewCalendar() {
        LocalDate referenceDate = LocalDate.now();
        if (referenceDate.getDayOfWeek() == DayOfWeek.SATURDAY) {
            referenceDate = referenceDate.plusDays(2);
        } else if (referenceDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            referenceDate = referenceDate.plusDays(1);
        }

        LocalDate candidateWeekStart = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (int weekIndex = 0; weekIndex < MAX_WEEK_LOOKAHEAD; weekIndex++) {
            LocalDate weekStart = candidateWeekStart.plusWeeks(weekIndex);
            InterviewCalendarResponse response = buildCalendarForWeek(weekStart);
            if (hasAvailableSlot(response)) {
                return response;
            }
        }

        throw new QuizAccessException(HttpStatus.CONFLICT,
                "No interview slot available in the coming weeks.");
    }

    public QuizResultResponse bookInterviewSlot(String email, InterviewBookingRequest request) {
        if (request == null || request.getInterviewDate() == null) {
            throw new QuizAccessException(HttpStatus.BAD_REQUEST, "interviewDate is required");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new QuizAccessException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() != Role.COURIER) {
            throw new QuizAccessException(HttpStatus.FORBIDDEN,
                    "Only users with the COURIER role can book an interview");
        }

        QuizResult acceptedResult = quizResultRepository.findTopByUserIdAndStatusOrderBySubmittedAtDesc(
                        user.getId(),
                        QuizStatus.ACCEPTED_QUIZ)
                .orElseThrow(() -> new QuizAccessException(HttpStatus.CONFLICT,
                        "The quiz must be accepted before booking an interview"));

        LocalDateTime interviewDate = request.getInterviewDate();
        validateInterviewSlot(interviewDate);

        if (acceptedResult.getInterviewScheduledAt() != null) {
            throw new QuizAccessException(HttpStatus.CONFLICT,
                    "An interview is already booked for this courier");
        }

        LocalDate weekStart = interviewDate.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime weekStartDateTime = weekStart.atStartOfDay();
        LocalDateTime weekEndExclusive = weekStart.plusWeeks(1).atStartOfDay();

        Set<LocalDateTime> bookedSlots = quizResultRepository
                .findByStatusAndInterviewScheduledAtGreaterThanEqualAndInterviewScheduledAtLessThan(
                        QuizStatus.ACCEPTED_QUIZ,
                        weekStartDateTime,
                        weekEndExclusive)
                .stream()
                .map(QuizResult::getInterviewScheduledAt)
                .collect(Collectors.toSet());

        if (bookedSlots.contains(interviewDate)) {
            throw new QuizAccessException(HttpStatus.CONFLICT,
                    "This slot is already taken by another courier");
        }

        acceptedResult.setInterviewScheduledAt(interviewDate);
        QuizResult saved = quizResultRepository.save(acceptedResult);
        return mapToResponse(saved, true);
    }

    /**
     * Mapper QuizResult à QuizResultResponse
     */
    private QuizResultResponse mapToResponse(QuizResult result, boolean isPassed) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextAllowedAttemptAt = null;
        boolean canRetake;
        long remainingMinutesBeforeRetake = 0;

        if (result.getStatus() == QuizStatus.ACCEPTED_QUIZ) {
            canRetake = false;
        } else if (result.getStatus() == QuizStatus.REJECTED) {
            nextAllowedAttemptAt = result.getSubmittedAt().plusHours(REJECTED_RETRY_DELAY_HOURS);
            canRetake = !now.isBefore(nextAllowedAttemptAt);
            if (!canRetake) {
                remainingMinutesBeforeRetake = Math.max(1, Duration.between(now, nextAllowedAttemptAt).toMinutes());
            }
        } else {
            canRetake = true;
        }

        String message = isPassed
                ? "🎉 Congratulations! You passed the quiz. An interview will be scheduled soon."
                : canRetake
                    ? "❌ Sorry, you did not reach the required score (80%). You can try again now."
                    : "❌ Sorry, you did not reach the required score (80%). You can retry after 24h.";

        return QuizResultResponse.builder()
                .id(result.getId())
                .userId(result.getUser().getId())
                .userName(result.getUser().getName())
                .score(result.getScore())
                .status(result.getStatus())
                .message(message)
                .meetingLink(result.getMeetingLink())
                .submittedAt(result.getSubmittedAt())
                .interviewScheduledAt(result.getInterviewScheduledAt())
                .canRetake(canRetake)
                .nextAllowedAttemptAt(nextAllowedAttemptAt)
                .remainingMinutesBeforeRetake(remainingMinutesBeforeRetake)
                .passed(isPassed)
                .build();
    }

    private InterviewCalendarResponse buildCalendarForWeek(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(4);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekStartDateTime = weekStart.atStartOfDay();
        LocalDateTime weekEndExclusive = weekStart.plusWeeks(1).atStartOfDay();

        Set<LocalDateTime> bookedSlots = quizResultRepository
                .findByStatusAndInterviewScheduledAtGreaterThanEqualAndInterviewScheduledAtLessThan(
                        QuizStatus.ACCEPTED_QUIZ,
                        weekStartDateTime,
                        weekEndExclusive)
                .stream()
                .map(QuizResult::getInterviewScheduledAt)
                .collect(Collectors.toCollection(HashSet::new));

        List<InterviewDayScheduleResponse> days = new ArrayList<>();
        for (int dayOffset = 0; dayOffset < 5; dayOffset++) {
            LocalDate currentDate = weekStart.plusDays(dayOffset);
            List<InterviewSlotResponse> slots = buildSlotsForDay(currentDate, now, bookedSlots);
            days.add(InterviewDayScheduleResponse.builder()
                    .date(currentDate)
                    .dayLabel(currentDate.getDayOfWeek().name())
                    .slots(slots)
                    .build());
        }

        return InterviewCalendarResponse.builder()
                .weekStart(weekStart)
                .weekEnd(weekEnd)
                .days(days)
                .build();
    }

    private AdminInterviewWeekResponse buildAdminWeekResponse(LocalDate weekStart) {
        List<AdminInterviewDayResponse> days = new ArrayList<>();
        int totalInterviews = 0;

        for (int dayOffset = 0; dayOffset < 5; dayOffset++) {
            LocalDate currentDate = weekStart.plusDays(dayOffset);
            AdminInterviewDayResponse dayResponse = buildAdminDayResponse(currentDate);
            days.add(dayResponse);
            totalInterviews += dayResponse.getInterviewCount();
        }

        return AdminInterviewWeekResponse.builder()
                .weekStart(weekStart)
                .weekEnd(weekStart.plusDays(4))
                .totalInterviews(totalInterviews)
                .days(days)
                .build();
    }

    private AdminInterviewDayResponse buildAdminDayResponse(LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEndExclusive = date.plusDays(1).atStartOfDay();

        List<QuizResultResponse> interviews = quizResultRepository
                .findByStatusAndInterviewScheduledAtGreaterThanEqualAndInterviewScheduledAtLessThanOrderByInterviewScheduledAtAsc(
                        QuizStatus.ACCEPTED_QUIZ,
                        dayStart,
                        dayEndExclusive)
                .stream()
                .map(result -> mapToResponse(result, true))
                .toList();

        return AdminInterviewDayResponse.builder()
                .date(date)
                .dayLabel(date.getDayOfWeek().name())
                .interviewCount(interviews.size())
                .interviews(interviews)
                .build();
    }

    private LocalDate resolveAdminWeekStart(LocalDate referenceDate) {
        DayOfWeek dayOfWeek = referenceDate.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return referenceDate.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        }

        return referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private List<InterviewSlotResponse> buildSlotsForDay(LocalDate date, LocalDateTime now, Set<LocalDateTime> bookedSlots) {
        List<InterviewSlotResponse> slots = new ArrayList<>();
        LocalTime currentStart = DAY_START;

        while (!currentStart.isAfter(DAY_LAST_START)) {
            LocalDateTime slotStart = date.atTime(currentStart);
            LocalDateTime slotEnd = slotStart.plusMinutes(INTERVIEW_DURATION_MINUTES);

            if (slotStart.toLocalTime().isBefore(LUNCH_START) && slotEnd.toLocalTime().isAfter(LUNCH_START)) {
                currentStart = LUNCH_END;
                continue;
            }

            boolean isFuture = slotStart.isAfter(now);
            boolean isBooked = bookedSlots.contains(slotStart);

            slots.add(InterviewSlotResponse.builder()
                    .start(slotStart)
                    .end(slotEnd)
                    .available(isFuture && !isBooked)
                    .build());

            currentStart = currentStart.plusMinutes((long) INTERVIEW_DURATION_MINUTES + BREAK_DURATION_MINUTES);
            if (currentStart.isAfter(LUNCH_START) && currentStart.isBefore(LUNCH_END)) {
                currentStart = LUNCH_END;
            }
        }

        return slots;
    }

    private boolean hasAvailableSlot(InterviewCalendarResponse response) {
        return response.getDays().stream()
                .flatMap(day -> day.getSlots().stream())
                .anyMatch(InterviewSlotResponse::getAvailable);
    }

    private void sendQuizResultEmail(QuizResult result) {
        User user = result.getUser();
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Quiz result email skipped for result {} because user email is missing", result.getId());
            return;
        }

        boolean isAccepted = result.getStatus() == QuizStatus.ACCEPTED_QUIZ;
        String subject = isAccepted
                ? "Esprit Market - Quiz de recrutement accepte"
                : "Esprit Market - Quiz de recrutement refuse";
        String messageBody = isAccepted
                ? buildAcceptedQuizEmailBody(user.getName(), result.getScore())
                : buildRejectedQuizEmailBody(user.getName(), result.getScore());

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(quizMailFrom);
            message.setTo(user.getEmail());
            message.setSubject(subject);
            message.setText(messageBody);
            javaMailSender.send(message);
        } catch (Exception exception) {
            log.warn("Unable to send quiz result email for user {}: {}", user.getEmail(), exception.getMessage());
        }
    }

    private String buildAcceptedQuizEmailBody(String rawName, Integer score) {
        String displayName = buildDisplayName(rawName);
        return "Bonjour " + displayName + ",\n\n"
                + "Felicitation, votre quiz de recrutement a ete accepte.\n"
                + "Score obtenu: " + score + "/100\n\n"
                + "Vous pouvez maintenant reserver votre entretien depuis votre espace candidat.\n"
                + "Un lien de visioconference vous sera communique apres la planification.\n\n"
                + "Cordialement,\n"
                + "Equipe Esprit Market";
    }

    private String buildRejectedQuizEmailBody(String rawName, Integer score) {
        String displayName = buildDisplayName(rawName);
        return "Bonjour " + displayName + ",\n\n"
                + "Nous vous informons que votre quiz de recrutement a ete refuse.\n"
                + "Score obtenu: " + score + "/100\n\n"
                + "Vous pourrez retenter votre chance apres 24 heures.\n\n"
                + "Cordialement,\n"
                + "Equipe Esprit Market";
    }

    private String buildDisplayName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "candidat";
        }

        return rawName.trim().replaceAll("\\s+", " ");
    }

    private void validateInterviewSlot(LocalDateTime interviewDate) {
        if (interviewDate.getDayOfWeek() == DayOfWeek.SATURDAY || interviewDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new QuizAccessException(HttpStatus.BAD_REQUEST,
                    "Interviews are only available Monday through Friday");
        }

        LocalTime time = interviewDate.toLocalTime();
        if (!isAllowedSlotStart(time)) {
            throw new QuizAccessException(HttpStatus.BAD_REQUEST,
                    "The selected time does not match an available slot");
        }

        if (interviewDate.isBefore(LocalDateTime.now())) {
            throw new QuizAccessException(HttpStatus.BAD_REQUEST,
                    "The selected slot is already in the past");
        }
    }

    private boolean isAllowedSlotStart(LocalTime time) {
        if (time.equals(LUNCH_END)) {
            return true;
        }

        if (time.isBefore(DAY_START) || time.isAfter(DAY_LAST_START)) {
            return false;
        }

        LocalTime cursor = DAY_START;
        while (!cursor.isAfter(DAY_LAST_START)) {
            if (cursor.equals(time)) {
                return true;
            }

            cursor = cursor.plusMinutes((long) INTERVIEW_DURATION_MINUTES + BREAK_DURATION_MINUTES);
            if (cursor.isAfter(LUNCH_START) && cursor.isBefore(LUNCH_END)) {
                cursor = LUNCH_END;
            }
        }

        return false;
    }
}
