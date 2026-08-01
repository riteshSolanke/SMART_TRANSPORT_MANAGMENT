import { beforeEach, describe, expect, it, vi } from 'vitest'

const client = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  patch: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
}))

vi.mock('./apiClient.js', () => ({
  apiClient: client,
  unwrap: (response) => response.data.data,
}))

import { authApi } from './api.js'

const success = { data: { data: { ok: true } } }

describe('authApi HTTP contract', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    Object.values(client).forEach((method) => method.mockResolvedValue(success))
  })

  it('uses POST with JSON for password and OTP login', async () => {
    await authApi.loginPassword({ email: 'test@example.com', password: 'Password1' })
    await authApi.sendLoginOtp('9876543210')
    await authApi.verifyLoginOtp({ mobileNumber: '9876543210', otp: '123456' })

    expect(client.post).toHaveBeenNthCalledWith(
      1,
      '/api/auth/login/password',
      { email: 'test@example.com', password: 'Password1' },
    )
    expect(client.post).toHaveBeenNthCalledWith(
      2,
      '/api/auth/login/send-otp',
      { mobileNumber: '9876543210' },
    )
    expect(client.post).toHaveBeenNthCalledWith(
      3,
      '/api/auth/login/verify-otp',
      { mobileNumber: '9876543210', otp: '123456' },
    )
    expect(client.get).not.toHaveBeenCalled()
  })

  it('matches the three-step registration contract', async () => {
    const profile = {
      name: 'API Test',
      email: 'api@example.com',
      password: 'Password1',
      preferredLanguage: 'en',
    }

    await authApi.sendRegistrationOtp('9876543210')
    await authApi.verifyRegistrationOtp({
      mobileNumber: '9876543210',
      otp: '123456',
    })
    await authApi.completeRegistration('registration-token', profile)

    expect(client.post).toHaveBeenNthCalledWith(
      1,
      '/api/auth/register/send-otp',
      { mobileNumber: '9876543210' },
    )
    expect(client.post).toHaveBeenNthCalledWith(
      2,
      '/api/auth/register/verify-otp',
      { mobileNumber: '9876543210', otp: '123456' },
    )
    expect(client.post).toHaveBeenNthCalledWith(
      3,
      '/api/auth/register/complete',
      profile,
      { headers: { Authorization: 'Bearer registration-token' } },
    )
  })

  it('matches the password-reset contract', async () => {
    await authApi.sendResetOtp('9876543210')
    await authApi.verifyResetOtp({ mobileNumber: '9876543210', otp: '123456' })
    await authApi.resetPassword('reset-token', {
      mobileNumber: '9876543210',
      newPassword: 'NewPassword1',
    })

    expect(client.post).toHaveBeenNthCalledWith(
      1,
      '/api/auth/forgot-password/send-otp',
      { mobileNumber: '9876543210' },
    )
    expect(client.post).toHaveBeenNthCalledWith(
      2,
      '/api/auth/forgot-password/verify-otp',
      { mobileNumber: '9876543210', otp: '123456' },
    )
    expect(client.post).toHaveBeenNthCalledWith(
      3,
      '/api/auth/forgot-password/reset',
      { mobileNumber: '9876543210', newPassword: 'NewPassword1' },
      { headers: { Authorization: 'Bearer reset-token' } },
    )
  })

  it('matches protected profile and account endpoints', async () => {
    await authApi.me()
    await authApi.updateProfile({ name: 'Updated Name' })
    await authApi.updateLanguage('hi')
    await authApi.sendEmailVerificationOtp()
    await authApi.verifyEmailOtp('123456')
    await authApi.changePassword({
      currentPassword: 'Password1',
      newPassword: 'NewPassword1',
    })
    await authApi.logout()

    expect(client.get).toHaveBeenCalledWith('/api/auth/me')
    expect(client.patch).toHaveBeenNthCalledWith(
      1,
      '/api/auth/profile',
      { name: 'Updated Name' },
    )
    expect(client.patch).toHaveBeenNthCalledWith(
      2,
      '/api/auth/preferences/language',
      { preferredLanguage: 'hi' },
    )
    expect(client.post).toHaveBeenNthCalledWith(1, '/api/auth/email/send-otp')
    expect(client.post).toHaveBeenNthCalledWith(
      2,
      '/api/auth/email/verify-otp',
      { otp: '123456' },
    )
    expect(client.patch).toHaveBeenNthCalledWith(
      3,
      '/api/auth/change-password',
      { currentPassword: 'Password1', newPassword: 'NewPassword1' },
    )
    expect(client.post).toHaveBeenNthCalledWith(3, '/api/auth/logout')
  })

  it('matches admin user-management endpoints', async () => {
    const staff = {
      mobileNumber: '9876543210',
      name: 'Test Conductor',
      email: 'conductor@example.com',
      password: 'Password1',
      role: 'CONDUCTOR',
    }

    await authApi.users()
    await authApi.createStaff(staff)
    await authApi.updateUserRole(42, 'DISPATCHER')
    await authApi.updateUserStatus(42, false)

    expect(client.get).toHaveBeenCalledWith('/api/auth/admin/users')
    expect(client.post).toHaveBeenCalledWith('/api/auth/admin/create-staff', staff)
    expect(client.patch).toHaveBeenNthCalledWith(
      1,
      '/api/auth/admin/users/42/role',
      { role: 'DISPATCHER' },
    )
    expect(client.patch).toHaveBeenNthCalledWith(
      2,
      '/api/auth/admin/users/42/status',
      { active: false },
    )
  })
})
