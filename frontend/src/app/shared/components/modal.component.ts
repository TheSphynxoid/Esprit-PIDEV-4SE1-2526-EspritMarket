import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './modal.component.html',
  styleUrls: ['./modal.component.css']
})
export class ModalComponent {
  readonly modalTitleId = 'modal-title-' + Math.random().toString(36).substring(2, 9);

  @Input() isOpen = false;
  @Input() title = '';
  @Input() submitLabel = 'save';
  @Input() size: 'md' | 'lg' | 'xl' | '2xl' = 'md';
  @Input() isSubmitting = false;
  @Input() isSubmitDisabled = false;
  @Output() close = new EventEmitter<void>();
  @Output() submitted = new EventEmitter<void>();

  get dialogWidthClass(): string {
    switch (this.size) {
      case 'lg':
        return 'max-w-2xl';
      case 'xl':
        return 'max-w-4xl';
      case '2xl':
        return 'max-w-6xl';
      default:
        return 'max-w-md';
    }
  }

  onClose(): void {
    this.close.emit();
  }

  onSubmit(): void {
    this.submitted.emit();
  }
}
