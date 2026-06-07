import { Component, inject, input, output, signal, computed, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SrvApiService } from '../../../services';
import { TimeSlotDto } from '@esprit-market/api-types';

@Component({
  selector: 'app-availability-slot-picker',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="space-y-3">
      <label class="block text-sm font-semibold text-primary dark:text-white mb-1">Select a date</label>
      <div class="flex items-center gap-2">
        <button
          type="button"
          (click)="previousDay()"
          class="p-1.5 rounded-lg border border-neutral-300 dark:border-neutral-600 hover:bg-neutral-100 dark:hover:bg-neutral-700 transition-colors"
          [disabled]="isToday()"
          [class.opacity-40]="isToday()"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
          </svg>
        </button>
        <input
          type="date"
          [value]="selectedDateStr()"
          (change)="onDateChange($event)"
          [min]="todayStr"
          class="flex-1 px-3 py-2 border border-neutral-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-esprit-red dark:bg-neutral-700 dark:border-neutral-600 text-sm"
        />
        <button
          type="button"
          (click)="nextDay()"
          class="p-1.5 rounded-lg border border-neutral-300 dark:border-neutral-600 hover:bg-neutral-100 dark:hover:bg-neutral-700 transition-colors"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
          </svg>
        </button>
      </div>

      <div class="text-xs text-secondary">
        {{ selectedDateLabel() }}
      </div>

      @if (loadingSlots()) {
        <div class="text-center py-4 text-sm text-secondary">Loading available slots...</div>
      } @else if (slots().length === 0) {
        <div class="text-center py-4 text-sm text-secondary">
          No availability on this day. Try another date.
        </div>
      } @else {
        <div>
          <div class="flex items-center justify-between mb-2">
            <label class="text-sm font-semibold text-primary dark:text-white">Available time slots</label>
            <span class="text-xs text-secondary">{{ availableCount() }} slots available</span>
          </div>
          <div class="grid grid-cols-3 gap-1.5 max-h-44 overflow-y-auto pr-1">
            @for (slot of slots(); track slot.start) {
              <button
                type="button"
                (click)="selectSlot(slot)"
                [disabled]="!slot.available && !isInRange(slot)"
                class="px-2 py-2 text-xs font-medium rounded-lg border transition-all text-center
                  [word-break:break-word]"
                [class]="slotBtnClass(slot)"
              >
                {{ slotTimeLabel(slot) }}
              </button>
            }
          </div>
          @if (selectedSlot() && rangeLabel()) {
            <div class="text-xs text-esprit-red font-medium mt-1">{{ rangeLabel() }}</div>
          }
          <div class="flex items-center gap-3 mt-2 text-xs text-secondary">
            <span class="flex items-center gap-1">
              <span class="inline-block w-3 h-3 rounded border-2 border-esprit-red bg-esprit-red/10"></span>
              Selected
            </span>
            <span class="flex items-center gap-1">
              <span class="inline-block w-3 h-3 rounded border border-esprit-red/40 bg-esprit-red/5"></span>
              Range
            </span>
            <span class="flex items-center gap-1">
              <span class="inline-block w-3 h-3 rounded border border-green-300 bg-green-50"></span>
              Available
            </span>
            <span class="flex items-center gap-1">
              <span class="inline-block w-3 h-3 rounded border border-neutral-200 bg-neutral-50 opacity-50"></span>
              Full
            </span>
          </div>
        </div>
      }
    </div>
  `,
  styles: []
})
export class AvailabilitySlotPickerComponent implements OnInit, OnDestroy {
  private readonly srvApi = inject(SrvApiService);

  readonly serviceId = input.required<number>();
  readonly duration = input<number>(1);
  readonly slotSelected = output<{ start: string; end: string; slotDurationMinutes: number }>();

  readonly today = new Date();
  readonly todayStr = this.formatDate(this.today);

  readonly selectedDate = signal<Date>(new Date());
  readonly slots = signal<TimeSlotDto[]>([]);
  readonly selectedSlot = signal<TimeSlotDto | null>(null);
  readonly loadingSlots = signal(false);

  readonly selectedDateStr = computed(() => this.formatDate(this.selectedDate()));
  readonly selectedDateLabel = computed(() => {
    const d = this.selectedDate();
    const options: Intl.DateTimeFormatOptions = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
    return d.toLocaleDateString('en-US', options);
  });

  readonly availableCount = computed(() => this.slots().filter(s => s.available).length);

  readonly rangeEnd = computed(() => {
    const slot = this.selectedSlot();
    const dur = this.duration();
    if (!slot?.start || !dur) return null;
    return new Date(new Date(slot.start).getTime() + dur * 3600000);
  });

  readonly rangeLabel = computed(() => {
    const anchor = this.selectedSlot();
    const end = this.rangeEnd();
    if (!anchor?.start || !end) return '';
    const dur = this.duration();
    const slotDur = anchor.slotDurationMinutes ?? 60;
    const count = Math.round((dur * 60) / slotDur);
    const s = new Date(anchor.start);
    const e = end;
    const timeRange = `${this.pad(s.getHours())}:${this.pad(s.getMinutes())} – ${this.pad(e.getHours())}:${this.pad(e.getMinutes())}`;
    if (count <= 1) return timeRange;
    return `${timeRange}  (${count} slots)`;
  });

  private loadTimeout: any;

  ngOnInit(): void {
    this.loadSlots();
  }

  ngOnDestroy(): void {
    if (this.loadTimeout) clearTimeout(this.loadTimeout);
  }

  reloadSlots(): void {
    this.selectedSlot.set(null);
    this.loadSlots();
  }

  onDateChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.value) {
      this.selectedDate.set(new Date(input.value + 'T00:00:00'));
      this.selectedSlot.set(null);
      this.loadSlots();
    }
  }

  previousDay(): void {
    const d = new Date(this.selectedDate());
    d.setDate(d.getDate() - 1);
    if (d >= this.stripTime(this.today)) {
      this.selectedDate.set(d);
      this.selectedSlot.set(null);
      this.loadSlots();
    }
  }

  nextDay(): void {
    const d = new Date(this.selectedDate());
    d.setDate(d.getDate() + 1);
    this.selectedDate.set(d);
    this.selectedSlot.set(null);
    this.loadSlots();
  }

  isToday(): boolean {
    return this.selectedDateStr() === this.todayStr;
  }

  selectSlot(slot: TimeSlotDto): void {
    if (!slot.available) return;
    this.selectedSlot.set(slot);
    this.slotSelected.emit({ start: slot.start!, end: slot.end!, slotDurationMinutes: slot.slotDurationMinutes ?? 60 });
  }

  isInRange(slot: TimeSlotDto): boolean {
    const end = this.rangeEnd();
    const anchor = this.selectedSlot();
    if (!end || !anchor?.start || !slot.start) return false;
    if (anchor.start === slot.start) return false;
    const t = new Date(slot.start).getTime();
    return t >= new Date(anchor.start).getTime() && t < end.getTime();
  }

  slotTimeLabel(slot: TimeSlotDto): string {
    if (!slot.start || !slot.end) return '';
    const s = new Date(slot.start);
    const e = new Date(slot.end);
    return `${this.pad(s.getHours())}:${this.pad(s.getMinutes())} - ${this.pad(e.getHours())}:${this.pad(e.getMinutes())}`;
  }

  slotBtnClass(slot: TimeSlotDto): string {
    const sel = this.selectedSlot();
    const isSelected = sel?.start === slot.start;
    if (isSelected) {
      return 'border-esprit-red bg-esprit-red/10 text-esprit-red ring-2 ring-esprit-red/30 font-bold';
    }
    if (this.isInRange(slot)) {
      if (!slot.available) {
        return 'border-esprit-red/30 bg-red-50 dark:bg-red-900/10 text-red-400 dark:text-red-500 line-through opacity-60';
      }
      return 'border-esprit-red/40 bg-esprit-red/5 text-esprit-red/80';
    }
    if (!slot.available) {
      return 'border-neutral-200 dark:border-neutral-700 bg-neutral-50 dark:bg-neutral-800 text-neutral-400 dark:text-neutral-500 cursor-not-allowed opacity-50';
    }
    return 'border-green-300 dark:border-green-800 bg-green-50 dark:bg-green-900/20 text-green-700 dark:text-green-400 hover:border-esprit-red hover:bg-esprit-red/5 cursor-pointer';
  }

  private loadSlots(): void {
    const sid = this.serviceId();
    if (!sid) return;

    this.loadingSlots.set(true);
    const dateStr = this.selectedDateStr();

    this.srvApi.getAvailableSlots(sid, dateStr, dateStr).subscribe({
      next: (slots) => {
        this.slots.set(slots);
        this.loadingSlots.set(false);
      },
      error: () => {
        this.slots.set([]);
        this.loadingSlots.set(false);
      }
    });
  }

  private formatDate(d: Date): string {
    return `${d.getFullYear()}-${this.pad(d.getMonth() + 1)}-${this.pad(d.getDate())}`;
  }

  private stripTime(d: Date): Date {
    return new Date(d.getFullYear(), d.getMonth(), d.getDate());
  }

  private pad(n: number): string {
    return n.toString().padStart(2, '0');
  }
}
