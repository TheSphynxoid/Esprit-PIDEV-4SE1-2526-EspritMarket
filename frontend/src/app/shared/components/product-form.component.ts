import { Component, Input, OnChanges, OnInit, SimpleChanges, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, Validators, ReactiveFormsModule, FormGroup } from '@angular/forms';
import type { ProductCategoryOption, ProductCreationFormValue } from '../../services/product.service';

export type ProductFormData = ProductCreationFormValue;

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './product-form.component.html',
  styleUrls: []
})
export class ProductFormComponent implements OnInit, OnChanges {
  @Input() initialData?: ProductFormData;
  @Input() categories: ProductCategoryOption[] = [];
  @Input() isCategoriesLoading = false;
  @Input() categoriesError = '';

  // Fallback categories to show when backend returns none
  private static readonly FALLBACK_CATEGORIES: ProductCategoryOption[] = [
    { id: 1001, name: 'clothes', description: 'clothes' },
    { id: 1002, name: 'furniture', description: 'furniture' },
    { id: 1003, name: 'electronic', description: 'Électronique' },
    { id: 1004, name: 'others', description: 'Autres' }
  ];

  form!: FormGroup;
  imagePreviewUrl = '';
  imageSelectionError = '';

  constructor(
    private fb: FormBuilder,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.createForm();
  }

  ngOnInit(): void {
    if (this.initialData) {
      this.form.patchValue(this.initialData);
    }

    this.applyFallbackCategoriesIfNeeded();
    this.syncCategoryControlDisabledState();
    this.ensureDefaultCategorySelection();

    this.imagePreviewUrl = this.form.get('imageUrl')?.value || '';
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isCategoriesLoading']) {
      this.syncCategoryControlDisabledState();
    }

    if (changes['categories']) {
      this.applyFallbackCategoriesIfNeeded();
      this.ensureDefaultCategorySelection();
    }
  }

  private applyFallbackCategoriesIfNeeded(): void {
    try {
      const hasCategories = Array.isArray(this.categories) && this.categories.length > 0;
      if (!hasCategories && !this.isCategoriesLoading) {
        // copy fallback so consumer cannot mutate static
        this.categories = ProductFormComponent.FALLBACK_CATEGORIES.map(c => ({ ...c }));
      }
    } catch (e) {
      // ignore
    }
  }

  private createForm(): FormGroup {
    return this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      phone: ['', [Validators.pattern(/^\d{8}$/)]],
      price: [0, [Validators.required, Validators.min(0.01)]],
      stock: [null, [Validators.required, Validators.min(1)]],
      description: ['', [Validators.required, Validators.minLength(10)]],
      imageUrl: [''],
      categoryId: [null, [Validators.required]],
      width: [null, [Validators.required, Validators.min(0.01)]],
      height: [null, [Validators.required, Validators.min(0.01)]],
      depth: [null, [Validators.required, Validators.min(0.01)]],
      dimensionUnit: ['cm', [Validators.required]],
      weight: [null, [Validators.required, Validators.min(0.01)]]
    });
  }

  getFormData(): ProductFormData | null {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return null;
    }
    return this.form.value as ProductFormData;
  }

  isControlInvalid(controlName: string): boolean {
    const control = this.form.get(controlName);
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    this.imageSelectionError = '';

    if (!file) {
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.imageSelectionError = 'Veuillez selectionner un fichier image valide.';
      input.value = '';
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = typeof reader.result === 'string' ? reader.result : '';
      this.form.patchValue({ imageUrl: dataUrl });
      this.imagePreviewUrl = dataUrl;
      this.cdr.detectChanges();
    };
    reader.onerror = () => {
      this.imageSelectionError = 'Impossible de lire le fichier image.';
    };
    reader.readAsDataURL(file);
  }

  reset(): void {
    this.form.reset(this.initialData || {
      name: '',
      phone: '',
      price: 0,
      stock: null,
      description: '',
      imageUrl: '',
      categoryId: null,
      width: null,
      height: null,
      depth: null,
      dimensionUnit: 'cm',
      weight: null
    });

    this.ensureDefaultCategorySelection();
    this.syncCategoryControlDisabledState();

    this.imagePreviewUrl = this.form.get('imageUrl')?.value || '';
    this.imageSelectionError = '';
  }

  private syncCategoryControlDisabledState(): void {
    const categoryControl = this.form.get('categoryId');
    if (!categoryControl) {
      return;
    }

    if (this.isCategoriesLoading) {
      if (categoryControl.enabled) {
        categoryControl.disable({ emitEvent: false });
      }
      return;
    }

    if (categoryControl.disabled) {
      categoryControl.enable({ emitEvent: false });
    }
  }

  private ensureDefaultCategorySelection(): void {
    const current = Number(this.form.get('categoryId')?.value || 0);
    if (current > 0) {
      return;
    }

    const firstCategoryId = Number(this.categories[0]?.id || 0);
    if (firstCategoryId > 0) {
      this.form.patchValue({ categoryId: firstCategoryId });
    }
  }
}
