# Srv Module — Refactoring & Feature Roadmap

## Product Direction (Core)

- **Project-first orchestration is integral to SRV**, not a side feature.
- Target experience is **Fiverr-inspired flow-of-work** adapted to Esprit Market:
  - Service discovery feeds project assembly
  - Booking workspace executes delivery/revision/chat per service
  - Multiple service bookings are coordinated under one project timeline
- Projects are **user-created coordination spaces** where users can combine multiple services/bookings and let the scheduler/system assist sequencing, dependencies, and completion tracking (instead of manual juggling).
- All phases should be implemented with this project-centric flow in mind.

### Domain Hierarchy Rule (Authoritative)

- **Projects are higher-level orchestration. Services are lower-level execution units.**
- Services must remain **self-contained** and reusable:
  - availability
  - pricing
  - provider capacity
  - booking feasibility
- Services should not contain project-specific planning intelligence.
- Project orchestration consumes service/booking data to produce planning outcomes.
- Booking remains the bridge (`booking.project_id`) between orchestration and execution.

## Execution Status and Scope Boundaries

Purpose of this section: prevent phase overlap and keep implementation sequencing explicit.

### Completed (Shipped Baseline)

- **Phase 1**: Foundation domain refactor and core booking/service/project model.
- **Phase 2**: Availability engine baseline (templates, constraints, overlap checks, reschedule flow).
- **Phase 3**: Deliverable/review workflow baseline in workspace.
- **Phase 6 (partial)**: Workspace chat realtime baseline (STOMP + polling fallback), cursor-batch message history loading.
- **Phase 5 (baseline only)**:
  - Project orchestration CRUD primitives (milestones/dependencies/timeline)
  - Booking-to-project contextual linkage
  - Frontend orchestration board baseline

### Not Started / Deferred (Do Not Treat as Done)

- **Phase 4** (next priority): scoring and scheduling intelligence
  - project-first weighted ranking
  - competitive slot allocation and tie-breaking
  - assisted alternative-slot suggestions
  - automation policies (expiry/escalation)
- **Phase 5 advanced orchestration**:
  - dependency auto-suggestions
  - cross-service conflict/risk prediction
  - recommendation engine for sequencing optimization
  - richer planning views (calendar/Gantt)
  - portfolio-level multi-project balancing

### Rule for upcoming work

- While implementing Phase 4, Phase 5 changes must be limited to compatibility/support only.
- No Phase 5 advanced assistant logic should be introduced unless explicitly planned as post-Phase-4 work.

## Phase 1 — Foundation (Completed)

### 1.1 Entity Model Fixes

| Change | Details |
|--------|---------|
| Rename `ServiceRequest` → `Booking` | Entity, repository, service, controller, mapper, DTOs, tests |
| Create `BookingStatus` enum | `PENDING`, `APPROVED`, `REJECTED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED` |
| Create `ServiceCategory` enum | Replace freeform `category` string on Service |
| Create `PricingType` enum | `HOURLY`, `FIXED` — add field to Service |
| Rename `hourlyRate` → `price` | Change type from `Double` to `BigDecimal` |
| Add `imageUrl` to Service | Single image URL (gallery deferred to later phase) |
| Tie `ServiceReview` to `Booking` | Add `booking` relationship, remove direct `service` link |
| Add `notes` to Booking | Freeform text for special requests |
| Add soft delete | `deletedAt` timestamp on Service, Booking, Partner, Project |
| Fix `Double` → `BigDecimal` | All monetary/rating fields across entities and DTOs |

### 1.2 New Entities & Relationships

| Entity | Details |
|--------|---------|
| `ServiceTag` (join) | `service_id`, `tag` — many-to-many for flexible search |
| `UserFavorite` (join) | `user_id`, `service_id`, `createdAt` — wishlist/favorites |
| `BookingAuditLog` | `booking_id`, `from_status`, `to_status`, `changed_by`, `changed_at` — state transition log |

### 1.3 Common Module Additions

