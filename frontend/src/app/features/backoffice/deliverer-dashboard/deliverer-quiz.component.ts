import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, computed, inject, input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

import { QuizResultResponse, QuizService, InterviewCalendarResponse, InterviewDayScheduleResponse, InterviewSlotResponse } from '../../../services';

interface QuizQuestion {
  id: number;
  question: string;
  options: string[];
  correctAnswer: number;
  userAnswer: number | null;
}

const QUIZ_QUESTIONS: QuizQuestion[] = [
  {
    id: 1,
    question: 'What is the standard delivery time limit?',
    options: ['30 minutes', '1 hour', '2 hours', '4 hours'],
    correctAnswer: 1,
    userAnswer: null
  },
  {
    id: 2,
    question: 'What should you do if you have an accident?',
    options: ['Continue', 'Call the police', 'Call the service', 'Stop everything'],
    correctAnswer: 1,
    userAnswer: null
  },
  {
    id: 3,
    question: 'What is the expected behavior with clients?',
    options: ['Ignoring', 'Polite and professional', 'Fast without speaking', 'Critical'],
    correctAnswer: 1,
    userAnswer: null
  },
  {
    id: 4,
    question: 'How to handle a damaged order?',
    options: ['Throw it away', 'Report immediately', 'Ignore it', 'Deduct from salary'],
    correctAnswer: 1,
    userAnswer: null
  },
  {
    id: 5,
    question: "What is the primary concern during a delivery?",
    options: ['Maximum speed', 'Safety and quality', 'Minimum cost', 'Profit'],
    correctAnswer: 1,
    userAnswer: null
  },
  {
  id: 6,
  question: 'What to do if you are late for a delivery?',
  options: ['Ignore', 'Cancel', 'Inform the client', 'Wait'],
  correctAnswer: 2,
  userAnswer: null
},
{
  id: 7,
  question: 'How to verify an order before delivery?',
  options: ['Do not verify', 'Verify the content and address', 'Only check the name', 'Ignore'],
  correctAnswer: 1,
  userAnswer: null
},
{
  id: 8,
  question: 'What equipment is important for a deliverer?',
  options: ['Helmet and gloves', 'Phone only', 'Empty bag', 'None'],
  correctAnswer: 0,
  userAnswer: null
},
{
  id: 9,
  question: 'What to do if the client refuses the order?',
  options: ['Force the client', 'Cancel and report', 'Leave without saying anything', 'Keep the package'],
  correctAnswer: 1,
  userAnswer: null
},
{
  id: 10,
  question: 'Why is it important to follow the GPS?',
  options: ['To waste time', 'To optimize the route', 'To ignore the roads', 'No reason'],
  correctAnswer: 1,
  userAnswer: null
}
];

