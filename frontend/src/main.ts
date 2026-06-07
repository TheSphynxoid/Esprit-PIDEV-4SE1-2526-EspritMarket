// Polyfill for CommonJS libs that expect Node-style global in browser.
if (typeof (globalThis as any).global === 'undefined') {
  (globalThis as any).global = globalThis;
}

import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
