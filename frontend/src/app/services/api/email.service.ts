import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ApiConfigService } from './api-config.service';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface EmailPayload {
  to: string;
  subject: string;
  body: string;
  html?: string;
}

export interface EmailResponse {
  success: boolean;
  message?: string;
  error?: string;
}

@Injectable({
  providedIn: 'root'
})
export class EmailService {
  private readonly http = inject(HttpClient);
  private readonly apiConfig = inject(ApiConfigService);

  /**
   * Envoyer un email de notification
   */
  sendEmail(payload: EmailPayload): Observable<EmailResponse> {
    if (environment.emailNotificationsEnabled === false) {
      return of({
        success: true,
        message: 'Notification email desactivee en environnement local.'
      });
    }

    const attempts: Array<{ path: string; body: Record<string, unknown> }> = [
      {
        path: '/api/email/send',
        body: {
          to: payload.to,
          subject: payload.subject,
          text: payload.body,
          html: payload.html
        }
      },
      {
        path: '/api/email/send',
        body: {
          to: payload.to,
          subject: payload.subject,
          body: payload.body,
          html: payload.html
        }
      },
      {
        path: '/api/email/send',
        body: {
          toEmail: payload.to,
          subject: payload.subject,
          text: payload.body,
          htmlContent: payload.html
        }
      },
      {
        path: '/api/email/send-email',
        body: {
          to: payload.to,
          subject: payload.subject,
          text: payload.body,
          message: payload.body,
          html: payload.html
        }
      },
      {
        path: '/api/email',
        body: {
          recipient: payload.to,
          subject: payload.subject,
          body: payload.body,
          html: payload.html
        }
      }
    ];

    const sendAttempt = (index: number): Observable<EmailResponse> => {
      const attempt = attempts[index];
      const url = this.apiConfig.buildUrl(attempt.path);

      return this.http.post<unknown>(url, attempt.body).pipe(
        map((response) => this.normalizeEmailResponse(response)),
        catchError((error) => {
          const canRetry = index < attempts.length - 1;
          const retriable =
            error?.status === 0 ||
            error?.status === 400 ||
            error?.status === 422 ||
            error?.status === 404 ||
            error?.status === 405 ||
            error?.status === 415 ||
            error?.status === 500;

          if (canRetry && retriable) {
            console.warn(`Email attempt ${index + 1} failed on ${attempt.path} (${error?.status}). Trying next fallback...`);
            return sendAttempt(index + 1);
          }

          return of({
            success: false,
            error: `Email indisponible (${error?.status ?? 'unknown'})`
          });
        })
      );
    };

    return sendAttempt(0);
  }

  private normalizeEmailResponse(response: unknown): EmailResponse {
    if (typeof response === 'string') {
      const normalized = response.toLowerCase();
      const looksSuccessful =
        normalized.includes('success') ||
        normalized.includes('envoye') ||
        normalized.includes('sent') ||
        normalized.includes('ok');

      return {
        success: looksSuccessful,
        message: response,
        error: looksSuccessful ? undefined : response
      };
    }

    if (response && typeof response === 'object') {
      const candidate = response as Partial<EmailResponse> & {
        status?: string;
        detail?: string;
        ok?: boolean;
        sent?: boolean;
        success?: boolean;
        message?: string;
      };

      if (typeof candidate.success === 'boolean') {
        return {
          success: candidate.success,
          message: candidate.message,
          error: candidate.error
        };
      }

      if (typeof candidate.ok === 'boolean') {
        return {
          success: candidate.ok,
          message: candidate.message,
          error: candidate.ok ? undefined : (candidate.error || candidate.detail || 'Erreur email')
        };
      }

      if (typeof candidate.sent === 'boolean') {
        return {
          success: candidate.sent,
          message: candidate.message,
          error: candidate.sent ? undefined : (candidate.error || candidate.detail || 'Erreur email')
        };
      }

      if (typeof candidate.status === 'string' && candidate.status.toLowerCase().includes('ok')) {
        return { success: true, message: candidate.message || candidate.detail || 'Email envoye.' };
      }

      if (typeof candidate.status === 'string' && candidate.status.toLowerCase().includes('error')) {
        return { success: false, message: candidate.message, error: candidate.error || candidate.detail || 'Erreur email' };
      }

      return {
        success: false,
        message: candidate.message,
        error: candidate.error || candidate.detail || 'Reponse email non reconnue'
      };
    }

    return { success: false, error: 'Reponse email vide ou invalide' };
  }

  /**
   * Envoyer un email d'approbation à un étudiant
   */
  sendApprovalEmail(studentEmail: string, studentName: string, nextSteps: string[]): Observable<EmailResponse> {
    const html = `
      <h2>Demande Acceptee ✓</h2>
      <p>Bonjour ${studentName},</p>
      <p>Votre demande de creation de boutique a ete <strong>acceptee</strong>.</p>
      <p><strong>Vous pouvez maintenant continuer l'etape 2 de creation de votre boutique.</strong></p>
      <h3>Prochaines étapes :</h3>
      <ol>
        ${nextSteps.map(step => `<li>${step}</li>`).join('')}
      </ol>
      <p>Merci de votre confiance.</p>
      <p>L'équipe Esprit Market</p>
    `;

    const payload: EmailPayload = {
      to: studentEmail,
      subject: 'Demande acceptee: passez a l\'etape 2 de creation de boutique - Esprit Market',
      body: `Bonjour ${studentName},\n\nVotre demande de creation de boutique a ete acceptee.\nVous pouvez maintenant continuer l'etape 2 de creation de votre boutique.\n\nL'equipe Esprit Market`,
      html
    };

    return this.sendEmail(payload);
  }

  /**
   * Envoyer un email de rejet à un étudiant
   */
  sendRejectionEmail(studentEmail: string, studentName: string, reason?: string): Observable<EmailResponse> {
    const html = `
      <h2>Demande Refusée</h2>
      <p>Bonjour ${studentName},</p>
      <p>Malheureusement, votre demande de devenir vendeur sur notre plateforme a été <strong>refusée</strong>.</p>
      ${reason ? `<p><strong>Raison :</strong> ${reason}</p>` : ''}
      <p>Vous pouvez réessayer ultérieurement en complétant une nouvelle demande.</p>
      <p>L'équipe Esprit Market</p>
    `;

    const payload: EmailPayload = {
      to: studentEmail,
      subject: 'Votre demande de vendeur - Esprit Market',
      body: `Bonjour ${studentName},\n\nVotre demande a été traitée.\n\nL'équipe Esprit Market`,
      html
    };

    return this.sendEmail(payload);
  }
}
