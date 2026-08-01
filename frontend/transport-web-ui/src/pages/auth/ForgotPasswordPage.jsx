import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { FiArrowLeft, FiArrowRight, FiCheck, FiLock, FiPhone } from 'react-icons/fi'
import toast from 'react-hot-toast'
import AuthLayout from './AuthLayout.jsx'
import { authApi } from '../../lib/api.js'
import { getErrorMessage } from '../../lib/apiClient.js'

export default function ForgotPasswordPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState(1)
  const [mobileNumber, setMobileNumber] = useState('')
  const [resetToken, setResetToken] = useState('')
  const { register, handleSubmit, setError, formState: { errors } } = useForm({
    defaultValues: { mobileNumber: '', otp: '', newPassword: '', confirmPassword: '' },
  })

  const sendOtp = useMutation({
    mutationFn: authApi.sendResetOtp,
    onSuccess: () => {
      setStep(2)
      toast.success('Password reset OTP sent')
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })
  const verifyOtp = useMutation({
    mutationFn: authApi.verifyResetOtp,
    onSuccess: (token) => {
      setResetToken(token)
      setStep(3)
      toast.success('OTP verified')
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })
  const reset = useMutation({
    mutationFn: ({ token, payload }) => authApi.resetPassword(token, payload),
    onSuccess: () => {
      toast.success('Password updated. You can sign in now.')
      navigate('/login', { replace: true })
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })

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
    if (!/(?=.*[A-Z])(?=.*[a-z])(?=.*\d).{5,}/.test(values.newPassword)) {
      setError('newPassword', { message: 'Use uppercase, lowercase and a number' })
      return
    }
    if (values.newPassword !== values.confirmPassword) {
      setError('confirmPassword', { message: 'Passwords do not match' })
      return
    }
    reset.mutate({
      token: resetToken,
      payload: { mobileNumber, newPassword: values.newPassword },
    })
  }

  const pending = sendOtp.isPending || verifyOtp.isPending || reset.isPending

  return (
    <AuthLayout
      eyebrow="Account recovery"
      title={step === 3 ? 'Choose a new password' : 'Reset your password'}
      description="Verify your registered mobile number and we’ll help you get back on board."
    >
      <div className="stepper stepper--compact">
        {[1, 2, 3].map((number) => (
          <span key={number} className={number <= step ? 'active' : ''}>
            {number < step ? <FiCheck /> : number}
          </span>
        ))}
      </div>
      <form className="form-stack" onSubmit={handleSubmit(submit)}>
        {step === 1 && (
          <label className="field">
            <span>Registered mobile number</span>
            <div className="field__control">
              <FiPhone />
              <span className="field__prefix">+91</span>
              <input inputMode="numeric" maxLength={10} {...register('mobileNumber')} />
            </div>
            {errors.mobileNumber && (
              <small className="field__error">{errors.mobileNumber.message}</small>
            )}
          </label>
        )}
        {step === 2 && (
          <label className="field">
            <span>One-time password</span>
            <div className="field__control field__control--otp">
              <input inputMode="numeric" maxLength={6} {...register('otp')} />
            </div>
            {errors.otp && <small className="field__error">{errors.otp.message}</small>}
          </label>
        )}
        {step === 3 && (
          <>
            <label className="field">
              <span>New password</span>
              <div className="field__control">
                <FiLock />
                <input type="password" {...register('newPassword')} />
              </div>
              {errors.newPassword && (
                <small className="field__error">{errors.newPassword.message}</small>
              )}
            </label>
            <label className="field">
              <span>Confirm new password</span>
              <div className="field__control">
                <FiLock />
                <input type="password" {...register('confirmPassword')} />
              </div>
              {errors.confirmPassword && (
                <small className="field__error">{errors.confirmPassword.message}</small>
              )}
            </label>
          </>
        )}
        <button className="button button--primary button--block" disabled={pending}>
          {pending ? <span className="button-spinner" /> : step === 3 ? 'Reset password' : 'Continue'}
          {!pending && <FiArrowRight />}
        </button>
      </form>
      <p className="auth-switch">
        <Link to="/login">
          <FiArrowLeft /> Back to sign in
        </Link>
      </p>
    </AuthLayout>
  )
}