| Addition | Details |
|----------|---------|
| `PageResponse<T>` | Generic pagination wrapper: `content`, `page`, `size`, `totalElements`, `totalPages` |
| `StatusTransitionEvent` | Base Spring event class for state changes |
| `NotificationEvent` | Simple notification abstraction (email/push/websocket placeholder) |

### 1.4 Repository & Service Changes

| Change | Details |
|--------|---------|
| Pagination everywhere | All `getAll()` → `getAll(Pageable)` returning `Page<T>` |
| Service search/filter | `findByCategory`, `findByLocationContaining`, `findByPriceBetween`, `findByRatingGreaterThanEqual`, tag-based search |
| Provider public profile | Endpoint exposing provider name, rating, services (no auth) |
| Browse services public | GET endpoints for service list/detail — no auth required |

### 1.5 Booking Flow (Backend)

```
PENDING ──(provider accepts)──→ APPROVED ──(provider starts)──→ IN_PROGRESS ──→ COMPLETED
   │                              │
   │(provider rejects)            │(user cancels)
   ↓                              ↓
REJECTED                       CANCELLED
```

Endpoints:
- `GET /api/srv/services` — public browse (paginated, filterable)
- `GET /api/srv/services/{id}` — public detail
- `GET /api/srv/services/provider/{providerId}` — public provider services
- `POST /api/srv/bookings` — user creates booking (auth required)
- `GET /api/srv/bookings/user/{userId}` — user sees their bookings
- `GET /api/srv/bookings/provider/{providerId}` — provider sees incoming
- `PATCH /api/srv/bookings/{id}/status` — provider accepts/rejects
- `PATCH /api/srv/bookings/{id}/cancel` — user cancels
- Booking state machine enforces valid transitions
- Every transition → audit log entry + Spring event fired

### 1.6 Frontend — Service Marketplace (Frontoffice)

| Page | Details |
|------|---------|
| Service Browse | Grid/list, category filter, search, price range, pagination |
| Service Detail | Full info, provider profile, reviews, book button |
| Booking Form | Date picker, duration, notes field, price estimate |
| My Bookings | List with status badges, cancel button, link to review |
| Provider Dashboard | Incoming requests, accept/reject, active bookings |
| Favorites | Saved services list, add/remove from detail page |
| Leave Review | Post-booking, tied to completed booking |

### 1.7 Frontend — Backoffice Enhancements

| Enhancement | Details |
|-------------|---------|
| Service CRUD update | Adapt to new fields (pricingType, price, category enum, imageUrl) |
| Tag management | Add/remove tags when creating/editing a service |
| Image upload | Wire up imageUrl field (storage integration) |

---

## Phase 2 — Availability Engine

- ProviderWeeklyTemplate entity (per-service, per-day, time windows, max concurrent)
- ProviderException entity (BLOCKED, CUSTOM_HOURS)
- ServiceMandate entity (per provider+service max)
- ProviderMandate entity (global max across services)
- Time slot generation from template
- Overlap detection against existing bookings
- Mandate threshold enforcement (most restrictive wins)
- Overbooked flags (service-level, provider-level)
- Booking state expansion: `PENDING` → `PENDING_EVALUATION` → `TENTATIVE` → `CONFIRMED`
- Booking validation checks templates + overlap directly (not via slot regeneration)
- Frontend `AvailabilitySlotPickerComponent` with auto-refresh after booking
- `Booking.duration` is `double` (fractional hours supported, e.g. 0.5, 1.5)
- Duration constrained to multiples of `slotDurationMinutes` (provider decides granularity)
- `TimeSlotDto.slotDurationMinutes` exposed to frontend for duration step enforcement
- Frontend duration input is a `<select>` constrained to valid multiples

### 2.x — Reschedule & Time Management

| Entity / Enum | Details |
|---------------|---------|
| `RescheduleRequest` | `booking_id`, `requested_by`, `original_date/duration`, `proposed_date/duration`, `reason`, `message`, `status`, `responded_by`, `responded_at`, `response_message` |
| `RescheduleStatus` | `PENDING`, `ACCEPTED`, `REJECTED`, `CANCELLED` |
| `RescheduleReason` | `SCHEDULING_CONFLICT`, `DELAY`, `CLIENT_REQUEST`, `OTHER` |

