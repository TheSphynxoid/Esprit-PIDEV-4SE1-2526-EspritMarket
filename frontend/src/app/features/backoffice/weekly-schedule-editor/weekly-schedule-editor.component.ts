import { Component, Input, Output, EventEmitter, inject, signal, computed, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SrvApiService } from '../../../services';
import { WeeklyTemplateResponse as WeeklyTemplateResource, WeeklyTemplateBatchRequest as WeeklyTemplateBatchPayload, DayEntry } from '@esprit-market/api-types';
import { BrnTooltipImports, provideBrnTooltipDefaultOptions } from '@spartan-ng/brain/tooltip';

interface DayRow {
  dayOfWeek: string;
  label: string;
  active: boolean;
  startHour: string;
  endHour: string;
}

@Component({
  selector: 'app-weekly-schedule-editor',
  standalone: true,
  imports: [CommonModule, BrnTooltipImports],
  providers: [provideBrnTooltipDefaultOptions({
    tooltipContentClasses: 'bg-neutral-800 text-white text-xs px-3 py-1.5 rounded-md shadow-lg',
  })],
  templateUrl: './weekly-schedule-editor.component.html',
  styleUrls: ['./weekly-schedule-editor.component.css']
})
export class WeeklyScheduleEditorComponent implements OnChanges {
  private readonly srvApi = inject(SrvApiService);

  @Input() providerId?: number;
  @Input() serviceId?: number | null;
  @Input() templates: WeeklyTemplateResource[] = [];
  @Input() compact = false;
  @Input() autoSave = true;
  @Output() saved = new EventEmitter<WeeklyTemplateResource[]>();
  @Output() saveRequested = new EventEmitter<WeeklyTemplateBatchPayload>();

  readonly DAYS: { key: string; label: string }[] = [
    { key: 'MONDAY', label: 'Mon' },
    { key: 'TUESDAY', label: 'Tue' },
    { key: 'WEDNESDAY', label: 'Wed' },
    { key: 'THURSDAY', label: 'Thu' },
    { key: 'FRIDAY', label: 'Fri' },
    { key: 'SATURDAY', label: 'Sat' },
    { key: 'SUNDAY', label: 'Sun' }
  ];

  readonly rows = signal<DayRow[]>(this.buildDefaultRows());
  readonly slotDuration = signal(60);
  readonly maxConcurrent = signal(1);
  readonly saving = signal(false);

  readonly hasActiveDays = computed(() => this.rows().some(r => r.active));

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['templates'] && this.templates.length > 0) {
      this.loadFromTemplates();
    }
  }

  toggleDay(index: number): void {
    this.rows.update(rows => {
      const copy = [...rows];
      copy[index] = { ...copy[index], active: !copy[index].active };
      return copy;
    });
  }

  updateRow(index: number, field: 'startHour' | 'endHour', value: string): void {
    this.rows.update(rows => {
      const copy = [...rows];
      copy[index] = { ...copy[index], [field]: value };
      return copy;
    });
  }

  applyToAllActive(sourceIndex: number): void {
    const source = this.rows()[sourceIndex];
    if (!source.active) return;
    this.rows.update(rows => rows.map(r =>
      r.active ? { ...r, startHour: source.startHour, endHour: source.endHour } : r
    ));
  }

  save(): void {
    if (!this.hasActiveDays()) return;

    const payload = this.getPayload();

    if (!this.autoSave) {
      this.saveRequested.emit(payload);
      return;
    }

    if (!this.providerId) return;
    payload.providerId = this.providerId;
    payload.serviceId = this.serviceId ?? undefined;
    this.saving.set(true);

    this.srvApi.saveWeeklyTemplatesBatch(payload).subscribe({
      next: (result) => {
        this.saving.set(false);
        this.saved.emit(result);
      },
      error: () => this.saving.set(false)
    });
  }

  getPayload(): WeeklyTemplateBatchPayload {
    return {
      providerId: this.providerId ?? 0,
      serviceId: this.serviceId ?? undefined,
      slotDurationMinutes: this.slotDuration(),
      maxConcurrent: this.maxConcurrent(),
      entries: this.rows()
        .filter(r => r.active)
        .map(r => ({ dayOfWeek: r.dayOfWeek as DayEntry.DayOfWeekEnum, startHour: r.startHour, endHour: r.endHour }))
    };
  }

  hasCustomSchedule(): boolean {
    return this.hasActiveDays();
  }

  private buildDefaultRows(): DayRow[] {
    return this.DAYS.map(d => ({
      dayOfWeek: d.key,
      label: d.label,
      active: d.key !== 'SATURDAY' && d.key !== 'SUNDAY',
      startHour: '09:00',
      endHour: '17:00'
    }));
  }

  private loadFromTemplates(): void {
    const templateMap = new Map(this.templates.map(t => [t.dayOfWeek, t]));
    const firstTemplate = this.templates[0];
    if (firstTemplate) {
      this.slotDuration.set(firstTemplate.slotDurationMinutes ?? 60);
      this.maxConcurrent.set(firstTemplate.maxConcurrent ?? 1);
    }
    this.rows.set(this.DAYS.map(d => {
      const t = templateMap.get(d.key as WeeklyTemplateResource.DayOfWeekEnum);
      return {
        dayOfWeek: d.key,
        label: d.label,
        active: !!t,
        startHour: t?.startHour ?? '09:00',
        endHour: t?.endHour ?? '17:00'
      };
    }));
  }
}
