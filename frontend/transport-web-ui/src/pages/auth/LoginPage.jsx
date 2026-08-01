import { useEffect, useState } from 'react'
import { Link, Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { FiArrowRight, FiEye, FiEyeOff, FiLock, FiMail, FiPhone } from 'react-icons/fi'
import toast from 'react-hot-toast'
import AuthLayout from './AuthLayout.jsx'
import { authApi } from '../../lib/api.js'
import { getErrorMessage } from '../../lib/apiClient.js'
import { useAuth } from '../../context/authContext.js'

const passwordSchema = z.object({
  email: z.email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
})

const otpSchema = z.object({
  mobileNumber: z
    .string()
    .regex(/^[6-9]\d{9}$/, 'Enter a valid 10-digit mobile number'),
  otp: z.string().optional(),
})

export default function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const { isAuthenticated, establishSession } = useAuth()
  const [mode, setMode] = useState('password')
  const [showPassword, setShowPassword] = useState(false)
  const [otpSent, setOtpSent] = useState(false)

  const passwordForm = useForm({
    resolver: zodResolver(passwordSchema),
    defaultValues: { email: '', password: '' },
  })
  const otpForm = useForm({
    resolver: zodResolver(otpSchema),
    defaultValues: { mobileNumber: '', otp: '' },
  })

  useEffect(() => {
    if (searchParams.get('reason') === 'session-expired') {
      toast.error('Your session expired. Please sign in again.')
    }
  }, [searchParams])

  const finishLogin = (response) => {
    establishSession(response)
    toast.success(`Welcome back, ${response.user?.name || 'traveller'}!`)
    navigate(location.state?.from || '/dashboard', { replace: true })
  }

  const passwordLogin = useMutation({
    mutationFn: authApi.loginPassword,
    onSuccess: finishLogin,
    onError: (error) => toast.error(getErrorMessage(error, 'Unable to sign in')),
  })

  const sendOtp = useMutation({
    mutationFn: authApi.sendLoginOtp,
    onSuccess: () => {
      setOtpSent(true)
      toast.success('A one-time password has been sent to your mobile')
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Unable to send OTP')),
  })

  const verifyOtp = useMutation({
    mutationFn: authApi.verifyLoginOtp,
    onSuccess: finishLogin,
    onError: (error) => toast.error(getErrorMessage(error, 'Unable to verify OTP')),
  })

  if (isAuthenticated) return <Navigate to="/dashboard" replace />

  const isPending = passwordLogin.isPending || sendOtp.isPending || verifyOtp.isPending

  return (
    <AuthLayout
      eyebrow="Welcome back"
      title="Sign in to TransitFlow"
      description="Choose your preferred secure sign-in method to continue."
    >
      <div className="auth-tabs" role="tablist" aria-label="Sign-in method">
        <button
          className={mode === 'password' ? 'active' : ''}
          onClick={() => setMode('password')}
          role="tab"
          aria-selected={mode === 'password'}
        >
          Password
        </button>
        <button
          className={mode === 'otp' ? 'active' : ''}
          onClick={() => setMode('otp')}
          role="tab"
          aria-selected={mode === 'otp'}
        >
          Mobile OTP
        </button>
      </div>

      {mode === 'password' ? (
        <form
          className="form-stack"
          onSubmit={passwordForm.handleSubmit((values) => passwordLogin.mutate(values))}
        >
          <label className="field">
            <span>Email address</span>
            <div className="field__control">
              <FiMail />
              <input
                type="email"
                placeholder="you@example.com"
                autoComplete="email"
                {...passwordForm.register('email')}
              />
            </div>
            {passwordForm.formState.errors.email && (
              <small className="field__error">
                {passwordForm.formState.errors.email.message}
              </small>
            )}
          </label>

          <label className="field">
            <span>Password</span>
            <div className="field__control">
              <FiLock />
              <input
                type={showPassword ? 'text' : 'password'}
                placeholder="Enter your password"
                autoComplete="current-password"
                {...passwordForm.register('password')}
              />
              <button
                type="button"
                className="field__action"
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                onClick={() => setShowPassword((value) => !value)}
              >
                {showPassword ? <FiEyeOff /> : <FiEye />}
              </button>
            </div>
            {passwordForm.formState.errors.password && (
              <small className="field__error">
                {passwordForm.formState.errors.password.message}
              </small>
            )}
          </label>

          <div className="form-row form-row--between">
            <label className="check-field">
              <input type="checkbox" />
              <span>Remember this device</span>
            </label>
            <Link to="/forgot-password">Forgot password?</Link>
          </div>

          <button className="button button--primary button--block" disabled={isPending}>
            {passwordLogin.isPending ? <span className="button-spinner" /> : 'Sign in'}
            {!passwordLogin.isPending && <FiArrowRight />}
          </button>
        </form>
      ) : (
        <form
          className="form-stack"
          onSubmit={otpForm.handleSubmit((values) => {
            if (!otpSent) sendOtp.mutate(values.mobileNumber)
            else if (!values.otp || values.otp.length !== 6) {
              otpForm.setError('otp', { message: 'Enter the 6-digit OTP' })
            } else verifyOtp.mutate(values)
          })}
        >
          <label className="field">
            <span>Mobile number</span>
            <div className="field__control">
              <FiPhone />
              <span className="field__prefix">+91</span>
              <input
                inputMode="numeric"
                maxLength={10}
                placeholder="98765 43210"
                disabled={otpSent}
                {...otpForm.register('mobileNumber')}
              />
            </div>
            {otpForm.formState.errors.mobileNumber && (
              <small className="field__error">
                {otpForm.formState.errors.mobileNumber.message}
              </small>
            )}
          </label>
          {otpSent && (
            <label className="field">
              <span>One-time password</span>
              <div className="field__control field__control--otp">
                <input
                  inputMode="numeric"
                  maxLength={6}
                  placeholder="••••••"
                  autoFocus
                  {...otpForm.register('otp')}
                />
              </div>
              {otpForm.formState.errors.otp && (
                <small className="field__error">{otpForm.formState.errors.otp.message}</small>
              )}
              <button
                type="button"
                className="text-button"
                disabled={sendOtp.isPending}
                onClick={() => sendOtp.mutate(otpForm.getValues('mobileNumber'))}
              >
                Resend OTP
              </button>
            </label>
          )}
          <button className="button button--primary button--block" disabled={isPending}>
            {isPending ? (
              <span className="button-spinner" />
            ) : otpSent ? (
              'Verify and sign in'
            ) : (
              'Send one-time password'
            )}
            {!isPending && <FiArrowRight />}
          </button>
          {otpSent && (
            <button
              type="button"
              className="button button--ghost button--block"
              onClick={() => {
                setOtpSent(false)
                otpForm.setValue('otp', '')
              }}
            >
              Use a different mobile number
            </button>
          )}
        </form>
      )}

      <p className="auth-switch">
        New to TransitFlow? <Link to="/register">Create passenger account</Link>
      </p>
    </AuthLayout>
  )
}