Flow:
- Either provider or client can propose a reschedule (one active PENDING per booking)
- Proposed slot validated against availability engine before request is created
- Other party accepts (booking date/duration updated, audit log) or rejects
- Requester can cancel their own pending request
- Notification events on create/accept/reject

Endpoints:
- `POST /api/srv/bookings/{bookingId}/reschedule` — propose new time
- `GET /api/srv/bookings/{bookingId}/reschedule` — get active request
- `GET /api/srv/bookings/{bookingId}/reschedule/history` — full history
- `PATCH /api/srv/bookings/reschedule/{id}/accept` — accept
- `PATCH /api/srv/bookings/reschedule/{id}/reject` — reject
- `PATCH /api/srv/bookings/reschedule/{id}/cancel` — cancel

Notes for future phases:
- `RescheduleReason.DELAY` is designed for Phase 3 deliverable delay flow — when a provider needs more time, they create a reschedule with reason=DELAY
- Reschedule does NOT change booking status — the booking keeps its current status regardless of the reschedule request
- This is distinct from Phase 4's `PENDING_RESCHEDULE` status (which was planned as a booking-level status). The current design is more flexible since it allows rescheduling at any booking state without losing status context

---

## Phase 3 — Deliverable & Review Workflow

### 3.1 Entities

| Entity | Details |
|--------|---------|
| `Deliverable` | `id`, `booking_id` (FK), `provider_id`, `title`, `description`, `status` (`DRAFT`, `SUBMITTED`, `ACCEPTED`, `REVISION_REQUESTED`, `REJECTED`), `version` (int), `submittedAt`, `reviewedAt`, `createdAt`, `deletedAt` |
| `DeliverableAttachment` | `id`, `deliverable_id` (FK), `fileUrl`, `fileName`, `fileSize`, `fileType`, `uploadedAt` |
| `DeliverableReview` | `id`, `deliverable_id` (FK), `reviewer_id` (user), `decision` (`ACCEPTED`, `REVISION_REQUESTED`, `REJECTED`), `comment`, `reviewedAt` |

### 3.2 Booking Status Expansion

```
IN_PROGRESS ──(provider submits deliverable)──→ PENDING_REVIEW
PENDING_REVIEW ──(client accepts)──→ COMPLETED
PENDING_REVIEW ──(client requests revision)──→ IN_PROGRESS
PENDING_REVIEW ──(client rejects)──→ DISPUTED
```

- New booking statuses: `PENDING_REVIEW`, `DISPUTED`
- `BookingAuditLog` entries for all new transitions
- `StatusTransitionEvent` + `NotificationEvent` fired as in Phase 1

### 3.3 Backend Endpoints

- `POST /api/srv/bookings/{bookingId}/deliverables` — provider creates/submits deliverable (multipart: metadata + files)
- `GET /api/srv/bookings/{bookingId}/deliverables` — list deliverables for a booking
- `GET /api/srv/deliverables/{id}` — deliverable detail with attachments
- `PATCH /api/srv/deliverables/{id}/submit` — submit draft deliverable (sets status to SUBMITTED, booking to PENDING_REVIEW)
- `POST /api/srv/deliverables/{id}/review` — client reviews (accept / request revision / reject)
- `POST /api/srv/deliverables/{id}/attachments` — add more files to an existing deliverable
- `DELETE /api/srv/deliverables/{id}/attachments/{attachmentId}` — remove attachment (provider only, draft status only)
- `GET /api/srv/deliverables/{id}/history` — version history with review comments

### 3.4 Business Rules

- Only the booking provider can create/edit deliverables
- Only the booking client can review deliverables
- Provider can edit/delete attachments only while deliverable is in DRAFT
- On revision request, booking reverts to IN_PROGRESS, deliverable status → REVISION_REQUESTED, provider can update and re-submit (increments version)
- On acceptance, booking moves to COMPLETED, client is prompted to leave a review (existing ServiceReview flow)
- On rejection, booking moves to DISPUTED — no auto-resolution (admin/mediation deferred)

