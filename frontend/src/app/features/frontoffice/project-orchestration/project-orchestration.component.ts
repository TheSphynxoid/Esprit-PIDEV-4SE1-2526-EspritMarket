import { Component, computed, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { forkJoin } from 'rxjs';

import {
  ProjectDependencyRequest,
  ProjectDependencyResponse,
  ProjectMilestoneRequest,
  ProjectMilestoneResponse,
  ProjectRequest,
  ProjectResponse,
  ProjectTimelineResponse,
  ServiceResponse
} from '@esprit-market/api-types';
import { AuthService, SrvApiService } from '../../../services';
import { HeaderComponent, FooterComponent } from '../../../shared/layout';
import { ParadoxTipComponent } from '../../../shared/components';
import { milestoneStatusClass, severityClass as severityBadgeClass, resolveHttpError } from '../../../shared/utils';

@Component({
  selector: 'app-project-orchestration',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, HeaderComponent, FooterComponent, ParadoxTipComponent],
  templateUrl: './project-orchestration.component.html',
  styleUrls: ['./project-orchestration.component.css']
})
export class ProjectOrchestrationComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly srvApi = inject(SrvApiService);
  private readonly auth = inject(AuthService);

  readonly projectId = signal<number | null>(null);
  readonly project = signal<ProjectResponse | null>(null);
  readonly timeline = signal<ProjectTimelineResponse | null>(null);
  readonly milestones = signal<ProjectMilestoneResponse[]>([]);
  readonly dependencies = signal<ProjectDependencyResponse[]>([]);
  readonly allocationAudit = signal<Array<{
    id?: number;
    serviceId?: number;
    projectId?: number;
    mode?: string;
    slotStart?: string;
    slotEnd?: string;
    finalScore?: number;
    reasonCode?: string;
    policyProfile?: string;
    tieBreakerWeight?: number;
    priorityMarkupApplied?: boolean;
    createdAt?: string;
  }>>([]);
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly activeTab = signal<'overview' | 'milestones' | 'schedule' | 'assistant'>('overview');
  readonly error = signal('');
  readonly success = signal('');
  readonly editingMilestoneId = signal<number | null>(null);
  readonly isReordering = signal(false);
  readonly dragMilestoneId = signal<number | null>(null);
  readonly expandedInsightIds = signal<number[]>([]);
  readonly projectServices = signal<Array<{ id: number; name: string; providerName: string; category: string; bookingCount: number }>>([]);
  readonly recommendationDate = signal(this.todayIso());
  readonly recommendationMode = signal<'PROJECT_FIRST' | 'COMPETITIVE'>('PROJECT_FIRST');
  readonly recommendationServiceId = signal<number | null>(null);
  readonly serviceSearchTerm = signal('');
  readonly recommendationResult = signal<Array<{
    rank?: number;
    start?: string;
    end?: string;
    score?: number;
    reasonCode?: string;
    policyProfile?: string;
    tieBreakerWeight?: number;
    availabilityWeight?: number;
    scarcityWeight?: number;
    projectUrgencyWeight?: number;
    projectProgressWeight?: number;
    reliabilityWeight?: number;
    fairnessWeight?: number;
    modeMultiplier?: number;
  }>>([]);
  readonly isLoadingRecommendations = signal(false);

  readonly riskAssessment = signal<{
    overallRiskScore?: number;
    alerts?: Array<{ severity?: string; type?: string; message?: string; milestoneId?: number; milestoneTitle?: string }>;
    criticalPath?: Array<{ milestoneId?: number; milestoneTitle?: string; sortOrder?: number; chainDepth?: number; isOnCriticalPath?: boolean }>;
  } | null>(null);
  readonly depSuggestions = signal<Array<{
    predecessorMilestoneId?: number; predecessorMilestoneTitle?: string;
    successorMilestoneId?: number; successorMilestoneTitle?: string;
    reason?: string; confidence?: number;
  }>>([]);
  readonly scheduleOptimization = signal<{
    recommendations?: Array<{
      serviceId?: number; serviceName?: string; bookingId?: number;
      recommendedDate?: string; recommendedDuration?: number;
      score?: number; reasonCode?: string; notes?: string[];
    }>;
    optimizationNotes?: string[];
  } | null>(null);
  readonly mlDelayPrediction = signal<{
    onTimeProbability?: number;
    delayRiskLevel?: string;
    estimatedDelayDays?: number;
    keyFactors?: string[];
    recommendation?: string;
  } | null>(null);
  readonly serviceRiskAnalysis = signal<{
    projectId?: number;
    services?: Array<{
      serviceId?: number;
      serviceName?: string;
      category?: string;
      providerName?: string;
      providerId?: number;
      milestoneTitle?: string;
      milestoneId?: number;
      riskLevel?: number;
      completionProbability?: number;
      confidence?: string;
      recommendation?: string;
      keyFactors?: string[];
    }>;
  } | null>(null);
  readonly showTemplateGallery = signal(false);
  readonly workflowTemplates = signal<Array<{ id: string; name: string; description: string; icon: string; milestones: any[] }>>([]);
  readonly applyingTemplate = signal(false);
  readonly progressReport = signal<any>(null);
  readonly showAssistantPanel = signal(false);
  readonly isLoadingAssistant = signal(false);

  readonly projectBookings = signal<Array<{
    id?: number; serviceName?: string; providerName?: string;
    status?: string; date?: string; duration?: number;
  }>>([]);
  readonly linkingMilestoneId = signal<number | null>(null);

  readonly statusMenuOpen = signal(false);

  readonly eligibleServices = signal<ServiceResponse[]>([]);
  readonly showServicePicker = signal(false);
  readonly showMemberAdd = signal(false);
  readonly memberUserId = signal('');
  readonly isEditingProject = signal(false);

  readonly assigningServiceMilestoneId = signal<number | null>(null);
  readonly schedule = signal<{
    projectId?: number;
    milestones?: Array<{
      milestoneId?: number;
      milestoneTitle?: string;
      sortOrder?: number;
      suggestedWeekStart?: string;
      suggestedWeekEnd?: string;
      estimatedDurationDays?: number;
      services?: Array<{
        serviceId?: number;
        serviceName?: string;
        providerName?: string;
        suggestedDate?: string;
        estimatedDuration?: number;
        available?: boolean;
      }>;
    }>;
    projectStartDate?: string;
    projectEndDate?: string;
  } | null>(null);
  readonly isLoadingSchedule = signal(false);
  readonly workflowResult = signal<{
    projectId?: number;
    status?: string;
    createdBookings?: Array<{
      bookingId?: number;
      serviceName?: string;
      providerName?: string;
      date?: string;
      duration?: number;
      milestoneTitle?: string;
    }>;
    skippedMilestones?: string[];
    warnings?: string[];
  } | null>(null);
  readonly isExecutingWorkflow = signal(false);
  readonly showWorkflowResult = signal(false);
  readonly serviceSearchForMilestone = signal('');
  readonly calViewMonth = signal(new Date().toISOString().slice(0, 7));
  readonly calSelectedDate = signal<string | null>(null);

  readonly projectStatusOptions = ['PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'] as const;

  readonly projectEditForm = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(150)]],
    details: ['', [Validators.maxLength(2000)]],
    priority: ['MEDIUM', [Validators.required]],
    budget: [0, [Validators.required, Validators.min(0)]],
    startDate: [''],
    estimatedEndDate: ['']
  });

  readonly priorityOptions = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  readonly NODE_W = 220;
  readonly NODE_H = 110;
  readonly DIAMOND_SIZE = 120;
  readonly PORT_R = 7;

  readonly wfPan = signal({ x: 100, y: 80 });
  readonly wfZoom = signal(1);
  readonly wfNodePositions = signal<Map<number, { x: number; y: number }>>(new Map());
  readonly wfDraggingNodeId = signal<number | null>(null);
  readonly wfDragOffset = signal({ x: 0, y: 0 });
  readonly wfConnectingFrom = signal<number | null>(null);
  readonly wfConnectingSide = signal<'in' | 'out' | 'bottom'>('out');
  readonly wfConnectingMouse = signal({ x: 0, y: 0 });

  readonly wfConnectingFromPortPos = computed(() => {
    const fromId = this.wfConnectingFrom();
    if (fromId == null) return null;
    const positions = this.wfNodePositions();
    const pos = positions.get(fromId);
    if (!pos) return null;
    const node = this.wfNodes().find(n => n.id === fromId);
    const isDiamond = node?.isDiamond;
    const side = this.wfConnectingSide();
    if (side === 'bottom' && isDiamond) {
      return { x: pos.x + this.DIAMOND_SIZE / 2, y: pos.y + this.DIAMOND_SIZE };
    }
    if (isDiamond) {
      return side === 'out'
        ? { x: pos.x + this.DIAMOND_SIZE, y: pos.y + this.DIAMOND_SIZE / 2 }
        : { x: pos.x, y: pos.y + this.DIAMOND_SIZE / 2 };
    }
    return side === 'out'
      ? { x: pos.x + this.NODE_W, y: pos.y + this.NODE_H / 2 }
      : { x: pos.x, y: pos.y + this.NODE_H / 2 };
  });

  readonly wfSearchTerm = signal('');
  readonly wfSearchOpen = signal(false);
  readonly wfContextMenuNodeId = signal<number | null>(null);
  readonly wfContextMenuPos = signal({ x: 0, y: 0 });
  readonly wfIsPanning = signal(false);
  readonly wfPanStart = signal({ x: 0, y: 0 });
  readonly wfEditingTitle = signal('');
  readonly decomposeInput = signal('');
  readonly decomposing = signal(false);
  readonly decomposition = signal<any>(null);
  readonly applyingDecomposition = signal(false);
  readonly scopeChanges = signal<any[]>([]);
  readonly delayPrediction = signal<any>(null);
  readonly isApplyingDependencies = signal(false);

  bulkApplyDependencies(): void {
    if (!this.canManage() || !this.projectId() || this.depSuggestions().length === 0) return;
    
    this.isApplyingDependencies.set(true);
    this.error.set('');
    
    const requests = this.depSuggestions().map(s => ({
      predecessorMilestoneId: s.predecessorMilestoneId!,
      successorMilestoneId: s.successorMilestoneId!
    }));

    this.srvApi.bulkApplyDependencies(this.projectId()!, requests).subscribe({
      next: (response) => {
        this.isApplyingDependencies.set(false);
        this.success.set(`Applied ${response.length} dependency suggestions successfully.`);
        this.loadAll();
      },
      error: (err) => {
        this.isApplyingDependencies.set(false);
        this.error.set(resolveHttpError(err, 'Failed to apply dependencies.'));
      }
    });
  }
  readonly wfPinchStartDist = signal(0);
  readonly wfPinchStartZoom = signal(1);
  readonly wfTouchStartTime = signal(0);
  readonly wfTouchStartPos = signal({ x: 0, y: 0 });
  readonly wfLongPressTimer = signal<ReturnType<typeof setTimeout> | null>(null);
  readonly wfLastTapTime = signal(0);
  readonly wfLastTapNodeId = signal<number | null>(null);

  readonly canManage = computed(() => {
    const user = this.auth.currentUser();
    const proj = this.project();
    if (!user) return false;
    if (user.role === 'admin_market') return true;
    return !!proj && proj.creatorId === user.id;
  });

  readonly filteredProjectServices = computed(() => {
    const term = this.serviceSearchTerm().toLowerCase().trim();
    let list = this.projectServices();
    if (term) {
      list = list.filter(s =>
        s.name.toLowerCase().includes(term) ||
        s.providerName.toLowerCase().includes(term) ||
        s.category.toLowerCase().includes(term)
      );
    }
    return list.sort((a, b) => b.bookingCount - a.bookingCount);
  });

  readonly wfNodes = computed(() => {
    const positions = this.wfNodePositions();
    const ms = this.milestones();
    const editingId: number | null = null;
    const editableStatuses = new Set(['PLANNED', 'IN_PROGRESS', 'BLOCKED']);
    return ms.map((m, idx) => {
      const pos = positions.get(m.id!);
      const services: any[] = (m as any).services || [];
      const bookingIds: number[] = m.linkedBookingIds || [];
      return {
        id: m.id!,
        title: m.title || 'Untitled',
        status: m.status || 'PLANNED',
        milestoneType: (m as any).milestoneType || 'MILESTONE',
        conditionExpression: (m as any).conditionExpression || '',
        x: pos?.x ?? (idx * 260 + 50),
        y: pos?.y ?? 50,
        isEditable: editableStatuses.has(m.status || 'PLANNED'),
        isEditing: false,
        isSelected: false,
        isDiamond: (m as any).milestoneType === 'CONDITION',
        svcCount: services.length,
        bookingCount: bookingIds.length,
        services: services.slice(0, 3).map((s: any) => s.name || `SVC#${s.id}`).join(', '),
        index: idx
      };
    });
  });

  readonly wfEdges = computed(() => {
    const positions = this.wfNodePositions();
    const deps = this.dependencies();
    const nodes = this.wfNodes();
    
    // Group outgoing edges to assign different ports for condition nodes
    const outCountMap = new Map<number, number>();

    return deps.map(d => {
      const fromPos = positions.get(d.predecessorMilestoneId!);
      const toPos = positions.get(d.successorMilestoneId!);
      const fromNode = nodes.find(n => n.id === d.predecessorMilestoneId);
      const toNode = nodes.find(n => n.id === d.successorMilestoneId);
      
      const fromId = d.predecessorMilestoneId!;
      const currentOutCount = outCountMap.get(fromId) || 0;
      outCountMap.set(fromId, currentOutCount + 1);

      const fromW = fromNode?.isDiamond ? this.DIAMOND_SIZE : this.NODE_W;
      const fromH = fromNode?.isDiamond ? this.DIAMOND_SIZE : this.NODE_H;
      const toH = toNode?.isDiamond ? this.DIAMOND_SIZE : this.NODE_H;
      
      let fromX = (fromPos?.x ?? 0) + fromW;
      let fromY = (fromPos?.y ?? 0) + fromH / 2;
      let isBottomPort = false;

      // If it's a diamond and it's the second (or later) outgoing edge, draw from the bottom port
      if (fromNode?.isDiamond && currentOutCount >= 1) {
        fromX = (fromPos?.x ?? 0) + fromW / 2;
        fromY = (fromPos?.y ?? 0) + fromH;
        isBottomPort = true;
      }

      return {
        id: d.id!,
        fromId: d.predecessorMilestoneId!,
        toId: d.successorMilestoneId!,
        fromX,
        fromY,
        toX: (toPos?.x ?? 0) - 10,
        toY: ((toPos?.y) ?? 0) + toH / 2,
        isBottomPort
      };
    });
  });

  readonly wfSearchResults = computed(() => {
    const term = this.wfSearchTerm().toLowerCase().trim();
    if (!term) return [];
    return this.wfNodes().filter(n =>
      n.title.toLowerCase().includes(term) ||
      n.status.toLowerCase().includes(term) ||
      (n.services || '').toLowerCase().includes(term)
    ).slice(0, 8);
  });

  readonly wfSearchMatchIds = computed(() => {
    const results = this.wfSearchResults();
    return new Set(results.map(r => r.id));
  });

  readonly wfEditableEdgeIds = computed(() => {
    const editableNodeIds = new Set(this.wfNodes().filter(n => n.isEditable).map(n => n.id));
    const ids = new Set<number>();
    for (const edge of this.wfEdges()) {
      if (editableNodeIds.has(edge.fromId) || editableNodeIds.has(edge.toId)) {
        ids.add(edge.id);
      }
    }
    return ids;
  });

  readonly milestoneForm = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(150)]],
    details: ['', [Validators.maxLength(2000)]],
    plannedStartDate: [''],
    plannedEndDate: [''],
    actualStartDate: [''],
    actualEndDate: [''],
    status: this.fb.control<ProjectMilestoneRequest.StatusEnum>(ProjectMilestoneRequest.StatusEnum.Planned, { validators: [Validators.required], nonNullable: true }),
    milestoneType: ['MILESTONE'],
    conditionExpression: [''],
    sortOrder: [0, [Validators.required, Validators.min(0)]]
  });

  readonly dependencyForm = this.fb.group({
    predecessorMilestoneId: [0, [Validators.required, Validators.min(1)]],
    successorMilestoneId: [0, [Validators.required, Validators.min(1)]]
  });

  readonly milestoneStatusOptions = [
    ProjectMilestoneRequest.StatusEnum.Planned,
    ProjectMilestoneRequest.StatusEnum.InProgress,
    ProjectMilestoneRequest.StatusEnum.Completed,
    ProjectMilestoneRequest.StatusEnum.Blocked,
    ProjectMilestoneRequest.StatusEnum.Cancelled
  ];

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) {
      this.error.set('Invalid project id.');
      this.isLoading.set(false);
      return;
    }

    this.projectId.set(id);
    this.loadAll();
  }

  loadAll(): void {
    const id = this.projectId();
    if (!id) return;
    this.isLoading.set(true);
    this.error.set('');

    this.srvApi.getProjectById(id).subscribe({
      next: (project) => {
        this.project.set(project);
      },
      error: () => {
        this.project.set(null);
      }
    });

    this.srvApi.getProjectTimeline(id).subscribe({
      next: (timeline) => {
        this.timeline.set(timeline);
        this.milestones.set([...(timeline.milestones ?? [])].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0)));
        this.dependencies.set(timeline.dependencies ?? []);
        if (this.wfNodePositions().size === 0) {
          this.initWorkflowLayout();
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set(resolveHttpError(err, 'Could not load project data.'));
        this.isLoading.set(false);
      }
    });

    this.srvApi.getProjectAllocationAudit(id).subscribe({
      next: (entries) => this.allocationAudit.set(entries || []),
      error: () => this.allocationAudit.set([])
    });

    this.srvApi.getProjectBookings(id, 0, 200).subscribe({
      next: (page) => {
        const map = new Map<number, { name: string; providerName: string; category: string; bookingCount: number }>();
        page.content.forEach((b) => {
          if (b.serviceId) {
            const existing = map.get(b.serviceId);
            const entry = {
              name: b.serviceName || `Service #${b.serviceId}`,
              providerName: b.providerName || 'Provider',
              category: '',
              bookingCount: (existing?.bookingCount ?? 0) + 1
            };
            map.set(b.serviceId, entry);
          }
        });
        this.projectBookings.set(page.content.map(b => ({
          id: b.id,
          serviceName: b.serviceName,
          providerName: b.providerName,
          status: b.status,
          date: b.date,
          duration: b.duration
        })));
        if (map.size > 0) {
          const services = Array.from(map.entries()).map(([serviceId, data]) => ({
            id: serviceId, ...data
          }));
          this.projectServices.set(services);
          if (!this.recommendationServiceId() && services.length > 0) {
            this.recommendationServiceId.set(services[0].id);
          }
        } else {
          this.loadEligibleServices();
        }
      },
      error: () => this.loadEligibleServices()
    });

    this.srvApi.getEligibleServices(0, 200).subscribe({
      next: (page) => this.eligibleServices.set(page.content || []),
      error: () => this.eligibleServices.set([])
    });
  }

  runProjectRecommendations(): void {
    const projectId = this.projectId();
    const serviceId = this.recommendationServiceId();
    const date = this.recommendationDate();
    if (!projectId || !serviceId || !date) {
      return;
    }

    this.isLoadingRecommendations.set(true);
    this.error.set('');

    this.srvApi.getProjectSlotSuggestions(projectId, serviceId, date, date, this.recommendationMode(), 5).subscribe({
      next: (response) => {
        const items = (response.suggestions || []).map((s) => ({
          rank: s.rank,
          start: s.slot?.start,
          end: s.slot?.end,
          score: s.score?.finalScore,
          reasonCode: s.score?.reasonCode,
          policyProfile: s.score?.policyProfile,
          tieBreakerWeight: s.score?.tieBreakerWeight,
          availabilityWeight: s.score?.availabilityWeight,
          scarcityWeight: s.score?.scarcityWeight,
          projectUrgencyWeight: s.score?.projectUrgencyWeight,
          projectProgressWeight: s.score?.projectProgressWeight,
          reliabilityWeight: s.score?.reliabilityWeight,
          fairnessWeight: s.score?.fairnessWeight,
          modeMultiplier: s.score?.modeMultiplier
        }));
        this.recommendationResult.set(items);
        this.isLoadingRecommendations.set(false);
      },
      error: (err) => {
        this.error.set(resolveHttpError(err, 'Failed to compute project recommendations.'));
        this.recommendationResult.set([]);
        this.isLoadingRecommendations.set(false);
      }
    });
  }

  saveMilestone(): void {
    if (!this.canManage() || this.milestoneForm.invalid || !this.projectId()) return;

    this.isSubmitting.set(true);
    this.success.set('');
    this.error.set('');

    const payload = this.buildMilestonePayload();
    const projectId = this.projectId()!;
    const milestoneId = this.editingMilestoneId();

    const request$ = milestoneId
      ? this.srvApi.updateProjectMilestone(projectId, milestoneId, payload)
      : this.srvApi.createProjectMilestone(projectId, payload);

    request$.subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.success.set(milestoneId ? 'Milestone updated.' : 'Milestone created.');
        this.resetMilestoneForm();
        this.loadAll();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.error.set(resolveHttpError(err, 'Could not save the milestone. Please try again.'));
      }
    });
  }

  editMilestone(milestone: ProjectMilestoneResponse): void {
    this.editingMilestoneId.set(milestone.id ?? null);
    this.milestoneForm.setValue({
      title: milestone.title ?? '',
      details: milestone.details ?? '',
      plannedStartDate: this.toDateInput(milestone.plannedStartDate),
      plannedEndDate: this.toDateInput(milestone.plannedEndDate),
      actualStartDate: this.toDateInput(milestone.actualStartDate),
      actualEndDate: this.toDateInput(milestone.actualEndDate),
      status: milestone.status ?? ProjectMilestoneRequest.StatusEnum.Planned,
      milestoneType: (milestone as any).milestoneType || 'MILESTONE',
      conditionExpression: (milestone as any).conditionExpression || '',
      sortOrder: milestone.sortOrder ?? 0
    });
  }

  cancelEditMilestone(): void {
    this.resetMilestoneForm();
  }

  deleteMilestone(milestone: ProjectMilestoneResponse): void {
    if (!this.canManage() || !this.projectId() || !milestone.id) return;
    if (!confirm('Delete this milestone?')) return;

    this.srvApi.deleteProjectMilestone(this.projectId()!, milestone.id).subscribe({
      next: () => {
        this.success.set('Milestone deleted.');
        this.loadAll();
      },
      error: (err) => {
        this.error.set(resolveHttpError(err, 'Could not delete the milestone. Please try again.'));
      }
    });
  }

  onMilestoneDragStart(milestoneId: number | undefined): void {
    if (!milestoneId || !this.canManage()) return;
    this.dragMilestoneId.set(milestoneId);
  }

  onMilestoneDragEnd(): void {
    this.dragMilestoneId.set(null);
  }

  onMilestoneDragOver(event: DragEvent): void {
    event.preventDefault();
  }

  onMilestoneDrop(targetMilestoneId: number | undefined): void {
    if (!this.canManage() || !targetMilestoneId) return;

    const sourceMilestoneId = this.dragMilestoneId();
    if (!sourceMilestoneId || sourceMilestoneId === targetMilestoneId) {
      this.dragMilestoneId.set(null);
      return;
    }

    const current = [...this.milestones()];
    const sourceIndex = current.findIndex((m) => m.id === sourceMilestoneId);
    const targetIndex = current.findIndex((m) => m.id === targetMilestoneId);
    if (sourceIndex < 0 || targetIndex < 0) {
      this.dragMilestoneId.set(null);
      return;
    }

    const [moved] = current.splice(sourceIndex, 1);
    current.splice(targetIndex, 0, moved);
    this.milestones.set(current.map((m, index) => ({ ...m, sortOrder: index })));
    this.dragMilestoneId.set(null);

    this.persistMilestoneOrder();
  }

  moveMilestone(milestoneId: number | undefined, direction: -1 | 1): void {
    if (!this.canManage() || !milestoneId) return;
    const current = [...this.milestones()];
    const index = current.findIndex((m) => m.id === milestoneId);
    if (index < 0) return;

    const target = index + direction;
    if (target < 0 || target >= current.length) return;

    const [moved] = current.splice(index, 1);
    current.splice(target, 0, moved);
    this.milestones.set(current.map((m, i) => ({ ...m, sortOrder: i })));
    this.persistMilestoneOrder();
  }

  createDependency(): void {
    if (!this.canManage() || this.dependencyForm.invalid || !this.projectId()) return;

    const predecessorMilestoneId = Number(this.dependencyForm.value.predecessorMilestoneId);
    const successorMilestoneId = Number(this.dependencyForm.value.successorMilestoneId);
    const payload: ProjectDependencyRequest = { predecessorMilestoneId, successorMilestoneId };

    this.isSubmitting.set(true);
    this.error.set('');
    this.success.set('');

    this.srvApi.createProjectDependency(this.projectId()!, payload).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.success.set('Dependency created.');
        this.dependencyForm.reset({ predecessorMilestoneId: 0, successorMilestoneId: 0 });
        this.loadAll();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.error.set(resolveHttpError(err, 'Could not create the dependency. Please try again.'));
      }
    });
  }

  deleteDependency(dependency: ProjectDependencyResponse): void {
    if (!this.canManage() || !this.projectId() || !dependency.id) return;
    if (!confirm('Delete this dependency?')) return;

    this.srvApi.deleteProjectDependency(this.projectId()!, dependency.id).subscribe({
      next: () => {
        this.success.set('Dependency deleted.');
        this.loadAll();
      },
      error: (err) => {
        this.error.set(resolveHttpError(err, 'Could not remove the dependency. Please try again.'));
      }
    });
  }

  toggleInsight(id: number | undefined): void {
    if (!id) return;
    this.expandedInsightIds.update((ids) => ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id]);
  }

  isInsightExpanded(id: number | undefined): boolean {
    if (!id) return false;
    return this.expandedInsightIds().includes(id);
  }

  insightBadgeClass(reasonCode?: string): string {
    switch (reasonCode) {
      case 'HIGH_PRIORITY_MATCH':
        return 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-400';
      case 'COMPETITIVE_ADVANTAGE':
        return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400';
      case 'GOOD_BALANCED_MATCH':
        return 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400';
      case 'POOR_MATCH':
        return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400';
      case 'LOW_PRIORITY_MATCH':
        return 'bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-400';
      default:
        return 'bg-neutral-100 text-neutral-700 dark:bg-neutral-700 dark:text-neutral-300';
    }
  }

  ideasForInsight(insight: {
    finalScore?: number;
    reasonCode?: string;
    tieBreakerWeight?: number;
    mode?: string;
    reliabilityWeight?: number;
    fairnessWeight?: number;
    projectUrgencyWeight?: number;
  }): string[] {
    const ideas: string[] = [];
    const score = insight.finalScore ?? 0;
    const tie = insight.tieBreakerWeight ?? 0;
    const mode = insight.mode ?? 'PROJECT_FIRST';
    const reliability = insight.reliabilityWeight ?? 0.5;
    const fairness = insight.fairnessWeight ?? 0.5;
    const urgency = insight.projectUrgencyWeight ?? 0.5;

    if (score >= 0.85) {
      ideas.push('Use this window as a preferred booking target for critical milestones.');
    } else if (score >= 0.65) {
      ideas.push('Good candidate slot; validate with dependency chain before confirming.');
    } else {
      ideas.push('Lower-priority slot; keep as fallback option if preferred windows fill up.');
    }

    if (mode === 'COMPETITIVE') {
      ideas.push('Competitive mode favored this slot; lock it quickly to avoid contention.');
    } else {
      ideas.push('Project-first mode favored continuity; align adjacent tasks around this slot.');
    }

    if (tie >= 0.8) {
      ideas.push('Strong tie-breaker advantage: prioritize this slot when scores are close.');
    } else if (tie <= 0.5) {
      ideas.push('Weak tie-breaker edge: keep at least one backup slot in the same day.');
    }

    if (reliability >= 0.8) {
      ideas.push('High reliability score: this provider has a strong track record for on-time delivery.');
    } else if (reliability <= 0.3) {
      ideas.push('Low reliability signal: consider adding buffer time or a backup provider.');
    }

    if (fairness >= 0.8) {
      ideas.push('Strong fairness distribution: this slot helps balance workload across providers.');
    } else if (fairness <= 0.3) {
      ideas.push('Low fairness weight: other providers may be over-committed; monitor for bottlenecks.');
    }

    if (urgency >= 0.8) {
      ideas.push('Project deadline is approaching — treat this slot as high urgency.');
    }

    if (insight.reasonCode === 'POOR_MATCH') {
      ideas.push('Poor match: strongly consider rescheduling or switching service providers.');
    } else if (insight.reasonCode === 'LOW_PRIORITY_MATCH' || insight.reasonCode === 'MODERATE_MATCH') {
      ideas.push('Consider adjusting milestone sequence to unlock higher quality windows.');
    }

    return ideas;
  }

  modeLabel(mode?: string): string {
    if (mode === 'COMPETITIVE') return 'Standard';
    return 'High Priority';
  }

  statusClass(status: string | undefined): string {
    return milestoneStatusClass(status);
  }

  private buildMilestonePayload(): ProjectMilestoneRequest {
    const value = this.milestoneForm.value;

    return {
      title: value.title ?? '',
      details: value.details || undefined,
      plannedStartDate: this.fromDateInput(value.plannedStartDate),
      plannedEndDate: this.fromDateInput(value.plannedEndDate),
      actualStartDate: this.fromDateInput(value.actualStartDate),
      actualEndDate: this.fromDateInput(value.actualEndDate),
      status: value.status ?? ProjectMilestoneRequest.StatusEnum.Planned,
      milestoneType: (value.milestoneType as any) || undefined,
      conditionExpression: value.conditionExpression || undefined,
      sortOrder: Number(value.sortOrder ?? 0)
    };
  }

  private persistMilestoneOrder(): void {
    const projectId = this.projectId();
    if (!projectId) return;

    const orderedIds = this.milestones()
      .filter(m => m.id != null)
      .map(m => m.id!);

    if (orderedIds.length === 0) return;

    this.isReordering.set(true);
    this.error.set('');

    this.srvApi.reorderProjectMilestones(projectId, orderedIds).subscribe({
      next: () => {
        this.isReordering.set(false);
        this.success.set('Milestone order updated.');
        this.loadAll();
      },
      error: (err) => {
        this.isReordering.set(false);
        this.error.set(resolveHttpError(err, 'Could not save the milestone order. Please try again.'));
        this.loadAll();
      }
    });
  }

  private toMilestoneRequest(milestone: ProjectMilestoneResponse, sortOrder: number): ProjectMilestoneRequest {
    return {
      title: milestone.title || '',
      details: milestone.details || undefined,
      plannedStartDate: milestone.plannedStartDate,
      plannedEndDate: milestone.plannedEndDate,
      actualStartDate: milestone.actualStartDate,
      actualEndDate: milestone.actualEndDate,
      status: milestone.status || ProjectMilestoneRequest.StatusEnum.Planned,
      sortOrder
    };
  }

  private resetMilestoneForm(): void {
    this.editingMilestoneId.set(null);
    this.milestoneForm.reset({
      title: '',
      details: '',
      plannedStartDate: '',
      plannedEndDate: '',
      actualStartDate: '',
      actualEndDate: '',
      status: ProjectMilestoneRequest.StatusEnum.Planned,
      milestoneType: 'MILESTONE',
      conditionExpression: '',
      sortOrder: 0
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

  private loadEligibleServices(): void {
    this.srvApi.getEligibleServices(0, 200).subscribe({
      next: (page) => {
        const services = page.content.map(s => ({
          id: s.id!,
          name: s.name || `Service #${s.id}`,
          providerName: s.providerName || 'Provider',
          category: s.category || '',
          bookingCount: 0
        }));
        this.projectServices.set(services);
        if (!this.recommendationServiceId() && services.length > 0) {
          this.recommendationServiceId.set(services[0].id);
        }
      },
      error: () => this.projectServices.set([])
    });
  }

  toggleAssistantPanel(): void {
    if (!this.riskAssessment()) {
      this.loadAssistantData();
    }
    if (!this.mlDelayPrediction()) {
      this.srvApi.getProjectDelayPrediction(this.projectId()!).subscribe({
        next: (data) => this.mlDelayPrediction.set(data),
        error: () => this.mlDelayPrediction.set(null)
      });
    }
    this.activeTab.set('assistant');
  }

  onTabChange(tab: 'overview' | 'milestones' | 'schedule' | 'assistant'): void {
    this.activeTab.set(tab);
    if (tab === 'assistant' && !this.riskAssessment()) {
      this.loadAssistantData();
    }
  }

  private loadAssistantData(): void {
    const id = this.projectId();
    if (!id) return;
    this.loadProgressReport();
    this.isLoadingAssistant.set(true);

    this.srvApi.getRiskAssessment(id).subscribe({
      next: (data) => { this.riskAssessment.set(data); this.isLoadingAssistant.set(false); },
      error: () => { this.riskAssessment.set(null); this.isLoadingAssistant.set(false); }
    });

    this.http.get(`/api/srv/projects/${id}/ml-delay-prediction`).subscribe({
      next: (data: any) => this.delayPrediction.set(data),
      error: () => this.delayPrediction.set(null)
    });

    this.srvApi.getDependencySuggestions(id).subscribe({
      next: (data) => this.depSuggestions.set(data || []),
      error: () => this.depSuggestions.set([])
    });

    this.srvApi.getScheduleOptimization(id).subscribe({
      next: (data) => this.scheduleOptimization.set(data),
      error: () => this.scheduleOptimization.set(null)
    });

    this.srvApi.getProjectDelayPrediction(id).subscribe({
      next: (data) => this.mlDelayPrediction.set(data),
      error: () => {
        this.mlDelayPrediction.set({
          onTimeProbability: 0.65,
          delayRiskLevel: 'MEDIUM',
          keyFactors: ['ML service unavailable'],
          recommendation: 'Could not load ML prediction.'
        });
      }
    });

    this.srvApi.getServiceRiskAnalysis(id).subscribe({
      next: (data) => this.serviceRiskAnalysis.set(data),
      error: () => this.serviceRiskAnalysis.set(null)
    });
  }

  clampPercent(val: number): number {
    return Math.min(Math.max(val, 0), 100);
  }

  loadProgressReport(): void {
    const id = this.projectId();
    if (!id) return;
    this.http.get(`/api/srv/projects/${id}/progress-report`).subscribe({
      next: (data: any) => this.progressReport.set(data),
      error: () => this.progressReport.set(null)
    });
    this.loadScopeChanges(id);
  }

  loadScopeChanges(id: number): void {
    this.http.get<any[]>(`/api/srv/projects/${id}/scope-changes`).subscribe({
      next: (data) => this.scopeChanges.set(data || []),
      error: () => this.scopeChanges.set([])
    });
  }

  decomposeProject(): void {
    const desc = this.decomposeInput().trim();
    if (!desc) return;
    this.decomposing.set(true);
    this.http.post(`/api/srv/projects/assistant/decompose`, { description: desc }).subscribe({
      next: (data: any) => {
        this.decomposing.set(false);
        this.decomposition.set(data);
      },
      error: () => {
        this.decomposing.set(false);
        this.decomposition.set(null);
      }
    });
  }

  applyDecomposition(): void {
    const id = this.projectId();
    const desc = this.decomposeInput().trim();
    if (!desc || !id || !this.canManage()) return;
    this.applyingDecomposition.set(true);
    this.http.post(`/api/srv/projects/${id}/assistant/decompose-apply`, { description: desc }).subscribe({
      next: () => {
        this.applyingDecomposition.set(false);
        this.decomposition.set(null);
        this.decomposeInput.set('');
        this.loadAll();
      },
      error: () => this.applyingDecomposition.set(false)
    });
  }

  applyTemplates(): void {
    const id = this.projectId();
    if (!id || !this.canManage()) return;
    this.isLoadingAssistant.set(true);

    this.srvApi.getMilestoneTemplates(id).subscribe({
      next: (data) => {
        const templates = data.milestones || [];
        let pending = templates.length;
        if (pending === 0) {
          this.isLoadingAssistant.set(false);
          return;
        }
        templates.forEach(t => {
          this.srvApi.createProjectMilestone(id, {
            title: t.title || 'Untitled',
            details: t.details || undefined,
            status: 'PLANNED' as any,
            sortOrder: t.sortOrder ?? 0,
            plannedStartDate: undefined,
            plannedEndDate: undefined,
            actualStartDate: undefined,
            actualEndDate: undefined,
          }).subscribe({
            next: () => { pending--; if (pending === 0) { this.isLoadingAssistant.set(false); this.loadAll(); } },
            error: () => { pending--; if (pending === 0) { this.isLoadingAssistant.set(false); this.loadAll(); } }
          });
        });
      },
      error: () => this.isLoadingAssistant.set(false)
    });
  }

  loadWorkflowTemplates(): void {
    this.http.get('/api/srv/projects/assistant/workflow-templates').subscribe({
      next: (data: any) => {
        this.workflowTemplates.set(data || []);
      },
      error: () => {}
    });
  }

  applyWorkflowTemplate(templateId: string): void {
    const id = this.projectId();
    if (!id || !this.canManage()) return;
    this.applyingTemplate.set(true);
    this.http.get<any>(`/api/srv/projects/${id}/assistant/apply-template/${templateId}`).subscribe({
      next: (data: any) => {
        const templates = data?.milestones || [];
        let idx = 0;
        const obs$ = templates.map((t: any) => this.srvApi.createProjectMilestone(id, {
          title: t.title || 'Untitled',
          details: t.details || undefined,
          status: 'PLANNED' as any,
          sortOrder: t.sortOrder ?? idx++
        }));
        forkJoin(obs$).subscribe({
          next: () => {
            this.showTemplateGallery.set(false);
            this.applyingTemplate.set(false);
            this.loadAll();
          },
          error: () => {
            this.applyingTemplate.set(false);
            this.loadAll();
          }
        });
      },
      error: () => this.applyingTemplate.set(false)
    });
  }

  applyDepSuggestion(s: { predecessorMilestoneId?: number; successorMilestoneId?: number }): void {
    const id = this.projectId();
    if (!id || !this.canManage() || !s.predecessorMilestoneId || !s.successorMilestoneId) return;

    this.srvApi.createProjectDependency(id, {
      predecessorMilestoneId: s.predecessorMilestoneId,
      successorMilestoneId: s.successorMilestoneId
    }).subscribe({
      next: () => { this.success.set('Dependency created.'); this.loadAll(); this.loadAssistantData(); },
      error: (err) => { this.error.set(resolveHttpError(err, 'Could not create the dependency. Please try again.')); }
    });
  }

  severityClass(severity?: string): string {
    return severityBadgeClass(severity);
  }

  serviceRiskLabel(level?: number): string {
    switch (level) {
      case 1: return 'LOW';
      case 2: return 'MODERATE';
      case 3: return 'HIGH';
      case 4: return 'CRITICAL';
      default: return 'UNKNOWN';
    }
  }

  serviceRiskClass(level?: number): string {
    switch (level) {
      case 1: return 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/30 dark:text-emerald-400';
      case 2: return 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400';
      case 3: return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400';
      case 4: return 'bg-red-200 text-red-900 dark:bg-red-900/40 dark:text-red-300';
      default: return 'bg-neutral-100 text-neutral-700 dark:bg-neutral-700 dark:text-neutral-300';
    }
  }

  changeProjectStatus(status: string): void {
    const id = this.projectId();
    if (!id || !this.canManage()) return;

    this.isSubmitting.set(true);
    this.error.set('');
    this.statusMenuOpen.set(false);

    this.srvApi.updateProjectStatus(id, status).subscribe({
      next: (updated) => {
        this.project.set(updated);
        this.isSubmitting.set(false);
        this.success.set(`Project status changed to ${status}.`);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.error.set(resolveHttpError(err, 'Could not update the project status. Please try again.'));
      }
    });
  }

  executeWorkflow(): void {
    const id = this.projectId();
    if (!id || !this.canManage()) return;

    this.isSubmitting.set(true);
    this.error.set('');
    this.success.set('');

    this.srvApi.executeProjectWorkflow(id).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.success.set('Workflow executed. Milestones were auto-updated from dependency and booking states.');
        this.loadAll();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.error.set(resolveHttpError(err, 'Could not execute the workflow. Please try again.'));
      }
    });
  }

  replanProject(): void {
    const id = this.projectId();
    if (!id || !this.canManage()) return;

    this.isSubmitting.set(true);
    this.error.set('');
    this.success.set('');

    this.srvApi.replanProject(id).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.success.set('Project replanned from dependency graph constraints.');
        this.loadAll();
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.error.set(resolveHttpError(err, 'Could not replan the project. Please try again.'));
      }
    });
  }

  toggleStatusMenu(): void {
    this.statusMenuOpen.update(v => !v);
  }

  availableStatusTransitions(): string[] {
    const current = this.project()?.status;
    switch (current) {
      case 'PLANNED': return ['IN_PROGRESS', 'CANCELLED'];
      case 'IN_PROGRESS': return ['COMPLETED', 'CANCELLED', 'PLANNED'];
      case 'COMPLETED': return ['IN_PROGRESS'];
      case 'CANCELLED': return ['PLANNED'];
      default: return [];
    }
  }

  removeMember(userId: number): void {
    const id = this.projectId();
    if (!id || !this.canManage()) return;
    if (!confirm('Remove this member from the project?')) return;

    this.srvApi.removeProjectMember(id, userId).subscribe({
      next: (updated) => {
        this.project.set(updated);
        this.success.set('Member removed.');
      },
      error: (err) => {
        this.error.set(resolveHttpError(err, 'Could not remove the team member. Please try again.'));
      }
    });
  }

  startLinkBooking(milestoneId: number): void {
    this.linkingMilestoneId.set(milestoneId);
  }

  cancelLinkBooking(): void {
    this.linkingMilestoneId.set(null);
  }

  linkBookingToMilestone(bookingId: number): void {
    const projectId = this.projectId();
    const milestoneId = this.linkingMilestoneId();
    if (!projectId || !milestoneId) return;

    this.srvApi.linkBookingToMilestone(projectId, milestoneId, bookingId).subscribe({
      next: () => {
        this.linkingMilestoneId.set(null);
        this.success.set('Booking linked to milestone.');
        this.loadAll();
      },
      error: (err) => {
        this.error.set(resolveHttpError(err, 'Could not link the booking. Please try again.'));
      }
    });
  }

  unlinkBookingFromMilestone(milestoneId: number, bookingId: number): void {
    const projectId = this.projectId();
    if (!projectId || !this.canManage()) return;

    this.srvApi.unlinkBookingFromMilestone(projectId, milestoneId, bookingId).subscribe({
      next: () => {
        this.success.set('Booking unlinked from milestone.');
        this.loadAll();
      },
      error: (err) => {
        this.error.set(resolveHttpError(err, 'Could not unlink the booking. Please try again.'));
      }
    });
  }

  unlinkedBookings(): Array<{ id?: number; serviceName?: string; providerName?: string; status?: string }> {
    const allIds = new Set<number>();
    this.milestones().forEach(m => (m.linkedBookingIds || []).forEach((id: number) => allIds.add(id)));
    return this.projectBookings().filter(b => b.id && !allIds.has(b.id));
  }

  getInitials(name?: string): string {
    if (!name) return '??';
    return name.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase() || '??';
  }

  toggleServicePicker(): void {
    this.showServicePicker.update(v => !v);
  }

  addServiceToProject(serviceId: number): void {
    const id = this.projectId();
    if (!id || !this.canManage()) return;

    this.srvApi.addProjectService(id, serviceId).subscribe({
      next: (updated) => {
        this.project.set(updated);
        this.success.set('Service added to project.');
        this.loadAll();
      },
      error: (err) => {
        this.error.set(resolveHttpError(err, 'Could not add the service. Please try again.'));
      }
    });
  }

  removeServiceFromProject(serviceId: number): void {
    const id = this.projectId();
    if (!id || !this.canManage()) return;
    if (!confirm('Remove this service from the project?')) return;

    this.srvApi.removeProjectService(id, serviceId).subscribe({
      next: (updated) => {
        this.project.set(updated);
        this.success.set('Service removed from project.');
        this.loadAll();
      },
      error: (err) => {
        this.error.set(resolveHttpError(err, 'Could not remove the service. Please try again.'));
      }
    });
  }

  isServiceLinked(serviceId: number | undefined): boolean {
    if (!serviceId) return false;
    return (this.project()?.services || []).some(s => s.id === serviceId);
  }

  toggleMemberAdd(): void {
    this.showMemberAdd.update(v => !v);
    this.memberUserId.set('');
  }

  addMemberToProject(): void {
    const id = this.projectId();
    const userId = Number(this.memberUserId());
    if (!id || !userId || isNaN(userId) || !this.canManage()) return;

    this.srvApi.addProjectMember(id, userId).subscribe({
      next: (updated) => {
        this.project.set(updated);
        this.success.set('Member added to project.');
        this.showMemberAdd.set(false);
        this.memberUserId.set('');
      },
      error: (err) => {
        this.error.set(resolveHttpError(err, 'Could not add the team member. Please try again.'));
      }
    });
  }

  startEditProject(): void {
    const p = this.project();
    if (!p) return;
    this.isEditingProject.set(true);
    this.projectEditForm.setValue({
      title: p.title ?? '',
      details: p.details ?? '',
      priority: p.priority ?? 'MEDIUM',
      budget: Number(p.budget ?? 0),
      startDate: this.toDateInput(p.startDate),
      estimatedEndDate: this.toDateInput(p.estimatedEndDate)
    });
  }

  cancelEditProject(): void {
    this.isEditingProject.set(false);
  }

  saveProjectEdit(): void {
    const id = this.projectId();
    if (!id || !this.canManage() || this.projectEditForm.invalid) return;

    this.isSubmitting.set(true);
    this.error.set('');
    this.success.set('');

    const value = this.projectEditForm.value;
    const today = new Date();
    const defaultStart = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    const defaultEnd = new Date(defaultStart);
    defaultEnd.setDate(defaultEnd.getDate() + 7);

    const payload: ProjectRequest = {
      title: value.title || '',
      details: value.details || undefined,
      priority: value.priority || 'MEDIUM',
      budget: Number(value.budget || 0),
      startDate: this.fromDateInput(value.startDate) || defaultStart.toISOString(),
      estimatedEndDate: this.fromDateInput(value.estimatedEndDate) || defaultEnd.toISOString(),
      status: (this.project()?.status || 'PLANNED') as ProjectRequest.StatusEnum
    };

    this.srvApi.updateProject(id, payload).subscribe({
      next: (updated) => {
        this.project.set(updated);
        this.isSubmitting.set(false);
        this.isEditingProject.set(false);
        this.success.set('Project updated.');
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.isEditingProject.set(false);
        this.error.set(resolveHttpError(err, 'Could not update the project. Please try again.'));
      }
    });
  }

  startAssignService(milestoneId: number): void {
    this.assigningServiceMilestoneId.set(milestoneId);
    this.serviceSearchForMilestone.set('');
  }

  cancelAssignService(): void {
    this.assigningServiceMilestoneId.set(null);
    this.serviceSearchForMilestone.set('');
  }

  linkServiceToMilestone(serviceId: number): void {
    const projectId = this.projectId();
    const milestoneId = this.assigningServiceMilestoneId();
    if (!projectId || !milestoneId || !this.canManage()) return;

    this.srvApi.linkServiceToMilestone(projectId, milestoneId, serviceId).subscribe({
      next: () => {
        this.assigningServiceMilestoneId.set(null);
        this.serviceSearchForMilestone.set('');
        this.success.set('Service linked to milestone.');
        this.loadAll();
      },
      error: (err) => {
        this.error.set(resolveHttpError(err, 'Could not link the service. Please try again.'));
      }
    });
  }

  unlinkServiceFromMilestone(milestoneId: number, serviceId: number): void {
    const projectId = this.projectId();
    if (!projectId || !this.canManage()) return;

    this.srvApi.unlinkServiceFromMilestone(projectId, milestoneId, serviceId).subscribe({
      next: () => {
        this.success.set('Service unlinked from milestone.');
        this.loadAll();
      },
      error: (err) => {
        this.error.set(resolveHttpError(err, 'Could not unlink the service. Please try again.'));
      }
    });
  }

  filteredEligibleServicesForMilestone(milestoneId: number | undefined): ServiceResponse[] {
    if (!milestoneId) return [];
    const milestone = this.milestones().find(m => m.id === milestoneId);
    if (!milestone) return [];
    const linkedIds = new Set(((milestone as any).services || []).map((s: any) => s.id));
    const term = this.serviceSearchForMilestone().toLowerCase().trim();
    return this.eligibleServices().filter(s => {
      if (linkedIds.has(s.id)) return false;
      if (term) {
        return (s.name || '').toLowerCase().includes(term) ||
               (s.category || '').toLowerCase().includes(term) ||
               (s.providerName || '').toLowerCase().includes(term);
      }
      return true;
    });
  }

  generateSchedule(): void {
    const id = this.projectId();
    if (!id || !this.canManage()) return;

    this.isLoadingSchedule.set(true);
    this.error.set('');

    this.srvApi.generateSchedule(id).subscribe({
      next: (data) => {
        this.schedule.set(data);
        this.isLoadingSchedule.set(false);
      },
      error: (err) => {
        this.error.set(resolveHttpError(err, 'Could not generate the schedule. Please try again.'));
        this.isLoadingSchedule.set(false);
      }
    });
  }

  updateServiceHours(milestoneId: number, serviceId: number, hours: number): void {
    const projectId = this.projectId();
    if (!projectId || !this.canManage() || hours <= 0) return;
    this.srvApi.updateServiceEstimatedHours(projectId, milestoneId, serviceId, hours).subscribe({
      next: () => { this.success.set('Hours updated.'); this.loadAll(); },
      error: (err) => { this.error.set(resolveHttpError(err, 'Could not update the estimated hours. Please try again.')); }
    });
  }

  allocateAndBook(): void {
    const id = this.projectId();
    if (!id || !this.canManage()) return;
    if (!confirm('This will create bookings for all milestones based on estimated hours. Continue?')) return;

    this.isExecutingWorkflow.set(true);
    this.error.set('');
    this.success.set('');

    this.srvApi.allocateAndBook(id).subscribe({
      next: (data) => {
        this.workflowResult.set(data);
        this.isExecutingWorkflow.set(false);
        this.showWorkflowResult.set(true);
        this.success.set(`Allocated: ${(data.createdBookings?.length || 0)} bookings created.`);
        if ((data.warnings?.length ?? 0) > 0) {
          this.error.set(data.warnings!.join('\n'));
        }
        this.loadAll();
      },
      error: (err) => {
        this.isExecutingWorkflow.set(false);
        this.error.set(resolveHttpError(err, 'Booking allocation failed. Check provider availability and try again.'));
      }
    });
  }

  executeAutomatedWorkflow(): void {
    const id = this.projectId();
    if (!id || !this.canManage()) return;
    if (!confirm('This will automatically create bookings for all milestones and activate the plan. Continue?')) return;

    this.isExecutingWorkflow.set(true);
    this.error.set('');
    this.success.set('');

    this.srvApi.executeAutomatedWorkflow(id).subscribe({
      next: (data) => {
        this.workflowResult.set(data);
        this.isExecutingWorkflow.set(false);
        this.showWorkflowResult.set(true);
        this.success.set(`Workflow executed: ${(data.createdBookings?.length || 0)} bookings created.`);
        this.loadAll();
      },
      error: (err) => {
        this.isExecutingWorkflow.set(false);
        this.error.set(resolveHttpError(err, 'Could not execute the workflow. Please try again.'));
      }
    });
  }

  closeWorkflowResult(): void {
    this.showWorkflowResult.set(false);
    this.workflowResult.set(null);
  }

  calMonthLabel(): string {
    const [y, m] = this.calViewMonth().split('-').map(Number);
    return new Date(y, m - 1, 1).toLocaleDateString('en-US', { month: 'long', year: 'numeric' });
  }

  calWeekDays(): string[] {
    return ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  }

  calDays(): Array<{ date: string; dayNum: number; isCurrentMonth: boolean; isToday: boolean; bookings: Array<{ id?: number; serviceName?: string; providerName?: string; status?: string; date?: string; duration?: number }> }> {
    const [y, m] = this.calViewMonth().split('-').map(Number);
    const first = new Date(y, m - 1, 1);
    const startDay = (first.getDay() + 6) % 7;
    const daysInMonth = new Date(y, m, 0).getDate();
    const todayStr = this.todayIso();
    const bookingsByDate = new Map<string, Array<{ id?: number; serviceName?: string; providerName?: string; status?: string; date?: string; duration?: number }>>();
    for (const b of this.projectBookings()) {
      if (b.date) {
        const key = b.date.slice(0, 10);
        const arr = bookingsByDate.get(key) || [];
        arr.push(b);
        bookingsByDate.set(key, arr);
      }
    }
    const result: Array<{ date: string; dayNum: number; isCurrentMonth: boolean; isToday: boolean; bookings: Array<{ id?: number; serviceName?: string; providerName?: string; status?: string; date?: string; duration?: number }> }> = [];
    for (let i = startDay - 1; i >= 0; i--) {
      const d = new Date(y, m - 1, -i);
      const key = this.dateKey(d);
      result.push({ date: key, dayNum: d.getDate(), isCurrentMonth: false, isToday: key === todayStr, bookings: bookingsByDate.get(key) || [] });
    }
    for (let i = 1; i <= daysInMonth; i++) {
      const d = new Date(y, m - 1, i);
      const key = this.dateKey(d);
      result.push({ date: key, dayNum: i, isCurrentMonth: true, isToday: key === todayStr, bookings: bookingsByDate.get(key) || [] });
    }
    const remaining = 42 - result.length;
    for (let i = 1; i <= remaining; i++) {
      const d = new Date(y, m, i);
      const key = this.dateKey(d);
      result.push({ date: key, dayNum: d.getDate(), isCurrentMonth: false, isToday: key === todayStr, bookings: bookingsByDate.get(key) || [] });
    }
    return result;
  }

  calSelectedDateBookings(): Array<{ id?: number; serviceName?: string; providerName?: string; status?: string; date?: string; duration?: number }> {
    const d = this.calSelectedDate();
    if (!d) return [];
    return this.projectBookings().filter(b => b.date && b.date.slice(0, 10) === d);
  }

  calNav(delta: number): void {
    const [y, m] = this.calViewMonth().split('-').map(Number);
    const d = new Date(y, m - 1 + delta, 1);
    this.calViewMonth.set(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
  }

  calSelectDate(date: string): void {
    this.calSelectedDate.set(this.calSelectedDate() === date ? null : date);
  }

  calBookingClass(status?: string): string {
    switch (status) {
      case 'COMPLETED': return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400';
      case 'CONFIRMED': case 'APPROVED': return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400';
      case 'IN_PROGRESS': return 'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-400';
      case 'CANCELLED': return 'bg-neutral-100 text-neutral-500 line-through';
      case 'REJECTED': return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400';
      case 'DISPUTED': return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400';
      default: return 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400';
    }
  }

  calBookingBarClass(status?: string): string {
    switch (status) {
      case 'COMPLETED': return 'bg-green-500';
      case 'CONFIRMED': case 'APPROVED': return 'bg-blue-500';
      case 'IN_PROGRESS': return 'bg-purple-500';
      case 'CANCELLED': return 'bg-neutral-300';
      case 'REJECTED': case 'DISPUTED': return 'bg-red-500';
      default: return 'bg-amber-500';
    }
  }

  private dateKey(d: Date): string {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }

  private todayIso(): string {
    const d = new Date();
    const m = `${d.getMonth() + 1}`.padStart(2, '0');
    const day = `${d.getDate()}`.padStart(2, '0');
    return `${d.getFullYear()}-${m}-${day}`;
  }

  wfBezierPath(x1: number, y1: number, x2: number, y2: number, isBottomPort: boolean = false): string {
    const dx = Math.abs(x2 - x1) * 0.5;
    if (isBottomPort) {
      // Curve starts going DOWN, then curves into the right side
      const dy = Math.max(50, Math.abs(y2 - y1) * 0.5);
      return `M${x1},${y1} C${x1},${y1 + dy} ${x2 - dx},${y2} ${x2},${y2}`;
    }
    return `M${x1},${y1} C${x1 + dx},${y1} ${x2 - dx},${y2} ${x2},${y2}`;
  }

  wfNodeStatusColor(status: string): string {
    switch (status) {
      case 'PLANNED': return '#8b5cf6';
      case 'IN_PROGRESS': return '#3b82f6';
      case 'COMPLETED': return '#22c55e';
      case 'BLOCKED': return '#ef4444';
      case 'CANCELLED': return '#6b7280';
      default: return '#64748b';
    }
  }

  wfServiceLabels(services: string): string {
    return services || '';
  }

  initWorkflowLayout(): void {
    const ms = this.milestones();
    const deps = this.dependencies();
    if (ms.length === 0) return;

    const adj = new Map<number, number[]>();
    const inDeg = new Map<number, number>();
    const nodeIds = new Set(ms.map(m => m.id!));
    nodeIds.forEach(id => { adj.set(id, []); inDeg.set(id, 0); });
    deps.forEach(d => {
      if (nodeIds.has(d.predecessorMilestoneId!) && nodeIds.has(d.successorMilestoneId!)) {
        adj.get(d.predecessorMilestoneId!)!.push(d.successorMilestoneId!);
        inDeg.set(d.successorMilestoneId!, (inDeg.get(d.successorMilestoneId!) ?? 0) + 1);
      }
    });

    const layers = new Map<number, number>();
    const queue: number[] = [];
    inDeg.forEach((deg, id) => { if (deg === 0) queue.push(id); });
    let layerIndex = 0;
    while (queue.length > 0) {
      const size = queue.length;
      for (let i = 0; i < size; i++) {
        const id = queue.shift()!;
        layers.set(id, layerIndex);
        for (const next of adj.get(id) ?? []) {
          inDeg.set(next, (inDeg.get(next) ?? 1) - 1);
          if (inDeg.get(next) === 0) queue.push(next);
        }
      }
      layerIndex++;
    }

    nodeIds.forEach(id => { if (!layers.has(id)) layers.set(id, layerIndex); });

    const layerGroups = new Map<number, number[]>();
    layers.forEach((layer, id) => {
      if (!layerGroups.has(layer)) layerGroups.set(layer, []);
      layerGroups.get(layer)!.push(id);
    });

    const positions = new Map<number, { x: number; y: number }>();
    const GAP_X = 280;
    const GAP_Y = 160;
    const START_X = 40;
    const START_Y = 40;

    layerGroups.forEach((ids, layer) => {
      ids.forEach((id, idx) => {
        const node = ms.find(m => m.id === id);
        const isDiamond = (node as any)?.milestoneType === 'CONDITION';
        const nodeH = isDiamond ? this.DIAMOND_SIZE : this.NODE_H;
        positions.set(id, {
          x: START_X + layer * GAP_X,
          y: START_Y + idx * GAP_Y
        });
      });
    });

    this.wfNodePositions.set(positions);
  }

  wfFitView(): void {
    const positions = this.wfNodePositions();
    const nodes = this.wfNodes();
    if (positions.size === 0) return;
    let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
    positions.forEach((pos, id) => {
      const node = nodes.find(n => n.id === id);
      const w = node?.isDiamond ? this.DIAMOND_SIZE : this.NODE_W;
      const h = node?.isDiamond ? this.DIAMOND_SIZE : this.NODE_H;
      minX = Math.min(minX, pos.x);
      minY = Math.min(minY, pos.y);
      maxX = Math.max(maxX, pos.x + w);
      maxY = Math.max(maxY, pos.y + h);
    });
    const canvasW = 800;
    const canvasH = 560;
    const contentW = maxX - minX + 100;
    const contentH = maxY - minY + 100;
    const zoom = Math.min(canvasW / contentW, canvasH / contentH, 1.5);
    const cx = (minX + maxX) / 2;
    const cy = (minY + maxY) / 2;
    this.wfZoom.set(zoom);
    this.wfPan.set({ x: canvasW / 2 - cx * zoom, y: canvasH / 2 - cy * zoom });
  }

  wfZoomIn(): void { this.wfZoom.update(z => Math.min(z + 0.15, 3)); }
  wfZoomOut(): void { this.wfZoom.update(z => Math.max(z - 0.15, 0.3)); }

  wfAddMilestone(): void {
    const id = this.projectId();
    if (!id || !this.canManage()) return;
    const count = this.milestones().length;
    this.isSubmitting.set(true);
    this.srvApi.createProjectMilestone(id, {
      title: `Milestone ${count + 1}`,
      status: 'PLANNED' as any,
      sortOrder: count
    }).subscribe({
      next: () => { this.isSubmitting.set(false); this.loadAll(); },
      error: (err) => { this.isSubmitting.set(false); this.error.set(resolveHttpError(err, 'Could not add the milestone. Please try again.')); }
    });
  }

  wfAddCondition(): void {
    const id = this.projectId();
    if (!id || !this.canManage()) return;
    const count = this.milestones().length;
    this.isSubmitting.set(true);
    this.srvApi.createProjectMilestone(id, {
      title: `Condition ${count + 1}`,
      status: 'PLANNED' as any,
      milestoneType: 'CONDITION' as any,
      conditionExpression: 'deliverable_approved',
      sortOrder: count
    }).subscribe({
      next: () => { this.isSubmitting.set(false); this.loadAll(); },
      error: (err) => { this.isSubmitting.set(false); this.error.set(resolveHttpError(err, 'Could not add the condition node. Please try again.')); }
    });
  }

  wfFocusNode(id: number): void {
    const pos = this.wfNodePositions().get(id);
    if (!pos) return;
    const zoom = this.wfZoom();
    this.wfPan.set({
      x: 400 - (pos.x + this.NODE_W / 2) * zoom,
      y: 300 - (pos.y + this.NODE_H / 2) * zoom
    });
    this.wfSearchOpen.set(false);
  }

  wfGetDependenciesForNode(nodeId: number): ProjectDependencyResponse[] {
    return this.dependencies().filter(d =>
      d.predecessorMilestoneId === nodeId || d.successorMilestoneId === nodeId
    );
  }

  wfDeleteDependency(depId: number): void {
    const projectId = this.projectId();
    if (!projectId || !this.canManage()) return;
    this.srvApi.deleteProjectDependency(projectId, depId).subscribe({
      next: () => { this.success.set('Dependency removed.'); this.loadAll(); },
      error: (err) => { this.error.set(resolveHttpError(err, 'Failed to remove dependency.')); }
    });
    this.closeWfContextMenu();
  }

  wfDeleteSelected(): void {
    const nodeId = this.wfContextMenuNodeId();
    const projectId = this.projectId();
    if (!nodeId || !projectId || !this.canManage()) return;
    if (!confirm('Delete this milestone?')) return;
    this.srvApi.deleteProjectMilestone(projectId, nodeId).subscribe({
      next: () => { this.success.set('Milestone deleted.'); this.loadAll(); },
      error: (err) => { this.error.set(resolveHttpError(err, 'Could not delete the milestone. Please try again.')); }
    });
    this.closeWfContextMenu();
  }

  wfEditFromContext(): void {
    this.closeWfContextMenu();
    this.activeTab.set('milestones');
    const nodeId = this.wfContextMenuNodeId();
    if (nodeId) {
      const m = this.milestones().find(ms => ms.id === nodeId);
      if (m) this.editMilestone(m);
    }
  }

  wfEdgeClick(edgeId: number): void {
    if (!confirm('Remove this dependency?')) return;
    this.wfDeleteDependency(edgeId);
  }

  closeWfContextMenu(): void {
    this.wfContextMenuNodeId.set(null);
  }

  onWfContextMenu(event: MouseEvent, nodeId: number): void {
    event.preventDefault();
    event.stopPropagation();
    this.wfContextMenuNodeId.set(nodeId);
    const rect = (event.target as HTMLElement).closest('.wf-canvas-wrap')?.getBoundingClientRect();
    if (rect) {
      this.wfContextMenuPos.set({ x: event.clientX - rect.left, y: event.clientY - rect.top });
    }
  }

  onWfNodeDblClick(nodeId: number): void {
    this.closeWfContextMenu();
    const m = this.milestones().find(ms => ms.id === nodeId);
    if (!m || !m.id) return;
    const editableStatuses = new Set(['PLANNED', 'IN_PROGRESS', 'BLOCKED']);
    if (!editableStatuses.has(m.status || 'PLANNED')) return;
    this.editMilestone(m);
    this.activeTab.set('milestones');
  }

  onWfEditBlur(nodeId: number): void {
    const newTitle = this.wfEditingTitle().trim();
    if (newTitle) {
      const projectId = this.projectId();
      const m = this.milestones().find(ms => ms.id === nodeId);
      if (projectId && m) {
        this.srvApi.updateProjectMilestone(projectId, nodeId, {
          ...this.toMilestoneRequest(m, m.sortOrder ?? 0),
          title: newTitle
        }).subscribe({
          next: () => this.loadAll(),
          error: () => {}
        });
      }
    }
    this.wfEditingTitle.set('');
  }

  onWfEditKeydown(event: KeyboardEvent, nodeId: number): void {
    if (event.key === 'Enter') {
      (event.target as HTMLElement).blur();
    } else if (event.key === 'Escape') {
      this.wfEditingTitle.set('');
      (event.target as HTMLElement).blur();
    }
  }

  onWfPortMouseDown(event: MouseEvent, nodeId: number, side: 'in' | 'out' | 'bottom'): void {
    event.stopPropagation();
    event.preventDefault();
    this.wfConnectingFrom.set(nodeId);
    this.wfConnectingSide.set(side);
    const svgPoint = this.svgPointFromEvent(event);
    this.wfConnectingMouse.set(svgPoint);
  }

  onWfSvgMouseDown(event: MouseEvent): void {
    this.closeWfContextMenu();
    const target = event.target as HTMLElement;
    const nodeGroup = target.closest('[data-node-id]');
    if (nodeGroup) {
      const nodeId = Number((nodeGroup as HTMLElement).getAttribute('data-node-id'));
      const positions = this.wfNodePositions();
      const pos = positions.get(nodeId);
      if (pos) {
        this.wfDraggingNodeId.set(nodeId);
        this.wfDragOffset.set({
          x: (event.clientX - this.wfPan().x) / this.wfZoom() - pos.x,
          y: (event.clientY - this.wfPan().y) / this.wfZoom() - pos.y
        });
      }
    } else {
      this.wfIsPanning.set(true);
      this.wfPanStart.set({ x: event.clientX - this.wfPan().x, y: event.clientY - this.wfPan().y });
    }
  }

  onWfSvgMouseMove(event: MouseEvent): void {
    if (this.wfConnectingFrom() !== null) {
      this.wfConnectingMouse.set(this.svgPointFromEvent(event));
      return;
    }
    const dragId = this.wfDraggingNodeId();
    if (dragId !== null) {
      const newX = (event.clientX - this.wfPan().x) / this.wfZoom() - this.wfDragOffset().x;
      const newY = (event.clientY - this.wfPan().y) / this.wfZoom() - this.wfDragOffset().y;
      const snappedX = Math.round(newX / 20) * 20;
      const snappedY = Math.round(newY / 20) * 20;
      const current = this.wfNodePositions().get(dragId);
      if (current && current.x !== snappedX || current && current.y !== snappedY) {
        this.wfNodePositions.set(new Map(this.wfNodePositions()).set(dragId, { x: snappedX, y: snappedY }));
      }
      return;
    }
    if (this.wfIsPanning()) {
      this.wfPan.set({
        x: event.clientX - this.wfPanStart().x,
        y: event.clientY - this.wfPanStart().y
      });
    }
  }

  onWfSvgMouseUp(event?: MouseEvent): void {
    if (this.wfConnectingFrom() !== null && event) {
      const target = event.target as HTMLElement;
      const nodeGroup = target.closest('[data-node-id]');
      if (nodeGroup) {
        const targetId = Number((nodeGroup as HTMLElement).getAttribute('data-node-id'));
        const fromId = this.wfConnectingFrom()!;
        if (targetId !== fromId) {
          const side = this.wfConnectingSide();
          const predecessorMilestoneId = (side === 'out' || side === 'bottom') ? fromId : targetId;
          const successorMilestoneId = (side === 'out' || side === 'bottom') ? targetId : fromId;
          const projectId = this.projectId();
          if (projectId && this.canManage()) {
            this.srvApi.createProjectDependency(projectId, { predecessorMilestoneId, successorMilestoneId }).subscribe({
              next: () => { this.success.set('Dependency created.'); this.loadAll(); },
              error: (err) => { this.error.set(resolveHttpError(err, 'Could not create the dependency. Please try again.')); }
            });
          }
        }
      }
    }
    this.wfConnectingFrom.set(null);
    this.wfDraggingNodeId.set(null);
    this.wfIsPanning.set(false);
  }

  onWfSvgClick(event: MouseEvent): void {
  }

  onWfWheel(event: WheelEvent): void {
    event.preventDefault();
    const delta = event.deltaY > 0 ? -0.08 : 0.08;
    this.wfZoom.update(z => Math.max(0.3, Math.min(3, z + delta)));
  }

  wfOnSearchInput(event: Event): void {
    const val = ((event.target as any).value || '') as string;
    this.wfSearchTerm.set(val);
    this.wfSearchOpen.set(true);
  }

  wfOnSearchBlur(): void {
    setTimeout(() => this.wfSearchOpen.set(false), 200);
  }

  wfOnSearchKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      this.wfSearchTerm.set('');
      this.wfSearchOpen.set(false);
    }
  }

  private svgPointFromEvent(event: MouseEvent): { x: number; y: number } {
    const svg = (event.target as HTMLElement).closest('svg');
    if (!svg) return { x: 0, y: 0 };
    const pt = svg.createSVGPoint();
    pt.x = event.clientX;
    pt.y = event.clientY;
    const ctm = svg.getScreenCTM();
    if (!ctm) return { x: 0, y: 0 };
    const svgPt = pt.matrixTransform(ctm.inverse());
    const pan = this.wfPan();
    const zoom = this.wfZoom();
    return { x: (svgPt.x - pan.x) / zoom, y: (svgPt.y - pan.y) / zoom };
  }

  private svgPointFromTouch(touch: Touch): { x: number; y: number } {
    const svg = document.querySelector('.wf-canvas-svg') as SVGSVGElement;
    if (!svg) return { x: 0, y: 0 };
    const pt = svg.createSVGPoint();
    pt.x = touch.clientX;
    pt.y = touch.clientY;
    const ctm = svg.getScreenCTM();
    if (!ctm) return { x: 0, y: 0 };
    const svgPt = pt.matrixTransform(ctm.inverse());
    const pan = this.wfPan();
    const zoom = this.wfZoom();
    return { x: (svgPt.x - pan.x) / zoom, y: (svgPt.y - pan.y) / zoom };
  }

  private findNodeIdAt(target: EventTarget): number | null {
    const el = target as HTMLElement;
    const nodeGroup = el.closest('[data-node-id]');
    if (nodeGroup) {
      return Number((nodeGroup as HTMLElement).getAttribute('data-node-id'));
    }
    return null;
  }

  onWfTouchStart(event: TouchEvent): void {
    event.preventDefault();
    if (event.touches.length === 2) {
      this.clearLongPress();
      this.onWfSvgMouseUp();
      const dx = event.touches[0].clientX - event.touches[1].clientX;
      const dy = event.touches[0].clientY - event.touches[1].clientY;
      this.wfPinchStartDist.set(Math.sqrt(dx * dx + dy * dy));
      this.wfPinchStartZoom.set(this.wfZoom());
      const cx = (event.touches[0].clientX + event.touches[1].clientX) / 2;
      const cy = (event.touches[0].clientY + event.touches[1].clientY) / 2;
      this.wfPanStart.set({ x: cx - this.wfPan().x, y: cy - this.wfPan().y });
      return;
    }
    if (event.touches.length !== 1) return;
    const touch = event.touches[0];
    const nodeId = this.findNodeIdAt(touch.target);

    this.wfTouchStartTime.set(Date.now());
    this.wfTouchStartPos.set({ x: touch.clientX, y: touch.clientY });

    if (nodeId !== null) {
      this.clearLongPress();
      const positions = this.wfNodePositions();
      const pos = positions.get(nodeId);
      if (pos) {
        this.wfDraggingNodeId.set(nodeId);
        this.wfDragOffset.set({
          x: (touch.clientX - this.wfPan().x) / this.wfZoom() - pos.x,
          y: (touch.clientY - this.wfPan().y) / this.wfZoom() - pos.y
        });
      }
      this.wfLongPressTimer.set(
        setTimeout(() => {
          this.onWfContextMenu(touch as any, nodeId);
          this.wfLongPressTimer.set(null);
        }, 600)
      );
    } else {
      this.wfIsPanning.set(true);
      this.wfPanStart.set({ x: touch.clientX - this.wfPan().x, y: touch.clientY - this.wfPan().y });
    }
  }

  onWfTouchMove(event: TouchEvent): void {
    event.preventDefault();
    if (event.touches.length === 2) {
      const dx = event.touches[0].clientX - event.touches[1].clientX;
      const dy = event.touches[0].clientY - event.touches[1].clientY;
      const dist = Math.sqrt(dx * dx + dy * dy);
      const startDist = this.wfPinchStartDist();
      if (startDist > 0) {
        const newZoom = Math.max(0.3, Math.min(3, this.wfPinchStartZoom() * (dist / startDist)));
        this.wfZoom.set(newZoom);
      }
      const cx = (event.touches[0].clientX + event.touches[1].clientX) / 2;
      const cy = (event.touches[0].clientY + event.touches[1].clientY) / 2;
      this.wfPan.set({ x: cx - this.wfPanStart().x, y: cy - this.wfPanStart().y });
      return;
    }
    if (event.touches.length !== 1) return;
    const touch = event.touches[0];
    const moved = Math.abs(touch.clientX - this.wfTouchStartPos().x) + Math.abs(touch.clientY - this.wfTouchStartPos().y);
    if (moved > 10) this.clearLongPress();

    const dragId = this.wfDraggingNodeId();
    if (dragId !== null) {
      const newX = (touch.clientX - this.wfPan().x) / this.wfZoom() - this.wfDragOffset().x;
      const newY = (touch.clientY - this.wfPan().y) / this.wfZoom() - this.wfDragOffset().y;
      const snappedX = Math.round(newX / 20) * 20;
      const snappedY = Math.round(newY / 20) * 20;
      const current = this.wfNodePositions().get(dragId);
      if (current && current.x !== snappedX || current && current.y !== snappedY) {
        this.wfNodePositions.set(new Map(this.wfNodePositions()).set(dragId, { x: snappedX, y: snappedY }));
      }
      return;
    }
    if (this.wfIsPanning()) {
      this.wfPan.set({ x: touch.clientX - this.wfPanStart().x, y: touch.clientY - this.wfPanStart().y });
    }
  }

  onWfTouchEnd(event: TouchEvent): void {
    event.preventDefault();
    this.clearLongPress();
    if (event.touches.length > 0) return;
    const now = Date.now();
    const elapsed = now - this.wfTouchStartTime();
    const moved = Math.abs((event.changedTouches[0]?.clientX ?? 0) - this.wfTouchStartPos().x) +
                  Math.abs((event.changedTouches[0]?.clientY ?? 0) - this.wfTouchStartPos().y);

    if (elapsed < 300 && moved < 10 && this.wfDraggingNodeId() === null && this.wfIsPanning() === false) {
      const nodeId = this.findNodeIdAt(event.changedTouches[0]?.target ?? (event.target as EventTarget));
      if (nodeId !== null && nodeId === this.wfLastTapNodeId() && now - this.wfLastTapTime() < 400) {
        this.onWfNodeDblClick(nodeId);
        this.wfLastTapTime.set(0);
        this.wfLastTapNodeId.set(null);
      } else {
        this.wfLastTapTime.set(now);
        this.wfLastTapNodeId.set(nodeId);
      }
    }

    this.wfConnectingFrom.set(null);
    this.wfDraggingNodeId.set(null);
    this.wfIsPanning.set(false);
    this.wfPinchStartDist.set(0);
  }

  private clearLongPress(): void {
    const timer = this.wfLongPressTimer();
    if (timer !== null) {
      clearTimeout(timer);
      this.wfLongPressTimer.set(null);
    }
  }
}
