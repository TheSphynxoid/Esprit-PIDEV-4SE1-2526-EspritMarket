import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { switchMap } from 'rxjs/operators';

import { ProjectRequest, ProjectResponse, ServiceResponse } from '@esprit-market/api-types';
import { FooterComponent, HeaderComponent } from '../../../shared/layout';
import { AuthService, SrvApiService } from '../../../services';
import { resolveHttpError } from '../../../shared/utils';

@Component({
  selector: 'app-projects-services',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, HeaderComponent, FooterComponent],
  templateUrl: './projects-services.component.html',
  styleUrls: []
})
export class ProjectsServicesComponent implements OnInit {
  private readonly srvApi = inject(SrvApiService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);

  readonly loading = signal(false);
  readonly isSaving = signal(false);
  readonly projects = signal<ProjectResponse[]>([]);
  readonly eligibleServices = signal<ServiceResponse[]>([]);
  readonly selectedServiceIds = signal<number[]>([]);
  readonly originalServiceIds = signal<number[]>([]);
  readonly error = signal('');
  readonly success = signal('');
  readonly editingProjectId = signal<number | null>(null);
  readonly serviceSearchTerm = signal('');
  readonly showCreateForm = signal(false);

  readonly wizardStep = signal<'describe' | 'review' | 'done' | 'manual'>('describe');
  readonly wizardDesc = signal('');
  readonly wizardDecomposing = signal(false);
  readonly wizardDecomposition = signal<any>(null);
  readonly wizardCreating = signal(false);

  readonly showWizard = computed(() => this.wizardStep() !== 'describe' || this.wizardDesc().trim().length > 0);

  readonly canManageProjects = computed(() => {
    return !!this.auth.currentUser()?.id;
  });

  isProjectOwner(project: ProjectResponse): boolean {
    const userId = this.auth.currentUser()?.id;
    return !!userId && project.creatorId === userId;
  }

