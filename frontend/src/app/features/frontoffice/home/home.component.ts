import { Component, AfterViewInit, OnDestroy, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';

import { FooterComponent, HeaderComponent } from '../../../shared/layout';
import { AuthService } from '../../../services';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, HeaderComponent, FooterComponent],
  templateUrl: './home.component.html',
  styleUrls: []
})
export class HomeComponent implements AfterViewInit, OnDestroy {
  private observers: IntersectionObserver[] = [];

  constructor(private router: Router, private el: ElementRef) {}

  ngAfterViewInit(): void {
    this.initHeroReveal();
    this.initScrollReveal();
    this.initCounters();
  }

  ngOnDestroy(): void {
    this.observers.forEach(o => o.disconnect());
  }

  private initHeroReveal(): void {
    requestAnimationFrame(() => {
      this.el.nativeElement.querySelectorAll('.hero-reveal').forEach((el: HTMLElement) => {
        el.classList.add('is-visible');
      });
    });
  }

  private initScrollReveal(): void {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry, index) => {
          if (entry.isIntersecting) {
            const el = entry.target as HTMLElement;
            el.style.animationDelay = `${index * 0.1}s`;
            el.classList.add('is-visible');
            observer.unobserve(el);
          }
        });
      },
      { threshold: 0.1, rootMargin: '0px 0px -50px 0px' }
    );

    this.el.nativeElement.querySelectorAll('.scroll-reveal').forEach((el: HTMLElement) => {
      observer.observe(el);
    });

    this.observers.push(observer);
  }

  private initCounters(): void {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            const el = entry.target as HTMLElement;
            const target = parseInt(el.dataset['target'] || '0', 10);
            const suffix = el.dataset['suffix'] || '';
            this.animateCounter(el, target, suffix);
            observer.unobserve(el);
          }
        });
      },
      { threshold: 0.5 }
    );

    this.el.nativeElement.querySelectorAll('.counter').forEach((el: HTMLElement) => {
      observer.observe(el);
    });

    this.observers.push(observer);
  }

  private animateCounter(element: HTMLElement, target: number, suffix: string): void {
    const duration = 2000;
    const startTime = performance.now();

    const tick = (currentTime: number) => {
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const eased = 1 - Math.pow(1 - progress, 3);
      const current = Math.round(target * eased);

      if (target >= 1000) {
        const kValue = current / 1000;
        const display = kValue % 1 === 0 ? kValue.toFixed(0) : kValue.toFixed(1);
        element.textContent = display + 'K' + suffix;
      } else {
        element.textContent = current + suffix;
      }

      if (progress < 1) {
        requestAnimationFrame(tick);
      }
    };

    requestAnimationFrame(tick);
  }

  createStore() {
    this.router.navigate(['/create-store']);
  }

  goToMarketplace(): void {
    this.router.navigate(['/marketplace']);
  }
}
