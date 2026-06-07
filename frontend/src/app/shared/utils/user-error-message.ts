const RULES: Array<{ test: (msg: string) => boolean; friendly: string }> = [
  { test: m => m.includes('outside the provider'), friendly: 'This time is outside the provider\'s working hours. Please pick a different slot.' },
  { test: m => m.includes('not available on the selected date'), friendly: 'The provider isn\'t available on this date. Try another day.' },
  { test: m => m.includes('already booked'), friendly: 'This slot was just taken. Please select another one.' },
  { test: m => m.includes('multiple of'), friendly: 'The duration doesn\'t match the available slot intervals.' },
  { test: m => m.includes('duration must be positive'), friendly: 'Please select a valid duration.' },
  { test: m => m.includes('has no assigned provider'), friendly: 'This service has no provider assigned yet.' },

  { test: m => m.includes('Cannot transition booking'), friendly: 'This action isn\'t allowed for the current booking status.' },
  { test: m => m.includes('Cannot reschedule'), friendly: 'This booking can\'t be rescheduled in its current state.' },
  { test: m => m.includes('pending reschedule request already exists'), friendly: 'A reschedule request is already pending for this booking.' },
  { test: m => m.includes('Only the booking provider or client'), friendly: 'Only the provider or client can request a reschedule.' },
  { test: m => m.includes('Only pending reschedule requests can be accepted'), friendly: 'This reschedule request is no longer pending.' },
  { test: m => m.includes('other party in this booking'), friendly: 'Only the other party can respond to this request.' },
  { test: m => m.includes('Only pending reschedule requests can be rejected'), friendly: 'This request can no longer be rejected.' },
  { test: m => m.includes('Only pending reschedule requests can be cancelled'), friendly: 'This request can no longer be cancelled.' },
  { test: m => m.includes('Only the requester can cancel'), friendly: 'Only the person who made the request can cancel it.' },

  { test: m => m.includes('Can only review completed bookings'), friendly: 'You can only review bookings that are completed.' },
  { test: m => m.includes('Only the booking owner can leave a review'), friendly: 'Only the person who made the booking can leave a review.' },

  { test: m => m.includes('not authorized to access this file'), friendly: 'You don\'t have permission to access this file.' },
  { test: m => m.includes('not authorized to access this booking chat'), friendly: 'You don\'t have access to this conversation.' },
  { test: m => m.includes('not authorized to upload files'), friendly: 'You don\'t have permission to upload files here.' },
  { test: m => m.includes('not authorized to view files'), friendly: 'You don\'t have permission to view these files.' },
  { test: m => m.includes('not authorized to clean files'), friendly: 'You don\'t have permission to manage these files.' },
  { test: m => m.includes('not authorized to view deliverable'), friendly: 'You don\'t have permission to view these deliverables.' },

  { test: m => m.includes('Deliverables can only be created for bookings'), friendly: 'Deliverables can only be created for active bookings.' },
  { test: m => m.includes('Only the booking provider can create deliverables'), friendly: 'Only the assigned provider can create deliverables.' },
  { test: m => m.includes('Only DRAFT or REVISION_REQUESTED deliverables can be submitted'), friendly: 'This deliverable can\'t be submitted in its current state.' },
  { test: m => m.includes('Only the provider can submit'), friendly: 'Only the provider can submit this deliverable.' },
  { test: m => m.includes('Only SUBMITTED deliverables can be reviewed'), friendly: 'This deliverable hasn\'t been submitted yet.' },
  { test: m => m.includes('Only the booking client can review deliverables'), friendly: 'Only the client can review deliverables.' },

  { test: m => m.includes('Attachments can only be added to'), friendly: 'Files can\'t be added to this deliverable anymore.' },
  { test: m => m.includes('Attachments can only be deleted from'), friendly: 'Files can\'t be removed from this deliverable.' },
  { test: m => m.includes('Only the provider can add attachments'), friendly: 'Only the provider can add files here.' },
  { test: m => m.includes('Only the provider can delete attachments'), friendly: 'Only the provider can remove files here.' },
  { test: m => m.includes('Attachment does not belong'), friendly: 'This file doesn\'t belong here.' },
  { test: m => m.includes('You can only delete your own files'), friendly: 'You can only delete files you uploaded.' },

  { test: m => m.includes('Milestone does not belong to this project'), friendly: 'This milestone doesn\'t belong to this project.' },
  { test: m => m.includes('Booking does not belong to this project'), friendly: 'This booking doesn\'t belong to this project.' },
  { test: m => m.includes('cannot depend on itself'), friendly: 'A milestone can\'t depend on itself.' },
  { test: m => m.includes('Dependency does not belong'), friendly: 'This dependency doesn\'t belong to this project.' },
  { test: m => m.includes('Planned start date must be before'), friendly: 'The start date must be before the end date.' },
  { test: m => m.includes('Actual start date must be before'), friendly: 'The actual start date must be before the end date.' },
  { test: m => m.includes('does not allow project participation'), friendly: 'This service isn\'t available for project bookings.' },

  { test: m => m.includes('Cannot remove the project creator'), friendly: 'The project creator can\'t be removed.' },
  { test: m => m.includes('Service already in favorites'), friendly: 'This service is already in your favorites.' },

  { test: m => m.includes('File is empty'), friendly: 'The selected file is empty.' },
  { test: m => m.includes('File size must not exceed'), friendly: 'This file is too large.' },
  { test: m => m.includes('Invalid filename'), friendly: 'The filename contains invalid characters.' },
  { test: m => m.includes('Unsupported file type'), friendly: 'This file type isn\'t supported.' },
  { test: m => m.includes('Unsupported image extension'), friendly: 'This image format isn\'t supported.' },
  { test: m => m.includes('Only image files are allowed'), friendly: 'Please select an image file.' },

  { test: m => m.includes('CUSTOM_HOURS exceptions require'), friendly: 'Both start and end hours are required.' },
];

function improveMessage(msg: string): string {
  if (!msg) return 'Something went wrong. Please try again.';
  const lower = msg.toLowerCase();
  for (const rule of RULES) {
    if (rule.test(lower)) return rule.friendly;
  }
  return msg;
}

function resolveHttpError(err: any, fallback: string): string {
  if (!err) return fallback;
  const serverMsg = err?.error?.message || err?.message || '';
  return improveMessage(serverMsg) || fallback;
}

export { resolveHttpError, improveMessage };