  readonly projectForm = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(150)]],
    details: ['', [Validators.maxLength(2000)]],
    priority: ['MEDIUM', [Validators.required]],
    budget: [0, [Validators.required, Validators.min(0)]],
    startDate: [''],
    estimatedEndDate: ['']
  });

  readonly priorityOptions = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  readonly filteredEligibleServices = computed(() => {
    const term = this.serviceSearchTerm().toLowerCase().trim();
    const list = this.eligibleServices();
    if (!term) return list;
    return list.filter(s =>
      (s.name || '').toLowerCase().includes(term) ||
      (s.category || '').toLowerCase().includes(term) ||
      (s.providerName || '').toLowerCase().includes(term)
    );
  });

  ngOnInit(): void {
    this.loadProjects();
    this.loadEligibleServices();
  }

  openProject(projectId: number | undefined): void {
    if (!projectId) return;
    this.router.navigate(['/projects', projectId]);
  }

  saveProject(): void {
    if (!this.canManageProjects() || this.projectForm.invalid) {
      return;
    }

    this.isSaving.set(true);
    this.error.set('');
    this.success.set('');

    const payload = this.toProjectPayload();
    const editingId = this.editingProjectId();
    const req$ = editingId
      ? this.srvApi.updateProject(editingId, payload)
      : this.srvApi.createProject(payload);

    req$
      .pipe(
        switchMap((response) => {
          const projectId = response?.id;
          if (!projectId) {
            return of(response);
          }
          return this.syncProjectServices(projectId).pipe(switchMap(() => of(response)));
        })
      )
      .subscribe({
      next: (response) => {
        this.isSaving.set(false);
        if (editingId) {
          this.success.set('Project updated.');
          this.resetProjectForm();
          this.loadProjects();
        } else {
          const newId = response?.id;
          if (newId) {
            this.router.navigate(['/projects', newId]);
          } else {
            this.success.set('Project created.');
            this.resetProjectForm();
            this.loadProjects();
          }
        }
      },
      error: (err) => {
        this.isSaving.set(false);
        this.error.set(resolveHttpError(err, 'Could not save the project. Please try again.'));
      }
    });
  }

  wizardAnalyze(): void {
    const desc = this.wizardDesc().trim();
    if (!desc) return;
    this.wizardDecomposing.set(true);
    this.http.post('/api/srv/projects/assistant/decompose', { description: desc }).subscribe({
      next: (data: any) => {
        this.wizardDecomposing.set(false);
        this.wizardDecomposition.set(data);
        this.wizardStep.set('review');
      },
      error: () => this.wizardDecomposing.set(false)
    });
  }

  wizardBack(): void {
    this.wizardStep.set('describe');
    this.wizardDecomposition.set(null);
  }

  wizardCreate(): void {
    this.wizardCreating.set(true);
    const decomp = this.wizardDecomposition();
    const today = new Date();
    const endDate = new Date(today);
    endDate.setDate(endDate.getDate() + (decomp?.estimatedDays || 7));
    const payload: any = {
      title: this.wizardDesc().split(/[,;.!?]/)[0].trim().substring(0, 150) || 'New Project',
      details: this.wizardDesc(),
      priority: 'MEDIUM',
      budget: decomp?.estimatedBudgetWithBuffer || 0,
      startDate: today.toISOString().split('T')[0],
      estimatedEndDate: endDate.toISOString().split('T')[0],
      status: 'PLANNED'
    };
    this.srvApi.createProject(payload).subscribe({
      next: (response: any) => {
        const id = response?.id;
        if (!id) { this.wizardCreating.set(false); return; }
        this.http.post(`/api/srv/projects/${id}/assistant/decompose-apply`, { description: this.wizardDesc() }).subscribe({
          next: () => {
            this.wizardCreating.set(false);
            this.wizardStep.set('done');
            setTimeout(() => this.router.navigate(['/projects', id]), 600);
          },
          error: () => {
            this.wizardCreating.set(false);
            this.router.navigate(['/projects', id]);
          }
        });
      },
      error: (err) => {
        this.wizardCreating.set(false);
        this.error.set(resolveHttpError(err, 'Could not create project.'));
      }
    });
  }

  wizardReset(): void {
    this.wizardStep.set('describe');
    this.wizardDesc.set('');
    this.wizardDecomposition.set(null);
  }

  cancelEdit(): void {
    this.resetProjectForm();
  }

  editProject(project: ProjectResponse): void {
    if (!this.isProjectOwner(project) || !project.id) return;
    this.editingProjectId.set(project.id);
    this.wizardStep.set('describe');
    this.wizardDesc.set('');
    this.wizardDecomposition.set(null);
    this.projectForm.patchValue({
      title: project.title || '',
      details: project.details || '',
      priority: project.priority || 'MEDIUM',
      budget: project.budget ?? 0,
      startDate: this.toDateInput(project.startDate),
      estimatedEndDate: this.toDateInput(project.estimatedEndDate)
    });
    this.showCreateForm.set(true);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  deleteProject(project: ProjectResponse): void {
    if (!this.isProjectOwner(project) || !project.id) return;
    if (!confirm(`Delete project "${project.title || 'Untitled'}"?`)) return;

    this.error.set('');
    this.success.set('');

    this.srvApi.deleteProject(project.id).subscribe({
      next: () => {
        this.success.set('Project deleted.');
        if (this.editingProjectId() === project.id) {
          this.resetProjectForm();
        }
        this.loadProjects();
      },
      error: (err) => {
        this.error.set(resolveHttpError(err, 'Could not delete the project. Please try again.'));
      }
    });
  }

  goToServices(): void {
    this.router.navigate(['/services']);
  }

  goToBookings(): void {
    this.router.navigate(['/bookings']);
  }

  toggleService(serviceId: number | undefined): void {
    if (!serviceId) return;
    const current = this.selectedServiceIds();
    if (current.includes(serviceId)) {
      this.selectedServiceIds.set(current.filter((id) => id !== serviceId));
      return;
    }
    this.selectedServiceIds.set([...current, serviceId]);
  }

  isServiceSelected(serviceId: number | undefined): boolean {
    if (!serviceId) return false;
    return this.selectedServiceIds().includes(serviceId);
  }

  private loadProjects(): void {
    this.loading.set(true);
    this.srvApi.getMyProjects(0, 100).subscribe({
      next: (page) => {
        this.projects.set(page.content);
        this.loading.set(false);
      },
      error: (err) => {
        this.projects.set([]);
        this.loading.set(false);
        this.error.set(resolveHttpError(err, 'Could not load projects. Please try again.'));
      }
    });
  }

  private loadEligibleServices(): void {
    this.srvApi.getEligibleServices(0, 200).subscribe({
      next: (page) => {
        this.eligibleServices.set(page.content || []);
      },
      error: () => {
        this.eligibleServices.set([]);
      }
    });
  }

  private syncProjectServices(projectId: number) {
    const selected = this.selectedServiceIds();
    const original = this.originalServiceIds();

    const toAdd = selected.filter((id) => !original.includes(id));
    const toRemove = original.filter((id) => !selected.includes(id));

    if (toAdd.length === 0 && toRemove.length === 0) {
      return of(null);
    }

    const addCalls = toAdd.map((serviceId) => this.srvApi.addProjectService(projectId, serviceId));
    const removeCalls = toRemove.map((serviceId) => this.srvApi.removeProjectService(projectId, serviceId));
    const calls = [...addCalls, ...removeCalls];

    return forkJoin(calls.length ? calls : [of(null)]).pipe(
      switchMap(() => {
        this.originalServiceIds.set([...selected]);
        return of(null);
      })
    );
  }

  private toProjectPayload(): ProjectRequest {
    const value = this.projectForm.value;
    const today = new Date();
    const defaultStart = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    const defaultEnd = new Date(defaultStart);
    defaultEnd.setDate(defaultEnd.getDate() + 7);

    return {
      title: value.title || '',
      details: value.details || undefined,
      priority: value.priority || 'MEDIUM',
      budget: Number(value.budget || 0),
      startDate: this.fromDateInput(value.startDate) || defaultStart.toISOString(),
      estimatedEndDate: this.fromDateInput(value.estimatedEndDate) || defaultEnd.toISOString(),
      status: ProjectRequest.StatusEnum.Planned
    };
  }

  private resetProjectForm(): void {
    this.editingProjectId.set(null);
    this.selectedServiceIds.set([]);
    this.originalServiceIds.set([]);
    this.projectForm.reset({
      title: '',
      details: '',
      priority: 'MEDIUM',
      budget: 0,
      startDate: '',
      estimatedEndDate: ''
    });
  }

  private toDateInput(value?: string): string {
    if (!value) return '';
    return new Date(value).toISOString().slice(0, 10);
  }

  private fromDateInput(value?: string | null): string | undefined {
    if (!value) return undefined;
    return new Date(`${value}T00:00:00`).toISOString();
  }
}
