import { Component, input, contentChild, TemplateRef } from '@angular/core';
import { CommonModule, NgTemplateOutlet } from '@angular/common';

@Component({
  selector: 'app-paradox-tip',
  standalone: true,
  imports: [CommonModule, NgTemplateOutlet],
  template: `
    <div class="paradox-tip-wrap">
      <ng-content />
      <div class="paradox-tip">
        <ng-container *ngIf="label(); let lbl">
          <span class="paradox-tip-label">{{ lbl }}</span>
        </ng-container>
        <ng-container *ngIf="value(); let val">
          <span class="paradox-tip-value"> {{ val }}</span>
        </ng-container>
        <p class="paradox-tip-body">{{ text() }}</p>
        <ng-container *ngIf="tipTemplate()">
          <ng-container *ngTemplateOutlet="tipTemplate()!" />
        </ng-container>
      </div>
    </div>
  `,
  styles: [`
    :host { display: inline-block; }

    .paradox-tip-wrap {
      position: relative;
      display: inline-block;
      cursor: help;
    }

    .paradox-tip {
      position: absolute;
      left: 50%;
      transform: translateX(-50%);
      top: 100%;
      width: 254px;
      padding: 10px 12px;
      border-radius: 8px;
      background: rgb(26, 34, 52);
      color: #cbd5e1;
      font-size: 12px;
      line-height: 1.5;
      text-align: left;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
      border: 1px solid rgba(255, 255, 255, 0.08);
      display: none;
      z-index: 9999;
    }

    .paradox-tip::before {
      content: '';
      position: absolute;
      top: -8px;
      left: 0;
      right: 0;
      height: 8px;
    }

    .paradox-tip-wrap:hover > .paradox-tip {
      display: block;
    }

    /* Recursive nesting — tooltip inside tooltip */
    .paradox-tip .paradox-tip-wrap > .paradox-tip {
      top: 100%;
      left: 0;
      transform: none;
      width: 230px;
      background: rgb(17, 24, 39);
      border: 1px solid rgba(255, 255, 255, 0.1);
      z-index: 10000;
    }

    .paradox-tip .paradox-tip-wrap > .paradox-tip::before {
      top: -6px;
      height: 6px;
    }

    .paradox-tip-label {
      display: block;
      font-weight: 600;
      color: #f1f5f9;
      margin-bottom: 4px;
    }

    .paradox-tip-value {
      color: #fbbf24;
      font-weight: 600;
      font-size: 11px;
    }

    .paradox-tip-body {
      margin: 4px 0 0;
      color: #94a3b8;
    }

    .paradox-tip-highlight {
      color: #93c5fd;
      text-decoration: underline dotted;
      text-underline-offset: 2px;
      cursor: help;
      white-space: nowrap;
    }

    .paradox-tip-highlight:hover {
      color: #bfdbfe;
    }
  `],
})
export class ParadoxTipComponent {
  readonly text = input<string>('');
  readonly label = input<string>('');
  readonly value = input<string>('');
  readonly tipTemplate = contentChild<TemplateRef<unknown>>('tipTemplate');
}
