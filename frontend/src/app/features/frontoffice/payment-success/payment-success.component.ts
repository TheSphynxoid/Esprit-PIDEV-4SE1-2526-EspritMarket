import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FooterComponent } from '../../../shared/layout/footer.component';

@Component({
  selector: 'app-payment-success',
  standalone: true,
  imports: [CommonModule, FooterComponent],
  template: `
    <div class="min-h-screen bg-gradient-to-br from-green-50 to-emerald-50 dark:from-neutral-900 dark:to-emerald-900/20 flex items-center justify-center p-4">
      <div class="max-w-md w-full">
        <div class="bg-white dark:bg-neutral-800 rounded-2xl shadow-2xl p-8 text-center">
          <!-- Success Icon -->
          <div class="flex justify-center mb-6">
            <div class="w-20 h-20 bg-gradient-to-br from-green-400 to-emerald-500 rounded-full flex items-center justify-center animate-bounce">
              <span class="text-4xl">✓</span>
            </div>
          </div>

          <!-- Title -->
          <h1 class="text-3xl font-bold text-green-600 dark:text-green-400 mb-2">Payment Successful!</h1>
          <p class="text-gray-600 dark:text-gray-300 mb-6">Your ticket has been purchased successfully.</p>

          <!-- Details -->
          <div class="bg-green-50 dark:bg-green-900/20 border-l-4 border-green-500 p-4 rounded mb-8 text-left">
            <p class="text-sm text-gray-700 dark:text-gray-300 mb-2">
              <strong>Status:</strong> <span class="text-green-600">COMPLETED</span>
            </p>
            <p class="text-sm text-gray-700 dark:text-gray-300">
              A confirmation email has been sent to your registered email address.
            </p>
          </div>

          <!-- Buttons -->
          <div class="space-y-3">
            <button 
              (click)="goToTickets()"
              class="w-full bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-600 hover:to-emerald-700 text-white font-bold py-3 rounded-lg transition-all duration-300 transform hover:scale-105"
            >
              View Your Tickets
            </button>
            <button 
              (click)="goHome()"
              class="w-full bg-gray-200 dark:bg-neutral-700 hover:bg-gray-300 dark:hover:bg-neutral-600 text-gray-800 dark:text-white font-bold py-3 rounded-lg transition-all duration-300"
            >
              Back to Home
            </button>
          </div>

          <!-- Message -->
          <p class="text-xs text-gray-500 dark:text-gray-400 mt-6">
            Thank you for your purchase! Enjoy the event.
          </p>
        </div>
      </div>
    </div>
    <app-footer></app-footer>
  `,
  styles: [`
    @keyframes bounce {
      0%, 100% { transform: translateY(0); }
      50% { transform: translateY(-10px); }
    }

    .animate-bounce {
      animation: bounce 2s infinite;
    }
  `]
})
export class PaymentSuccessComponent implements OnInit {
  private router = inject(Router);

  ngOnInit() {
    // Payment successful - ticket has been created
    console.log('✅ Payment successful!');
  }

  goToTickets() {
    this.router.navigate(['/frontoffice/tickets']);
  }

  goHome() {
    this.router.navigate(['/']);
  }
}
