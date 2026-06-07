export function formatStatusLabel(status: string | undefined): string {
  return (status ?? '').replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
}

export function bookingStatusBadgeClass(status: string | undefined): string {
  switch (status ?? '') {
    case 'PENDING':
    case 'PENDING_EVALUATION':
    case 'TENTATIVE':
      return 'badge badge-warning';
    case 'APPROVED':
    case 'CONFIRMED':
      return 'badge badge-success';
    case 'IN_PROGRESS':
      return 'badge badge-info';
    case 'COMPLETED':
      return 'badge badge-success';
    case 'PENDING_REVIEW':
      return 'badge badge-info';
    case 'CANCELLED':
      return 'badge badge-neutral';
    case 'REJECTED':
      return 'badge badge-danger';
    case 'DISPUTED':
      return 'badge badge-warning';
    default:
      return 'badge';
  }
}

export function bookingStatusPillClass(status: string | undefined): string {
  switch (status ?? '') {
    case 'PENDING':
    case 'PENDING_EVALUATION':
    case 'TENTATIVE':
      return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400';
    case 'APPROVED':
    case 'CONFIRMED':
      return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400';
    case 'IN_PROGRESS':
      return 'bg-purple-100 text-purple-800 dark:bg-purple-900/30 dark:text-purple-400';
    case 'PENDING_REVIEW':
      return 'bg-indigo-100 text-indigo-800 dark:bg-indigo-900/30 dark:text-indigo-400';
    case 'COMPLETED':
      return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400';
    case 'CANCELLED':
      return 'bg-neutral-100 text-neutral-600 dark:bg-neutral-700 dark:text-neutral-400';
    case 'REJECTED':
      return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400';
    case 'DISPUTED':
      return 'bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-400';
    default:
      return 'bg-neutral-100 text-neutral-600 dark:bg-neutral-700 dark:text-neutral-400';
  }
}

export function deliverableStatusClass(status: string | undefined): string {
  switch (status ?? '') {
    case 'DRAFT':
      return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400';
    case 'SUBMITTED':
      return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400';
    case 'ACCEPTED':
      return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400';
    case 'REVISION_REQUESTED':
      return 'bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-400';
    case 'REJECTED':
      return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400';
    default:
      return 'bg-neutral-100 text-neutral-600 dark:bg-neutral-700 dark:text-neutral-400';
  }
}

export function milestoneStatusClass(status: string | undefined): string {
  switch (status) {
    case 'COMPLETED':
      return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400';
    case 'IN_PROGRESS':
      return 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400';
    case 'BLOCKED':
      return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400';
    case 'CANCELLED':
      return 'bg-neutral-100 text-neutral-600 dark:bg-neutral-700 dark:text-neutral-400';
    default:
      return 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400';
  }
}

export function projectStatusBadgeClass(status: string | undefined): string {
  switch (status) {
    case 'IN_PROGRESS':
      return 'badge badge-info';
    case 'COMPLETED':
      return 'badge badge-success';
    case 'CANCELLED':
      return 'badge badge-neutral';
    default:
      return 'badge badge-warning';
  }
}

export function serviceStatusBadgeClass(status: string | undefined): string {
  if (status === 'AVAILABLE') return 'badge badge-success';
  if (status === 'UNAVAILABLE') return 'badge badge-danger';
  if (status === 'PAUSED') return 'badge badge-warning';
  return 'badge';
}

export function severityClass(severity?: string): string {
  switch (severity) {
    case 'CRITICAL':
      return 'bg-red-200 text-red-900 dark:bg-red-900/40 dark:text-red-300';
    case 'HIGH':
      return 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400';
    case 'MEDIUM':
      return 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400';
    case 'LOW':
      return 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400';
    default:
      return 'bg-neutral-100 text-neutral-700 dark:bg-neutral-700 dark:text-neutral-300';
  }
}

export function formatFileSize(bytes: number | undefined): string {
  if (!bytes || bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const i = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  const val = bytes / Math.pow(1024, i);
  return `${val % 1 === 0 ? val : val.toFixed(1)} ${units[i]}`;
}
