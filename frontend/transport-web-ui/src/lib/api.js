import { apiClient, unwrap } from './apiClient.js'

const data = (promise) => promise.then(unwrap)

export const authApi = {
  loginPassword: (payload) => data(apiClient.post('/api/auth/login/password', payload)),
  sendLoginOtp: (mobileNumber) =>
    data(apiClient.post('/api/auth/login/send-otp', { mobileNumber })),
  verifyLoginOtp: (payload) =>
    data(apiClient.post('/api/auth/login/verify-otp', payload)),
  sendRegistrationOtp: (mobileNumber) =>
    data(apiClient.post('/api/auth/register/send-otp', { mobileNumber })),
  verifyRegistrationOtp: (payload) =>
    data(apiClient.post('/api/auth/register/verify-otp', payload)),
  completeRegistration: (token, payload) =>
    data(
      apiClient.post('/api/auth/register/complete', payload, {
        headers: { Authorization: `Bearer ${token}` },
      }),
    ),
  sendResetOtp: (mobileNumber) =>
    data(apiClient.post('/api/auth/forgot-password/send-otp', { mobileNumber })),
  verifyResetOtp: (payload) =>
    data(apiClient.post('/api/auth/forgot-password/verify-otp', payload)),
  resetPassword: (token, payload) =>
    data(
      apiClient.post('/api/auth/forgot-password/reset', payload, {
        headers: { Authorization: `Bearer ${token}` },
      }),
    ),
  me: () => data(apiClient.get('/api/auth/me')),
  updateProfile: (payload) => data(apiClient.patch('/api/auth/profile', payload)),
  updateLanguage: (preferredLanguage) =>
    data(apiClient.patch('/api/auth/preferences/language', { preferredLanguage })),
  sendEmailVerificationOtp: () =>
    data(apiClient.post('/api/auth/email/send-otp')),
  verifyEmailOtp: (otp) =>
    data(apiClient.post('/api/auth/email/verify-otp', { otp })),
  changePassword: (payload) =>
    data(apiClient.patch('/api/auth/change-password', payload)),
  logout: () => data(apiClient.post('/api/auth/logout')),
  users: () => data(apiClient.get('/api/auth/admin/users')),
  createStaff: (payload) => data(apiClient.post('/api/auth/admin/create-staff', payload)),
  updateUserRole: (userId, role) =>
    data(apiClient.patch(`/api/auth/admin/users/${userId}/role`, { role })),
  updateUserStatus: (userId, active) =>
    data(apiClient.patch(`/api/auth/admin/users/${userId}/status`, { active })),
}

export const routesApi = {
  all: () => data(apiClient.get('/api/routes')),
  get: (routeId) => data(apiClient.get(`/api/routes/${routeId}`)),
  search: (params) => data(apiClient.get('/api/routes/search', { params })),
  fare: (routeId, params) =>  data(apiClient.get(`/api/routes/${routeId}/fare`, { params })),
  create: (payload) => data(apiClient.post('/api/routes', payload)),
  update: (routeId, payload) => data(apiClient.put(`/api/routes/${routeId}`, payload)),
  remove: (routeId) => data(apiClient.delete(`/api/routes/${routeId}`)),
}

export const stopsApi = {
  all: (routeId) =>
    data(apiClient.get(`/api/routes/${routeId}/stops`)),

  get: (routeId, stopId) =>
    data(apiClient.get(`/api/routes/${routeId}/stops/${stopId}`)),

  create: (routeId, payload) =>
    data(apiClient.post(`/api/routes/${routeId}/stops`, payload)),

  update: (routeId, stopId, payload) =>
    data(apiClient.put(`/api/routes/${routeId}/stops/${stopId}`, payload)),

  remove: (routeId, stopId) =>
    data(apiClient.delete(`/api/routes/${routeId}/stops/${stopId}`)),
}

export const schedulesApi = {
  all: (routeId) =>
    data(apiClient.get(`/api/routes/${routeId}/schedules`)),

  get: (routeId, scheduleId) =>
    data(apiClient.get(`/api/routes/${routeId}/schedules/${scheduleId}`)),

  create: (routeId, payload) =>
    data(apiClient.post(`/api/routes/${routeId}/schedules`, payload)),

  update: (routeId, scheduleId, payload) =>
    data(apiClient.put(`/api/routes/${routeId}/schedules/${scheduleId}`, payload)),

  remove: (routeId, scheduleId) =>
    data(apiClient.delete(`/api/routes/${routeId}/schedules/${scheduleId}`)),
}

export const ticketsApi = {
  mine: () => data(apiClient.get('/api/tickets/me')),
  get: (ticketId) => data(apiClient.get(`/api/tickets/${ticketId}`)),
  byPnr: (pnr) => data(apiClient.get(`/api/tickets/pnr/${pnr}`)),
  book: (payload, idempotencyKey) =>
    data(
      apiClient.post('/api/tickets', payload, {
        headers: { 'Idempotency-Key': idempotencyKey },
      }),
    ),
  cancel: (ticketId) => data(apiClient.delete(`/api/tickets/${ticketId}`)),
}

export const paymentsApi = {
  mine: () => data(apiClient.get('/api/payments/me')),
  process: (payload, idempotencyKey) =>
    data(
      apiClient.post('/api/payments', payload, {
        headers: { 'Idempotency-Key': idempotencyKey },
      }),
    ),
  refund: (paymentId) =>
    data(apiClient.post(`/api/payments/${paymentId}/refund`)),
}

export const vehiclesApi = {
  all: () => data(apiClient.get('/api/vehicles')),
  get: (vehicleId) => data(apiClient.get(`/api/vehicles/${vehicleId}`)),
  create: (payload) => data(apiClient.post('/api/vehicles', payload)),
  update: (vehicleId, payload) =>
    data(apiClient.put(`/api/vehicles/${vehicleId}`, payload)),
  updateStatus: (vehicleId, status) =>
    data(apiClient.patch(`/api/vehicles/${vehicleId}/status`, { status })),
  remove: (vehicleId) => data(apiClient.delete(`/api/vehicles/${vehicleId}`)),
  assign: (vehicleId, payload) =>
    data(apiClient.post(`/api/vehicles/${vehicleId}/assignments`, payload)),
  assignments: (vehicleId) =>
    data(apiClient.get(`/api/vehicles/${vehicleId}/assignments`)),
  completeAssignment: (vehicleId, assignmentId) =>
    data(
      apiClient.patch(
        `/api/vehicles/${vehicleId}/assignments/${assignmentId}/complete`,
      ),
    ),
  cancelAssignment: (vehicleId, assignmentId) =>
    data(
      apiClient.patch(
        `/api/vehicles/${vehicleId}/assignments/${assignmentId}/cancel`,
      ),
    ),
  recordLocation: (vehicleId, payload) =>
    data(apiClient.post(`/api/vehicles/${vehicleId}/locations`, payload)),
  latestLocation: (vehicleId) =>
    data(apiClient.get(`/api/vehicles/${vehicleId}/locations/latest`)),
}

export const analyticsApi = {
  usage: (params) => data(apiClient.get('/api/analytics/usage', { params })),
  revenue: (params) => data(apiClient.get('/api/analytics/revenue', { params })),
  performance: (params) =>
    data(apiClient.get('/api/analytics/performance', { params })),
  reports: () => data(apiClient.get('/api/analytics/reports')),
  generate: (params) =>
    data(apiClient.post('/api/analytics/reports', null, { params })),
}
