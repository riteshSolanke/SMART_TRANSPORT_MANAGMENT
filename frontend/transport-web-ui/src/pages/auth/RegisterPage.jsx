import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { FiArrowLeft, FiArrowRight, FiCheck, FiLock, FiMail, FiPhone, FiUser } from 'react-icons/fi'
import toast from 'react-hot-toast'
import AuthLayout from './AuthLayout.jsx'
import { authApi } from '../../lib/api.js'
import { getErrorMessage } from '../../lib/apiClient.js'
import { useAuth } from '../../context/authContext.js'

export default function RegisterPage() {
  const navigate = useNavigate()
  const { establishSession } = useAuth()
  const [step, setStep] = useState(1)
  const [mobileNumber, setMobileNumber] = useState('')
  const [registrationToken, setRegistrationToken] = useState('')
  const { register, handleSubmit, getValues, formState: { errors }, setError } = useForm({
    defaultValues: {
      mobileNumber: '',
      otp: '',
      name: '',
      email: '',
      password: '',
      preferredLanguage: 'en',
    },
  })

  const sendOtp = useMutation({
    mutationFn: authApi.sendRegistrationOtp,
    onSuccess: () => {
      setStep(2)
      toast.success('OTP sent. It will remain valid for a few minutes.')
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })
  const verifyOtp = useMutation({
    mutationFn: authApi.verifyRegistrationOtp,
    onSuccess: (token) => {
      setRegistrationToken(token)
      setStep(3)
      toast.success('Mobile number verified')
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })
  const complete = useMutation({
    mutationFn: ({ token, payload }) => authApi.completeRegistration(token, payload),
    onSuccess: (response) => {
      establishSession(response)
      toast.success('Your passenger account is ready')
      navigate('/dashboard', { replace: true })
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  const pending = sendOtp.isPending || verifyOtp.isPending || complete.isPending

  function submit(values) {
    if (step === 1) {
      if (!/^[6-9]\d{9}$/.test(values.mobileNumber)) {
        setError('mobileNumber', { message: 'Enter a valid 10-digit mobile number' })
        return
      }
      setMobileNumber(values.mobileNumber)
      sendOtp.mutate(values.mobileNumber)
      return
    }
    if (step === 2) {
      if (!/^\d{6}$/.test(values.otp)) {
        setError('otp', { message: 'Enter the 6-digit OTP' })
        return
      }
      verifyOtp.mutate({ mobileNumber, otp: values.otp })
      return
    }
    if (!values.name || values.name.trim().length < 2) {
      setError('name', { message: 'Enter your full name' })
      return
    }
    if (!/(?=.*[A-Z])(?=.*[a-z])(?=.*\d).{5,}/.test(values.password)) {
      setError('password', {
        message: 'Use uppercase, lowercase and a number (minimum 5 characters)',
      })
      return
    }
    complete.mutate({
      token: registrationToken,
      payload: {
        name: values.name.trim(),
        email: values.email || null,
        password: values.password,
        preferredLanguage: values.preferredLanguage,
      },
    })
  }

  return (
    <AuthLayout
      eyebrow={`Passenger registration · Step ${step} of 3`}
      title={
        step === 1
          ? 'Start with your mobile'
          : step === 2
            ? 'Verify it’s really you'
            : 'Complete your account'
      }
      description="Your account gives you secure access to bookings, payments and journey history."
    >
      <div className="stepper" aria-label={`Registration step ${step} of 3`}>
        {[1, 2, 3].map((number) => (
          <span key={number} className={number <= step ? 'active' : ''}>
            {number < step ? <FiCheck /> : number}
          </span>
        ))}
      </div>

      <form className="form-stack" onSubmit={handleSubmit(submit)}>
        {step === 1 && (
          <label className="field">
            <span>Mobile number</span>
            <div className="field__control">
              <FiPhone />
              <span className="field__prefix">+91</span>
              <input
                inputMode="numeric"
                maxLength={10}
                placeholder="98765 43210"
                autoFocus
                {...register('mobileNumber')}
              />
            </div>
            {errors.mobileNumber && (
              <small className="field__error">{errors.mobileNumber.message}</small>
            )}
          </label>
        )}

        {step === 2 && (
          <>
            <div className="verification-note">
              We sent a 6-digit code to <strong>+91 {mobileNumber}</strong>
            </div>
            <label className="field">
              <span>One-time password</span>
              <div className="field__control field__control--otp">
                <input
                  inputMode="numeric"
                  maxLength={6}
                  placeholder="••••••"
                  autoFocus
                  {...register('otp')}
                />
              </div>
              {errors.otp && <small className="field__error">{errors.otp.message}</small>}
              <button
                type="button"
                className="text-button"
                onClick={() => sendOtp.mutate(mobileNumber)}
              >
                Resend OTP
              </button>
            </label>
          </>
        )}

        {step === 3 && (
          <>
            <div className="form-grid form-grid--two">
              <label className="field">
                <span>Full name</span>
                <div className="field__control">
                  <FiUser />
                  <input placeholder="Aarav Sharma" {...register('name')} />
                </div>
                {errors.name && <small className="field__error">{errors.name.message}</small>}
              </label>
              <label className="field">
                <span>Email address <em>optional</em></span>
                <div className="field__control">
                  <FiMail />
                  <input type="email" placeholder="you@example.com" {...register('email')} />
                </div>
              </label>
            </div>
            <label className="field">
              <span>Create password</span>
              <div className="field__control">
                <FiLock />
                <input
                  type="password"
                  placeholder="Uppercase, lowercase and a number"
                  autoComplete="new-password"
                  {...register('password')}
                />
              </div>
              {errors.password && (
                <small className="field__error">{errors.password.message}</small>
              )}
            </label>
            <label className="field">
              <span>Preferred language</span>
              <div className="field__control">
                <select {...register('preferredLanguage')}>
                  <option value="en">English</option>
                  <option value="hi">Hindi</option>
                </select>
              </div>
            </label>
          </>
        )}

        <button className="button button--primary button--block" disabled={pending}>
          {pending ? <span className="button-spinner" /> : step === 3 ? 'Create account' : 'Continue'}
          {!pending && <FiArrowRight />}
        </button>
        {step > 1 && (
          <button
            type="button"
            className="button button--ghost button--block"
            onClick={() => setStep((value) => value - 1)}
          >
            <FiArrowLeft />
            Back
          </button>
        )}
      </form>

      <p className="auth-switch">
        Already registered? <Link to="/login">Sign in</Link>
      </p>
      {step === 2 && (
        <span className="sr-only">Current mobile number: {getValues('mobileNumber')}</span>
      )}
    </AuthLayout>
  )
}