@Component({
  selector: 'app-deliverer-quiz',
  imports: [CommonModule],
  templateUrl: './deliverer-quiz.component.html',
  styleUrl: './deliverer-quiz.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DelivererQuizComponent implements OnInit, OnDestroy {
  readonly userId = input<number | null>(null);

  private readonly route = inject(ActivatedRoute, { optional: true });
  private readonly quizService = inject(QuizService);

  readonly questionList = signal<QuizQuestion[]>(QUIZ_QUESTIONS.map((q) => ({ ...q })));
  readonly currentQuestionIndex = signal(0);
  readonly quizCompleted = signal(false);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly copyMessage = signal<string | null>(null);
  readonly result = signal<QuizResultResponse | null>(null);
  readonly routeUserId = signal<number | null>(null);
  readonly nowMs = signal(Date.now());

  readonly calendarLoading = signal(false);
  readonly calendarError = signal<string | null>(null);
  readonly calendarResponse = signal<InterviewCalendarResponse | null>(null);
  readonly selectedSlot = signal<InterviewSlotResponse | null>(null);
  readonly bookingLoading = signal(false);

  private nowTimerId: ReturnType<typeof setInterval> | null = null;

  readonly currentQuestion = computed(() => this.questionList()[this.currentQuestionIndex()] ?? null);
  readonly progressPercent = computed(() => ((this.currentQuestionIndex() + 1) / this.questionList().length) * 100);
  readonly canGoPrevious = computed(() => this.currentQuestionIndex() > 0);
  readonly isLastQuestion = computed(() => this.currentQuestionIndex() === this.questionList().length - 1);
  readonly effectiveUserId = computed(() => this.userId() ?? this.routeUserId());
  readonly restartAllowedAt = computed(() => {
    const currentResult = this.result();
    if (!currentResult || currentResult.status !== 'REJECTED') {
      return null;
    }

    const submittedAt = this.parseSubmittedAt(currentResult.submittedAt);
    if (!submittedAt) {
      return null;
    }

    return new Date(submittedAt.getTime() + 24 * 60 * 60 * 1000);
  });
  readonly canRestartQuiz = computed(() => {
    const currentResult = this.result();
    if (!currentResult) {
      return false;
    }

    if (currentResult.isPassed || currentResult.status === 'ACCEPTED_QUIZ') {
      return false;
    }

    if (currentResult.status === 'REJECTED') {
      const allowedAt = this.restartAllowedAt();
      if (!allowedAt) {
        return false;
      }
      return this.nowMs() >= allowedAt.getTime();
    }

    return false;
  });
  readonly restartHint = computed(() => {
    const currentResult = this.result();
    if (!currentResult) {
      return null;
    }

    if (currentResult.isPassed || currentResult.status === 'ACCEPTED_QUIZ') {
      return 'Quiz completed: you cannot retake this quiz.';
    }

    if (currentResult.status === 'REJECTED') {
      const allowedAt = this.restartAllowedAt();
      if (!allowedAt) {
        return 'Restart unavailable at the moment.';
      }

      if (this.canRestartQuiz()) {
        return 'You can restart the quiz now.';
      }

      return `You can restart the quiz from ${this.formatDateTime(allowedAt)}.`;
    }

    return 'Result pending, restart unavailable.';
  });
  readonly progressStorageKey = computed(() => {
    const userId = this.effectiveUserId();
    return userId ? `deliverer-quiz-progress-${userId}` : null;
  });

  ngOnInit(): void {
    const routeUserIdRaw = this.route?.snapshot.paramMap.get('userId');
    if (routeUserIdRaw) {
      const parsed = Number(routeUserIdRaw);
      this.routeUserId.set(Number.isFinite(parsed) ? parsed : null);
    }

    this.loadInitialState();

    this.nowTimerId = setInterval(() => {
      this.nowMs.set(Date.now());
    }, 60000);
  }

  ngOnDestroy(): void {
    if (this.nowTimerId) {
      clearInterval(this.nowTimerId);
      this.nowTimerId = null;
    }
  }

  selectAnswer(optionIndex: number): void {
    if (this.quizCompleted()) {
      return;
    }

    this.error.set(null);
    const questionIndex = this.currentQuestionIndex();
    this.questionList.update((questions) =>
      questions.map((question, index) =>
        index === questionIndex ? { ...question, userAnswer: optionIndex } : question
      )
    );
    this.saveLocalProgress();
  }

  nextQuestion(): void {
    const currentQuestion = this.currentQuestion();
    if (!currentQuestion || currentQuestion.userAnswer === null) {
      this.error.set('Select an answer before continuing.');
      return;
    }

    if (this.isLastQuestion()) {
      this.submitQuiz();
      return;
    }

    this.currentQuestionIndex.update((index) => index + 1);
    this.error.set(null);
    this.saveLocalProgress();
  }

  previousQuestion(): void {
    if (!this.canGoPrevious()) {
      return;
    }

    this.currentQuestionIndex.update((index) => index - 1);
    this.error.set(null);
    this.saveLocalProgress();
  }

  submitQuiz(): void {
    const userId = this.effectiveUserId();
    if (!userId) {
      this.error.set('Missing user ID');
      return;
    }

    const unanswered = this.questionList().some((question) => question.userAnswer === null);
    if (unanswered) {
      this.error.set('Please answer all questions before submitting.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    const score = this.computeScore();

    this.quizService.submitQuiz(userId, score).subscribe({
      next: (response) => {
        this.result.set(response);
        this.quizCompleted.set(true);
        this.clearLocalProgress();
        this.loading.set(false);
        this.checkAndLoadCalendar(response);
      },
      error: (err: string) => {
        this.error.set(err);
        this.loading.set(false);
      }
    });
  }

  private checkAndLoadCalendar(response: QuizResultResponse | null): void {
    if (response?.status === 'ACCEPTED_QUIZ' && !response.interviewScheduledAt) {
      this.loadCalendar();
    }
  }

  loadCalendar(): void {
    this.calendarLoading.set(true);
    this.calendarError.set(null);
    this.quizService.getInterviewCalendar().subscribe({
      next: (cal) => {
        this.calendarResponse.set(cal);
        this.calendarLoading.set(false);
      },
      error: (err: string) => {
        this.calendarError.set(err);
        this.calendarLoading.set(false);
      }
    });
  }

  selectSlot(slot: InterviewSlotResponse): void {
    if (!slot.available) {
      return;
    }
    if (this.selectedSlot()?.start === slot.start) {
      this.selectedSlot.set(null); // toggle off
    } else {
      this.selectedSlot.set(slot);
    }
  }

  bookSlot(): void {
    const slot = this.selectedSlot();
    if (!slot) {
      return;
    }

    this.bookingLoading.set(true);
    this.error.set(null);

    this.quizService.bookInterviewSlot({ interviewDate: slot.start }).subscribe({
      next: (response) => {
        this.result.set(response); // update result with the scheduled date
        this.bookingLoading.set(false);
      },
      error: (err: string) => {
        this.error.set(err);
        this.bookingLoading.set(false);
      }
    });
  }

  restartQuiz(): void {
    if (!this.canRestartQuiz()) {
      return;
    }

    this.currentQuestionIndex.set(0);
    this.quizCompleted.set(false);
    this.result.set(null);
    this.error.set(null);
    this.copyMessage.set(null);
    this.questionList.set(QUIZ_QUESTIONS.map((q) => ({ ...q, userAnswer: null })));
    this.clearLocalProgress();
  }

  copyMeetingLink(): void {
    const meetingLink = this.result()?.meetingLink;
    if (!meetingLink) {
      return;
    }

    navigator.clipboard
      .writeText(meetingLink)
      .then(() => {
        this.copyMessage.set('Meeting link copied.');
      })
      .catch(() => {
        this.copyMessage.set('Unable to copy the link.');
      });
  }

  private computeScore(): number {
    const questions = this.questionList();
    const pointsPerQuestion = Math.floor(100 / questions.length);

    return questions.reduce((total, question) => {
      if (question.userAnswer === question.correctAnswer) {
        return total + pointsPerQuestion;
      }
      return total;
    }, 0);
  }

  private parseSubmittedAt(value: string | null | undefined): Date | null {
    if (!value) {
      return null;
    }

    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) {
      return null;
    }

    return parsed;
  }

  private formatDateTime(value: Date): string {
    return value.toLocaleString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatSlotTime(isoString: string): string {
    const raw = new Date(isoString);
    if(Number.isNaN(raw.getTime())) return '';
    return raw.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }

  isSlotSelected(slot: InterviewSlotResponse): boolean {
    return this.selectedSlot()?.start === slot.start;
  }

  private loadInitialState(): void {
    const userId = this.effectiveUserId();
    if (!userId) {
      this.restoreLocalProgress();
      return;
    }

    this.loading.set(true);
    this.quizService.getMostRecentQuizResult(userId).subscribe({
      next: (recentResult) => {
        this.result.set(recentResult);
        this.quizCompleted.set(true);
        this.clearLocalProgress();
        this.loading.set(false);
        this.checkAndLoadCalendar(recentResult);
      },
      error: (err: string) => {
        // If no previous result exists yet, continue with local/in-memory quiz state.
        if (err === 'Ressource non trouvee') {
          this.restoreLocalProgress();
          this.loading.set(false);
          return;
        }

        this.error.set(err);
        this.restoreLocalProgress();
        this.loading.set(false);
      }
    });
  }

  private saveLocalProgress(): void {
    const storageKey = this.progressStorageKey();
    if (!storageKey || this.quizCompleted()) {
      return;
    }

    const progress = {
      currentQuestionIndex: this.currentQuestionIndex(),
      questionList: this.questionList()
    };

    localStorage.setItem(storageKey, JSON.stringify(progress));
  }

  private restoreLocalProgress(): void {
    const storageKey = this.progressStorageKey();
    if (!storageKey) {
      return;
    }

    const raw = localStorage.getItem(storageKey);
    if (!raw) {
      return;
    }

    try {
      const parsed = JSON.parse(raw) as {
        currentQuestionIndex?: number;
        questionList?: QuizQuestion[];
      };

      if (Array.isArray(parsed.questionList) && parsed.questionList.length === QUIZ_QUESTIONS.length) {
        this.questionList.set(
          parsed.questionList.map((question, index) => ({
            ...QUIZ_QUESTIONS[index],
            userAnswer: typeof question.userAnswer === 'number' ? question.userAnswer : null
          }))
        );
      }

      if (
        typeof parsed.currentQuestionIndex === 'number' &&
        parsed.currentQuestionIndex >= 0 &&
        parsed.currentQuestionIndex < QUIZ_QUESTIONS.length
      ) {
        this.currentQuestionIndex.set(parsed.currentQuestionIndex);
      }
    } catch {
      localStorage.removeItem(storageKey);
    }
  }

  private clearLocalProgress(): void {
    const storageKey = this.progressStorageKey();
    if (!storageKey) {
      return;
    }

    localStorage.removeItem(storageKey);
  }
}