### 3.5 Storage Integration

- File uploads stored via configurable storage backend (local filesystem for dev, S3-compatible for prod)
- Max file size per attachment: 25 MB
- Allowed file types: images, PDFs, documents, archives
- Virus scan hook placeholder for production

### 3.6 Frontend

| Page / Component | Details |
|------------------|---------|
| Booking detail — provider view | "Submit Deliverable" button, upload files, view submission history |
| Booking detail — client view | Review submitted deliverable, accept / request revision / reject, add comment |
| Deliverable detail page | File previews, version timeline, review comments thread |
| My Bookings enhancement | New status badges for PENDING_REVIEW, DISPUTED; "Review deliverable" CTA for client |
| Provider Dashboard enhancement | "Submit work" action on IN_PROGRESS bookings |

### 3.7 Implementation Notes (Completed)

- **DISPUTED status** has no auto-resolution — admin/mediation panel deferred to a future phase
- **RescheduleReason.DELAY** integration: providers use the existing reschedule mechanism with `reason=DELAY` when deliverable submission is delayed, independent of booking status
- **Phase 4's PENDING_RESCHEDULE status** is effectively replaced by the reschedule mechanism with `reason=DELAY` — no dedicated booking status needed for rescheduling
- **Deliverable version tracking**: each revision increments the `version` field on the `Deliverable` entity, creating a clear audit trail
- **FileStorageService** uses configurable `srv.deliverable.upload-dir` property (defaults to `uploads/deliverables`)
- **Frontend routing**: `/deliverables/:id` (detail view) and `/deliverables/:id/review` (review form) — both auth-guarded
- **Provider Dashboard** updated with Submit Work (IN_PROGRESS), View Deliverable (PENDING_REVIEW, DISPUTED) action buttons in both Bookings and Scheduling tabs

---

## Phase 4 — Intelligence & Scoring

- **Objective:** make booking allocation and scheduling decisions project-aware, explainable, and policy-driven.

### 4.1 Decision Modes and Policies

- Priority modes: `PROJECT_FIRST`, `COMPETITIVE`
- Provider/service policy profile:
  - fairness weight
  - urgency weight
  - completion-pressure weight
  - premium/customer-tier weight
  - manual override threshold

### 4.2 Scoring Engine

- Weighted scoring function inputs:
  - scarcity (slot density / provider load)
  - project size and criticality
  - project age and deadline proximity
  - completion percentage and blocker count
  - requester/provider reliability signals
- Output:
  - normalized score
  - score breakdown (for explainability in UI/logs)

### 4.3 Competition and Allocation

- Slot competition resolution when demand exceeds supply
- Deterministic tie-breakers (createdAt, project urgency bucket, policy override)
- Allocation audit event written for each decision

### 4.4 Assisted Rescheduling

- Suggest ranked alternative slots (with explanation)
- Human still confirms final reschedule decision
- Preserve booking status semantics from earlier phases

### 4.5 Automation and Expiry

- Scheduled jobs (`pg_cron` or Spring scheduler fallback) for:
  - tentative hold expiry
  - stale pending-evaluation cleanup
  - optional reminder/escalation hooks

### 4.6 API and UI Deliverables (Phase 4)

- Backend:
  - score computation endpoint(s)
  - ranked slot suggestion endpoint(s)
  - competition outcome endpoint(s)
  - allocation audit retrieval endpoint(s)
- Frontend:
  - scoring insight panel in booking/project context
  - suggested slot list with reason codes
  - competition outcome transparency in provider/client views

Design boundary for Phase 4 endpoints:
- Service endpoints may expose raw capability and availability scoring only.
- Project-aware recommendation/scoring must live under project orchestration routes (project-owned APIs).
- Existing mixed endpoints are temporary baseline scaffolding and should be migrated to project-owned contracts.

### 4.7 Out of Scope for Phase 4

- Full Phase 5 assistant orchestration automation
- Portfolio-level optimization across multiple projects
- Complex graph layout planning tools

