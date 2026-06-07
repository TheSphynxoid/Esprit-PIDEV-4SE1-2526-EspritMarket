import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FooterComponent } from '../../../shared/layout/footer.component';

@Component({
  selector: 'app-payment-cancel',
  standalone: true,
  imports: [CommonModule, FooterComponent],
  template: `
    <div class="min-h-screen bg-gradient-to-br from-amber-50 to-orange-50 dark:from-neutral-900 dark:to-orange-900/20 flex items-center justify-center p-4">
      <div class="max-w-md w-full">
        <div class="bg-white dark:bg-neutral-800 rounded-2xl shadow-2xl p-8 text-center">
          <!-- Cancel Icon -->
          <div class="flex justify-center mb-6">
            <div class="w-20 h-20 bg-gradient-to-br from-amber-400 to-orange-500 rounded-full flex items-center justify-center animate-pulse">
              <span class="text-4xl">✕</span>
            </div>
          </div>

          <!-- Title -->
          <h1 class="text-3xl font-bold text-amber-600 dark:text-amber-400 mb-2">Payment Cancelled</h1>
          <p class="text-gray-600 dark:text-gray-300 mb-6">Your payment was not completed.</p>

          <!-- Details -->
          <div class="bg-amber-50 dark:bg-amber-900/20 border-l-4 border-amber-500 p-4 rounded mb-8 text-left">
            <p class="text-sm text-gray-700 dark:text-gray-300 mb-2">
              <strong>Status:</strong> <span class="text-amber-600">CANCELLED</span>
            </p>
            <p class="text-sm text-gray-700 dark:text-gray-300">
              No payment was charged. You can try again to complete your purchase.
            </p>
          </div>

          <!-- Buttons -->
          <div class="space-y-3">
            <button 
              (click)="retryPayment()"
              class="w-full bg-gradient-to-r from-amber-500 to-orange-600 hover:from-amber-600 hover:to-orange-700 text-white font-bold py-3 rounded-lg transition-all duration-300 transform hover:scale-105"
            >
              Retry Payment
            </button>
            <button 
              (click)="goHome()"
              class="w-full bg-gray-200 dark:bg-neutral-700 hover:bg-gray-300 dark:hover:bg-neutral-600 text-gray-800 dark:text-white font-bold py-3 rounded-lg transition-all duration-300"
            >
              Back to Home
            </button>
          </div>

          <!-- Help -->
          <p class="text-xs text-gray-500 dark:text-gray-400 mt-6">
            Need help? <a href="/contact" class="text-amber-600 dark:text-amber-400 hover:underline">Contact Support</a>
          </p>
        </div>
      </div>
    </div>
    <app-footer></app-footer>
  `,
  styles: [`
    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.5; }
    }

    .animate-pulse {
      animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
    }
  `]
})
export class PaymentCancelComponent implements OnInit {
  private router = inject(Router);

  ngOnInit() {
    console.log('⚠️ Payment cancelled');
  }

  retryPayment() {
    // Go back to tickets page to retry
    this.router.navigate(['/frontoffice/tickets']);
  }

  goHome() {
    this.router.navigate(['/']);
  }
}
