import { Component, inject, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ServiceResponse, WeeklyTemplateBatchRequest as WeeklyTemplateBatchPayload } from '@esprit-market/api-types';
import { WeeklyScheduleEditorComponent } from '../weekly-schedule-editor';

@Component({
  selector: 'app-service-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, WeeklyScheduleEditorComponent],
  templateUrl: './service-form.component.html',
  styleUrls: ['./service-form.component.css']
})
export class ServiceFormComponent {
  private fb = inject(FormBuilder);

  @ViewChild('scheduleEditor') scheduleEditor?: WeeklyScheduleEditorComponent;

  categories = Object.values(ServiceResponse.CategoryEnum);
  pricingTypes = Object.values(ServiceResponse.PricingTypeEnum);
  statuses = Object.values(ServiceResponse.StatusEnum);
  readonly imagePreview = signal<string | null>(null);
  selectedFile: File | null = null;

  readonly scheduleMode = signal<'default' | 'custom'>('default');

  form: FormGroup;

  constructor() {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      category: ['', Validators.required],
      description: [''],
      pricingType: ['HOURLY', Validators.required],
      price: [0, [Validators.required, Validators.min(0)]],
      imageUrl: [''],
      tags: [''],
      status: ['AVAILABLE', Validators.required]
    });
  }

  getFormData() {
    const value = { ...this.form.value };
    if (typeof value.tags === 'string' && value.tags.trim()) {
      value.tags = value.tags.split(',').map((t: string) => t.trim()).filter((t: string) => t);
    } else {
      value.tags = [];
    }
    return value;
  }

  getSchedulePayload(): WeeklyTemplateBatchPayload | null {
    if (this.scheduleMode() !== 'custom' || !this.scheduleEditor) return null;
    if (!this.scheduleEditor.hasCustomSchedule()) return null;
    return this.scheduleEditor.getPayload();
  }

  setFormData(data: any) {
    this.form.patchValue({
      name: data.name ?? '',
      category: data.category ?? '',
      description: data.description ?? '',
      pricingType: data.pricingType ?? 'HOURLY',
      price: data.price ?? data.hourlyRate ?? 0,
      imageUrl: data.imageUrl ?? '',
      tags: Array.isArray(data.tags) ? data.tags.join(', ') : '',
      status: data.status ?? 'AVAILABLE'
    });
  }

  resetForm() {
    this.form.reset({
      name: '',
      category: '',
      description: '',
      pricingType: 'HOURLY',
      price: 0,
      imageUrl: '',
      tags: '',
      status: 'AVAILABLE'
    });
    Object.keys(this.form.controls).forEach((key) => {
      this.form.get(key)?.markAsUntouched();
      this.form.get(key)?.markAsPristine();
    });
    this.scheduleMode.set('default');
  }

  isFormValid(): boolean {
    return this.form.valid;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];
    if (!file.type.startsWith('image/')) return;
    if (file.size > 5 * 1024 * 1024) return;
    this.selectedFile = file;
    const reader = new FileReader();
    reader.onload = () => this.imagePreview.set(reader.result as string);
    reader.readAsDataURL(file);
  }

  clearImage(): void {
    this.selectedFile = null;
    this.imagePreview.set(null);
    this.form.patchValue({ imageUrl: '' });
  }
}