---

## Phase 5 — Project Coordination (Core Expansion, not optional)

**Status framing:** baseline delivered; advanced orchestration remains pending and should follow Phase 4.

- Multi-service project timeline
- Project booking orchestration
- Cross-service dependency management
- Project completion tracking
- User-managed project orchestration board:
  - Milestone CRUD with order management
  - Dependency links between milestones
  - Timeline/progress metrics surfaced to user
  - Booking-to-project linkage shown in workspace for contextual execution

Notes:
- This phase formalizes capabilities that are already product-core in earlier phases.
- Booking workspace, deliverables, and communication must remain project-aware for future orchestration.

Implementation snapshot (2026-04):
- Backend orchestration APIs are available (`/projects/{id}/milestones`, `/dependencies`, `/timeline`).
- Booking-to-project link is implemented in API + DB and exposed to workspace.
- Frontend orchestration page exists at `/projects/:id` with:
  - Milestone CRUD
  - Dependency create/delete
  - Timeline progress summary
  - Milestone ordering controls (drag/drop + arrows)
  - Dependency graph section

### 5.1 Baseline (Delivered)

- Project-level timeline entity model
- Milestone/dependency CRUD APIs
- Booking linkage to project context
- Basic orchestration UI and ordering controls

Service boundary enforcement notes:
- Project intelligence panels belong to project screens, not service catalog screens.
- Service pages can surface execution-level data only (availability, price, provider fit), without project-specific planning controls.

### 5.2 Advanced (Planned, Post-Phase-4)

- Auto-generated milestone templates from selected service bundles
- Dependency suggestion assistant (rule + scoring assisted)
- Risk detection (critical path slippage, blocked chain alerts)
- Multi-booking schedule optimization recommendations

### 5.3 Expert (Later)

- Portfolio-level view (many projects per user/team)
- What-if simulation for timeline shifts
- Constraint solver hooks for automated sequencing proposals

---

## Phase 6 — Real-time & Infrastructure

- pg_notify on booking state transitions
- PGNotificationListener in Spring Boot
- WebSocket STOMP broker integration
- Real-time status updates to requester, provider, project owner
- Notification delivery (email/push integration hooks)
- GIST index for slot overlap constraints

### 6.x — Booking Workspace Communication (Fiverr-inspired workflow)

- Product intent: model the end-to-end booking execution flow after Fiverr-style buyer/seller collaboration
  (shared workspace, structured delivery, revision loop, and contextual chat in one place)
- Add a per-booking workspace chat that supports this Fiverr-inspired flow-of-work context
- Keep first release simple: REST + frontend polling (no websocket yet)
- Endpoints:
  - `GET /api/srv/bookings/{id}/messages`
  - `POST /api/srv/bookings/{id}/messages`
  - `GET /api/srv/bookings/{id}/messages/batch?beforeId=&limit=` (cursor batch loading)
- Authorization: only booking participants (client/provider) can read/send
- Include sender name + timestamp in response for timeline rendering
- Frontend supports batch-based chat history loading and older-message fetching.
- STOMP push is integrated with polling fallback for resiliency.
- pg_notify-based event fanout remains a future enhancement.

---

## Naming Alignment (Current → Future)

| Current (Phase 1) | Future (Phase 2+) |
|--------------------|--------------------|
| `Booking.status = PENDING` | `PENDING_EVALUATION` |
| `Booking.status = APPROVED` | `CONFIRMED` |
| `Booking.status = IN_PROGRESS` | `IN_PROGRESS` → `PENDING_REVIEW` → `COMPLETED` (via Phase 3) |
| `Booking.notes` | Kept |
| `Booking.date` (Date) | `slot_range` (TSRANGE) |
| `Booking.duration` (double hours) | Derived from `slot_range` |
| Simple status transitions | Score-based slot allocation |
| No reschedule mechanism | `RescheduleRequest` entity (Phase 2.x) — no `PENDING_RESCHEDULE` status needed |
| No deliverable tracking | `Deliverable` entity with versioned submissions (Phase 3) |
