package net.thesphynx.espritmarket.Delivery.Service;

import net.thesphynx.espritmarket.Common.Entity.Role;
import net.thesphynx.espritmarket.Common.Entity.User;
import net.thesphynx.espritmarket.Common.Repository.UserRepository;
import net.thesphynx.espritmarket.Delivery.Dto.QuizResultResponse;
import net.thesphynx.espritmarket.Delivery.Dto.QuizSubmitRequest;
import net.thesphynx.espritmarket.Delivery.Entity.QuizResult;
import net.thesphynx.espritmarket.Delivery.Entity.QuizStatus;
import net.thesphynx.espritmarket.Delivery.Exception.QuizAccessException;
import net.thesphynx.espritmarket.Delivery.Repository.IQuizResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @Mock
    private IQuizResultRepository quizResultRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private QuizService service;

    private User buildCourierUser() {
        var user = new User();
        user.setId(1L);
        user.setName("Courier Test");
        user.setRole(Role.COURIER);
        return user;
    }

    @Test
    void submitQuiz_withPassingScore_shouldAccept() {
        var user = buildCourierUser();
        var request = QuizSubmitRequest.builder().userId(1L).score(90).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(quizResultRepository.findTopByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(Optional.empty());
        when(quizResultRepository.save(any(QuizResult.class))).thenAnswer(inv -> {
            QuizResult r = inv.getArgument(0);
            r.setId(100L);
            r.setSubmittedAt(LocalDateTime.now());
            return r;
        });

        var result = service.submitQuiz(request);

        assertNotNull(result);
        assertTrue(result.getPassed());
        verify(quizResultRepository).save(any(QuizResult.class));
    }

    @Test
    void submitQuiz_withFailingScore_shouldReject() {
        var user = buildCourierUser();
        var request = QuizSubmitRequest.builder().userId(1L).score(50).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(quizResultRepository.findTopByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(Optional.empty());
        when(quizResultRepository.save(any(QuizResult.class))).thenAnswer(inv -> {
            QuizResult r = inv.getArgument(0);
            r.setId(101L);
            r.setSubmittedAt(LocalDateTime.now());
            return r;
        });

        var result = service.submitQuiz(request);

        assertNotNull(result);
        assertEquals(QuizStatus.REJECTED, result.getStatus());
    }

    @Test
    void submitQuiz_whenUserNotFound_shouldThrow() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        var request = QuizSubmitRequest.builder().userId(99L).score(80).build();

        var ex = assertThrows(QuizAccessException.class, () -> service.submitQuiz(request));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void submitQuiz_whenNotCourier_shouldThrow() {
        var user = new User();
        user.setId(1L);
        user.setRole(Role.USER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        var request = QuizSubmitRequest.builder().userId(1L).score(80).build();

        var ex = assertThrows(QuizAccessException.class, () -> service.submitQuiz(request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void submitQuiz_whenAlreadyAccepted_shouldThrow() {
        var user = buildCourierUser();
        var prev = new QuizResult();
        prev.setUser(user);
        prev.setStatus(QuizStatus.ACCEPTED_QUIZ);
        prev.setScore(90);
        prev.setSubmittedAt(LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(quizResultRepository.findTopByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(Optional.of(prev));

        var request = QuizSubmitRequest.builder().userId(1L).score(85).build();

        var ex = assertThrows(QuizAccessException.class, () -> service.submitQuiz(request));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void submitQuiz_whenRejectedWithin24h_shouldThrow() {
        var user = buildCourierUser();
        var prev = new QuizResult();
        prev.setUser(user);
        prev.setStatus(QuizStatus.REJECTED);
        prev.setScore(40);
        prev.setSubmittedAt(LocalDateTime.now().minusHours(1));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(quizResultRepository.findTopByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(Optional.of(prev));

        var request = QuizSubmitRequest.builder().userId(1L).score(85).build();

        var ex = assertThrows(QuizAccessException.class, () -> service.submitQuiz(request));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, ex.getStatus());
    }

    @Test
    void submitQuiz_whenRejectedAfter24h_shouldAllowRetake() {
        var user = buildCourierUser();
        var prev = new QuizResult();
        prev.setUser(user);
        prev.setStatus(QuizStatus.REJECTED);
        prev.setScore(40);
        prev.setSubmittedAt(LocalDateTime.now().minusHours(25));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(quizResultRepository.findTopByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(Optional.of(prev));
        when(quizResultRepository.save(any(QuizResult.class))).thenAnswer(inv -> {
            QuizResult r = inv.getArgument(0);
            r.setId(102L);
            r.setSubmittedAt(LocalDateTime.now());
            return r;
        });

        var request = QuizSubmitRequest.builder().userId(1L).score(85).build();
        var result = service.submitQuiz(request);

        assertNotNull(result);
        assertTrue(result.getPassed());
    }

    @Test
    void getQuizResult_whenFound_shouldReturn() {
        var user = buildCourierUser();
        var quizResult = new QuizResult();
        quizResult.setId(1L);
        quizResult.setUser(user);
        quizResult.setStatus(QuizStatus.ACCEPTED_QUIZ);
        quizResult.setScore(90);
        quizResult.setSubmittedAt(LocalDateTime.now());

        when(quizResultRepository.findTopByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(Optional.of(quizResult));

        var result = service.getQuizResult(1L);

        assertNotNull(result);
        assertTrue(result.isPresent());
    }

    @Test
    void getQuizResult_whenMissing_shouldReturnEmpty() {
        when(quizResultRepository.findTopByUserIdOrderBySubmittedAtDesc(99L)).thenReturn(Optional.empty());

        var result = service.getQuizResult(99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getResultsByStatus_shouldReturnFilteredList() {
        var user = buildCourierUser();
        var r1 = new QuizResult();
        r1.setId(1L);
        r1.setUser(user);
        r1.setStatus(QuizStatus.ACCEPTED_QUIZ);
        r1.setScore(90);
        r1.setSubmittedAt(LocalDateTime.now());

        when(quizResultRepository.findByStatus(QuizStatus.ACCEPTED_QUIZ)).thenReturn(List.of(r1));

        var result = service.getResultsByStatus(QuizStatus.ACCEPTED_QUIZ);

        assertEquals(1, result.size());
    }

    @Test
    void getAcceptedCandidates_shouldReturnAccepted() {
        var user = buildCourierUser();
        var r1 = new QuizResult();
        r1.setId(1L);
        r1.setUser(user);
        r1.setStatus(QuizStatus.ACCEPTED_QUIZ);
        r1.setScore(90);
        r1.setSubmittedAt(LocalDateTime.now());

        when(quizResultRepository.findByStatus(QuizStatus.ACCEPTED_QUIZ)).thenReturn(List.of(r1));

        var result = service.getAcceptedCandidates();

        assertEquals(1, result.size());
    }

    @Test
    void getRejectedCandidates_shouldReturnRejected() {
        var user = buildCourierUser();
        var r1 = new QuizResult();
        r1.setId(1L);
        r1.setUser(user);
        r1.setStatus(QuizStatus.REJECTED);
        r1.setScore(50);
        r1.setSubmittedAt(LocalDateTime.now().minusHours(25));

        when(quizResultRepository.findByStatus(QuizStatus.REJECTED)).thenReturn(List.of(r1));

        var result = service.getRejectedCandidates();

        assertEquals(1, result.size());
    }
}
