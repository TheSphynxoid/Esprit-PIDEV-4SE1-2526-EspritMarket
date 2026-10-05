package net.thesphynx.espritmarket.Delivery.Controller;

import net.thesphynx.espritmarket.Delivery.Dto.ApiErrorResponse;
import net.thesphynx.espritmarket.Delivery.Dto.AdminInterviewDayResponse;
import net.thesphynx.espritmarket.Delivery.Dto.AdminInterviewWeekResponse;
import net.thesphynx.espritmarket.Delivery.Dto.InterviewBookingRequest;
import net.thesphynx.espritmarket.Delivery.Dto.InterviewCalendarResponse;
import net.thesphynx.espritmarket.Delivery.Dto.QuizResultResponse;
import net.thesphynx.espritmarket.Delivery.Dto.QuizSubmitRequest;
import net.thesphynx.espritmarket.Delivery.Entity.QuizStatus;
import net.thesphynx.espritmarket.Delivery.Exception.QuizAccessException;
import net.thesphynx.espritmarket.Delivery.Service.QuizService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/quiz")
@CrossOrigin(origins = "*", maxAge = 3600)
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    /**
     * POST /api/quiz/submit
     * Soumettre le quiz avec le score
     * ⚠️ Restreint au rôle COURIER uniquement
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitQuiz(@RequestBody QuizSubmitRequest request) {
        if (request.getUserId() == null || request.getScore() == null) {
            return ResponseEntity.badRequest().body(ApiErrorResponse.builder()
                    .message("userId et score sont obligatoires")
                    .timestamp(LocalDateTime.now())
                    .build());
        }

        if (request.getScore() < 0 || request.getScore() > 100) {
            return ResponseEntity.badRequest().body(ApiErrorResponse.builder()
                    .message("Le score doit être entre 0 et 100")
                    .timestamp(LocalDateTime.now())
                    .build());
        }

        try {
            QuizResultResponse response = quizService.submitQuiz(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (QuizAccessException e) {
            return ResponseEntity.status(e.getStatus()).body(ApiErrorResponse.builder()
                    .message(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    /**
     * GET /api/quiz/{userId}
     * Récupérer le résultat du quiz pour un utilisateur
     */
    @GetMapping("/{userId}")
    public ResponseEntity<QuizResultResponse> getQuizResult(@PathVariable Long userId) {
        Optional<QuizResultResponse> result = quizService.getQuizResult(userId);
        return result.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * GET /api/quiz/{userId}/recent
     * Récupérer le dernier résultat de quiz
     */
    @GetMapping("/{userId}/recent")
    public ResponseEntity<QuizResultResponse> getMostRecentQuizResult(@PathVariable Long userId) {
        Optional<QuizResultResponse> result = quizService.getMostRecentQuizResult(userId);
        return result.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * GET /api/quiz/status/accepted
     * Récupérer tous les utilisateurs acceptés
     */
    @GetMapping("/status/accepted")
    public ResponseEntity<List<QuizResultResponse>> getAcceptedCandidates() {
        List<QuizResultResponse> results = quizService.getAcceptedCandidates();
        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/quiz/status/rejected
     * Récupérer tous les utilisateurs rejetés
     */
    @GetMapping("/status/rejected")
    public ResponseEntity<List<QuizResultResponse>> getRejectedCandidates() {
        List<QuizResultResponse> results = quizService.getRejectedCandidates();
        return ResponseEntity.ok(results);
    }

    /**
     * GET /api/quiz/status/{status}
     * Récupérer les résultats par statut
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<QuizResultResponse>> getResultsByStatus(@PathVariable String status) {
        try {
            QuizStatus quizStatus = QuizStatus.valueOf(status.toUpperCase());
            List<QuizResultResponse> results = quizService.getResultsByStatus(quizStatus);
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/interview/calendar")
    public ResponseEntity<InterviewCalendarResponse> getInterviewCalendar() {
        return ResponseEntity.ok(quizService.getInterviewCalendar());
    }

    @PostMapping("/interview/book")
    public ResponseEntity<?> bookInterview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody InterviewBookingRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiErrorResponse.builder()
                    .message("Authentification requise")
                    .timestamp(LocalDateTime.now())
                    .build());
        }

        try {
            QuizResultResponse response = quizService.bookInterviewSlot(userDetails.getUsername(), request);
            return ResponseEntity.ok(response);
        } catch (QuizAccessException e) {
            return ResponseEntity.status(e.getStatus()).body(ApiErrorResponse.builder()
                    .message(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    @GetMapping("/admin/interviews/week")
    @PreAuthorize("hasRole('ADMIN_DELIVERY')")
    public ResponseEntity<AdminInterviewWeekResponse> getAdminInterviewWeek(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(quizService.getAdminInterviewWeek(date));
    }

    @GetMapping("/admin/interviews/today")
    @PreAuthorize("hasRole('ADMIN_DELIVERY')")
    public ResponseEntity<AdminInterviewDayResponse> getAdminTodayInterviews() {
        return ResponseEntity.ok(quizService.getAdminTodayInterviews());
    }

    @GetMapping("/admin/interviews/day")
    @PreAuthorize("hasRole('ADMIN_DELIVERY')")
    public ResponseEntity<AdminInterviewDayResponse> getAdminInterviewsByDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(quizService.getAdminInterviewDay(date));
    }
}
