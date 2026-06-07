import { Injectable, signal, effect } from '@angular/core';

export type Theme = 'light' | 'dark';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly THEME_KEY = 'esprit_theme';
  
  // Signal to track current theme
  theme = signal<Theme>(this.getInitialTheme());
  
  // Computed signal for dark mode state
  isDarkMode = signal<boolean>(this.theme() === 'dark');

  constructor() {
    // Apply theme immediately on service initialization
    this.applyTheme(this.theme());
    
    // Effect to update isDarkMode whenever theme changes
    effect(() => {
      this.isDarkMode.set(this.theme() === 'dark');
      this.applyTheme(this.theme());
      this.saveTheme(this.theme());
    });
  }

  /**
   * Toggle between light and dark mode
   */
  toggleTheme(): void {
    this.theme.set(this.theme() === 'light' ? 'dark' : 'light');
  }

  /**
   * Set theme explicitly
   */
  setTheme(theme: Theme): void {
    this.theme.set(theme);
  }

  /**
   * Get initial theme from localStorage or system preference
   */
  private getInitialTheme(): Theme {
    // Check localStorage first
    const savedTheme = localStorage.getItem(this.THEME_KEY) as Theme | null;
    if (savedTheme === 'light' || savedTheme === 'dark') {
      return savedTheme;
    }

    // Check system preference
    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      return 'dark';
    }

    return 'light';
  }

  /**
   * Apply theme to document
   */
  private applyTheme(theme: Theme): void {
    const html = document.documentElement;
    if (theme === 'dark') {
      html.classList.add('dark');
    } else {
      html.classList.remove('dark');
    }
  }

  /**
   * Save theme to localStorage
   */
  private saveTheme(theme: Theme): void {
    localStorage.setItem(this.THEME_KEY, theme);
  }
}
