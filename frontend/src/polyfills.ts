declare global {
  interface Window {
    global?: unknown;
  }
}

if (typeof window !== 'undefined' && typeof window.global === 'undefined') {
  window.global = window;
}

export {};
