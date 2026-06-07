import { Routes } from '@angular/router';

import { EventReservationComponent, TicketsComponent } from './features/frontoffice';
import { EventsSchedulerDashboardComponent } from './features/backoffice';
import { MyApplicationsComponent } from './features/frontoffice/my-applications/my-applications.component';
import { InterviewRoomComponent } from './features/video-call/interview-room/interview-room.component';

import {
  RecruiterOverviewComponent,
  RecruiterCompaniesComponent,
  RecruiterJoboffersComponent,
  RecruiterApplicationsComponent,
  RecruiterInterviewsComponent
} from './features/backoffice/recruiter-dashboard';

import { authGuard, roleGuard } from './guards/auth.guard';
import { CreateStoreComponent } from './features/frontoffice/create-store/create-store.component';
import { CreateStoreStep2Component } from './features/frontoffice/create-store/create-store-step-2.component';
import { StoreShowcaseComponent } from './features/frontoffice/create-store/store-showcase.component';
import { StorePublicComponent } from './features/frontoffice/create-store/store-public.component';
import { ProductDetailComponent } from './features/frontoffice/product-detail/product-detail.component';
import { CheckoutComponent } from './features/frontoffice/checkout/checkout.component';
import { VisualSearchComponent } from './features/frontoffice/visual-search/visual-search.component';
import { IntelligentSearchComponent } from './features/frontoffice/intelligent-search/intelligent-search.component';
import { ProductSearchComponent } from './features/frontoffice/product-search/product-search.component';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth').then((m) => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth').then((m) => m.RegisterComponent)
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./features/auth').then((m) => m.ForgotPasswordComponent)
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./features/auth').then((m) => m.ResetPasswordComponent)
  },

  {
    path: 'home',
    loadComponent: () => import('./features/frontoffice').then((m) => m.HomeComponent)
  },
  {
    path: 'marketplace',
    loadComponent: () => import('./features/frontoffice').then((m) => m.MarketplaceComponent)
  },
  {
    path: 'search/products',
    component: ProductSearchComponent
  },
  {
    path: 'projects',
    loadComponent: () => import('./features/frontoffice').then((m) => m.ProjectsServicesComponent)
  },
  {
    path: 'services',
    loadComponent: () => import('./features/frontoffice').then((m) => m.ServicesCatalogComponent)
  },
  {
    path: 'projects/:id',
    loadComponent: () => import('./features/frontoffice').then((m) => m.ProjectOrchestrationComponent),
    canActivate: [authGuard]
  },
  {
    path: 'services/:id',
    loadComponent: () => import('./features/frontoffice').then((m) => m.ServiceDetailComponent)
  },
  {
    path: 'bookings/:id',
    loadComponent: () => import('./features/frontoffice').then((m) => m.BookingWorkspaceComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['visitor', 'service_provider', 'admin_market'] }
  },
  {
    path: 'bookings',
    loadComponent: () => import('./features/frontoffice').then((m) => m.MyBookingsComponent),
    canActivate: [authGuard]
  },
  {
    path: 'favorites',
    loadComponent: () => import('./features/frontoffice').then((m) => m.UserFavoritesComponent),
    canActivate: [authGuard]
  },
  {
    path: 'reviews/new',
    loadComponent: () => import('./features/frontoffice').then((m) => m.ServiceReviewFormComponent),
    canActivate: [authGuard]
  },
  {
    path: 'tracking',
    loadComponent: () => import('./features/frontoffice').then((m) => m.OrderTrackingComponent)
  },
  {
    path: 'packages',
    loadComponent: () => import('./features/frontoffice').then((m) => m.MyPackagesComponent)
  },
  {
    path: 'my-deliveries',
    loadComponent: () => import('./features/frontoffice').then((m) => m.MyDeliveriesComponent),
    canActivate: [authGuard]
  },
  {
    path: 'offers',
    loadComponent: () => import('./features/frontoffice').then((m) => m.JobOffersComponent)
  },
  {
    path: 'events',
    loadComponent: () => import('./features/frontoffice').then((m) => m.EventsComponent)
  },
  {
    path: 'my-applications',
    component: MyApplicationsComponent,
    canActivate: [authGuard]
  },
  {
    path: 'confirmation-de-delivery',
    loadComponent: () => import('./features/frontoffice').then((m) => m.ConfirmationDeDeliveryComponent)
  },
  {
    path: 'verification-paiement',
    loadComponent: () => import('./features/frontoffice').then((m) => m.VerificationPaiementComponent)
  },
  {
    path: 'create-store',
    component: CreateStoreComponent,
    canActivate: [authGuard]
  },
  {
    path: 'create-store-step-2',
    component: CreateStoreStep2Component,
    canActivate: [authGuard]
  },
  {
    path: 'store-showcase',
    component: StoreShowcaseComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['seller'] }
  },
  {
    path: 'store/:storeName',
    component: StorePublicComponent
  },
  {
    path: 'product/:id',
    component: ProductDetailComponent
  },
  {
    path: 'checkout',
    component: CheckoutComponent
  },
  {
    path: 'visual-search',
    component: VisualSearchComponent
  },
  {
    path: 'intelligent-search',
    component: IntelligentSearchComponent
  },
  {
    path: 'tickets',
    component: TicketsComponent
  },
  {
    path: 'event-reservation/:id',
    component: EventReservationComponent
  },

  {
    path: 'dashboard',
    loadComponent: () => import('./features/backoffice').then((m) => m.AdminDashboardComponent),
    canActivate: [authGuard],
    data: { roles: ['seller', 'service_provider', 'recruiter', 'deliverer', 'event'] }
  },
  {
    path: 'dashboard/vendor',
    loadComponent: () => import('./features/backoffice').then((m) => m.VendorDashboardComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['seller', 'visitor'] }
  },
  {
    path: 'dashboard/service-provider',
    loadComponent: () => import('./features/backoffice').then((m) => m.ServiceProviderDashboardComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['service_provider'] }
  },
  {
    path: 'dashboard/deliverer',
    loadComponent: () => import('./features/backoffice').then((m) => m.DeliveryDashboardComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['deliverer'] }
  },
  {
    path: 'quiz/:userId',
    loadComponent: () => import('./features/backoffice/deliverer-dashboard/deliverer-quiz.component').then((m) => m.DelivererQuizComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['deliverer'] }
  },
  {
    path: 'dashboard/admin-delivery',
    loadComponent: () => import('./features/backoffice').then((m) => m.AdminDeliveryDashboardComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['admin_delivery'] }
  },
  {
    path: 'dashboard/recruiter',
    loadComponent: () => import('./features/backoffice').then((m) => m.RecruiterDashboardComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['recruiter'] },
    children: [
      {
        path: 'overview',
        component: RecruiterOverviewComponent
      },
      {
        path: 'companies',
        component: RecruiterCompaniesComponent
      },
      {
        path: 'joboffers',
        component: RecruiterJoboffersComponent
      },
      {
        path: 'applications',
        component: RecruiterApplicationsComponent
      },
      {
        path: 'interviews',
        component: RecruiterInterviewsComponent
      },
      {
        path: '',
        redirectTo: 'overview',
        pathMatch: 'full'
      }
    ]
  },

  {
    path: 'interview-room/:id',
    component: InterviewRoomComponent,
    canActivate: [authGuard]
  },
  {
    path: 'dashboard/events',
    loadComponent: () => import('./features/backoffice').then((m) => m.EventsAdminDashboardComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['event'] }
  },
  {
    path: 'dashboard/events-scheduler',
    component: EventsSchedulerDashboardComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['event'] }
  },

  {
    path: 'deliverables/new',
    loadComponent: () => import('./features/frontoffice').then((m) => m.DeliverableSubmitComponent),
    canActivate: [authGuard, roleGuard],
    data: { roles: ['service_provider'] }
  },
  {
    path: 'deliverables/:id',
    loadComponent: () => import('./features/frontoffice').then((m) => m.DeliverableDetailComponent),
    canActivate: [authGuard]
  },
  {
    path: 'deliverables/:id/review',
    loadComponent: () => import('./features/frontoffice').then((m) => m.DeliverableReviewComponent),
    canActivate: [authGuard]
  },
  {
    path: '',
    redirectTo: '/home',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: '/home'
  }
];
