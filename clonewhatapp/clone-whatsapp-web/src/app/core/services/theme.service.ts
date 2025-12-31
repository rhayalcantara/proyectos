import { Injectable, signal, effect } from '@angular/core';

export type Theme = 'light' | 'dark';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly STORAGE_KEY = 'whatsapp-theme';

  currentTheme = signal<Theme>(this.getInitialTheme());
  isDarkMode = signal<boolean>(this.currentTheme() === 'dark');

  constructor() {
    // Aplicar tema al iniciar
    this.applyTheme(this.currentTheme());

    // Efecto para sincronizar isDarkMode con currentTheme
    effect(() => {
      this.isDarkMode.set(this.currentTheme() === 'dark');
    });

    // Escuchar cambios en preferencia del sistema
    this.listenToSystemPreference();
  }

  private getInitialTheme(): Theme {
    // Primero verificar localStorage
    const savedTheme = localStorage.getItem(this.STORAGE_KEY) as Theme;
    if (savedTheme === 'light' || savedTheme === 'dark') {
      return savedTheme;
    }

    // Si no hay preferencia guardada, usar preferencia del sistema
    if (window.matchMedia?.('(prefers-color-scheme: dark)').matches) {
      return 'dark';
    }

    return 'light';
  }

  private applyTheme(theme: Theme): void {
    if (theme === 'dark') {
      document.documentElement.setAttribute('data-theme', 'dark');
    } else {
      document.documentElement.removeAttribute('data-theme');
    }
  }

  private listenToSystemPreference(): void {
    const mediaQuery = window.matchMedia?.('(prefers-color-scheme: dark)');
    if (mediaQuery) {
      mediaQuery.addEventListener('change', (e) => {
        // Solo cambiar si no hay preferencia guardada
        if (!localStorage.getItem(this.STORAGE_KEY)) {
          this.setTheme(e.matches ? 'dark' : 'light', false);
        }
      });
    }
  }

  setTheme(theme: Theme, persist = true): void {
    this.currentTheme.set(theme);
    this.applyTheme(theme);

    if (persist) {
      localStorage.setItem(this.STORAGE_KEY, theme);
    }
  }

  toggleTheme(): void {
    const newTheme = this.currentTheme() === 'light' ? 'dark' : 'light';
    this.setTheme(newTheme);
  }
}
