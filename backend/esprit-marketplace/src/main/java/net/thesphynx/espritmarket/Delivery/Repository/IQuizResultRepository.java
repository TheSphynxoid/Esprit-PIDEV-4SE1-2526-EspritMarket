package net.thesphynx.espritmarket.Delivery.Repository;

import net.thesphynx.espritmarket.Delivery.Entity.QuizResult;
import net.thesphynx.espritmarket.Delivery.Entity.QuizStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IQuizResultRepository extends JpaRepository<QuizResult, Long> {

    Optional<QuizResult> findByUserId(Long userId);

    List<QuizResult> findByStatus(QuizStatus status);

    List<QuizResult> findByScoreGreaterThanEqual(Integer score);

    Optional<QuizResult> findTopByUserIdOrderBySubmittedAtDesc(Long userId);

    Optional<QuizResult> findTopByUserIdAndStatusOrderBySubmittedAtDesc(Long userId, QuizStatus status);

    List<QuizResult> findByStatusAndInterviewScheduledAtGreaterThanEqualAndInterviewScheduledAtLessThan(
            QuizStatus status,
            LocalDateTime interviewScheduledAtStart,
            LocalDateTime interviewScheduledAtEndExclusive);

        List<QuizResult> findByStatusAndInterviewScheduledAtGreaterThanEqualAndInterviewScheduledAtLessThanOrderByInterviewScheduledAtAsc(
            QuizStatus status,
            LocalDateTime interviewScheduledAtStart,
            LocalDateTime interviewScheduledAtEndExclusive);

            List<QuizResult> findByStatusAndInterviewScheduledAtGreaterThanEqualAndInterviewScheduledAtLessThanAndInterviewReminderSentAtIsNullOrderByInterviewScheduledAtAsc(
                QuizStatus status,
                LocalDateTime interviewScheduledAtStart,
                LocalDateTime interviewScheduledAtEndExclusive);
}
