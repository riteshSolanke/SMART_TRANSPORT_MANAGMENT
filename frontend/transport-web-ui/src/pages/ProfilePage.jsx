import { useEffect, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import {
  FiCheckCircle,
  FiGlobe,
  FiKey,
  FiMail,
  FiPhone,
  FiSave,
  FiSend,
  FiShield,
  FiUser,
} from 'react-icons/fi'
import toast from 'react-hot-toast'
import SectionHeader from '../components/ui/SectionHeader.jsx'
import { useAuth } from '../context/authContext.js'
import { authApi } from '../lib/api.js'
import { getErrorMessage } from '../lib/apiClient.js'
import { humanize } from '../lib/formatters.js'

export default function ProfilePage() {
  const { user, updateCurrentUser } = useAuth()
  const [emailOtp, setEmailOtp] = useState('')
  const [emailOtpSent, setEmailOtpSent] = useState(false)
  const { register, handleSubmit, reset, formState: { errors, isDirty } } = useForm({
    defaultValues: { name: user?.name || '', email: user?.email || '' },
  })
  const passwordForm = useForm({
    defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' },
  })

  useEffect(() => {
    reset({ name: user?.name || '', email: user?.email || '' })
  }, [user, reset])

  const updateProfile = useMutation({
    mutationFn: authApi.updateProfile,
    onSuccess: (updatedUser) => {
      updateCurrentUser(updatedUser)
      reset({ name: updatedUser.name || '', email: updatedUser.email || '' })
      toast.success('Profile updated')
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })
  const updateLanguage = useMutation({
    mutationFn: authApi.updateLanguage,
    onSuccess: (_, preferredLanguage) => {
      updateCurrentUser({ ...user, preferredLanguage })
      toast.success('Language preference updated')
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })
  const sendEmailOtp = useMutation({
    mutationFn: authApi.sendEmailVerificationOtp,
    onSuccess: () => {
      setEmailOtpSent(true)
      toast.success('Verification code sent to your email')
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })
  const verifyEmailOtp = useMutation({
    mutationFn: authApi.verifyEmailOtp,
    onSuccess: () => {
      updateCurrentUser({ ...user, emailVerified: true })
      setEmailOtp('')
      setEmailOtpSent(false)
      toast.success('Email verified successfully')
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })
  const changePassword = useMutation({
    mutationFn: authApi.changePassword,
    onSuccess: () => {
      passwordForm.reset()
      toast.success('Password changed. Other sessions have been signed out.')
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  function submitPassword(values) {
    if (values.newPassword !== values.confirmPassword) {
      passwordForm.setError('confirmPassword', {
        message: 'Passwords do not match',
      })
      return
    }
    changePassword.mutate({
      currentPassword: values.currentPassword,
      newPassword: values.newPassword,
    })
  }

  const initials = user?.name
    ?.split(' ')
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase()

  return (
    <div className="page-stack">
      <SectionHeader
        eyebrow="Personal settings"
        title="Your account"
        description="Keep your profile accurate and review the identity connected to this workspace."
      />

      <section className="profile-layout">
        <aside className="panel profile-card">
          <span className="avatar avatar--profile">{initials || 'TF'}</span>
          <h2>{user?.name}</h2>
          <p>{user?.email || `+91 ${user?.mobileNumber}`}</p>
          <span className="profile-card__role"><FiShield /> {humanize(user?.role)}</span>
          <div className="profile-card__verification">
            <div className={user?.mobileVerified ? 'verified' : ''}>
              {user?.mobileVerified ? <FiCheckCircle /> : <FiPhone />}
              <span><strong>Mobile</strong><small>{user?.mobileVerified ? 'Verified' : 'Pending'}</small></span>
            </div>
            <div className={user?.emailVerified ? 'verified' : ''}>
              {user?.emailVerified ? <FiCheckCircle /> : <FiMail />}
              <span><strong>Email</strong><small>{user?.emailVerified ? 'Verified' : 'Pending'}</small></span>
            </div>
          </div>
        </aside>

        <div className="profile-main">
          <section className="panel settings-card">
            <div className="panel__header panel__header--bordered">
              <div><span className="eyebrow">Profile details</span><h3>Personal information</h3></div>
            </div>
            <form
              className="form-stack"
              onSubmit={handleSubmit((values) =>
                updateProfile.mutate({ name: values.name.trim(), email: values.email || null }),
              )}
            >
              <div className="form-grid form-grid--two">
                <label className="field">
                  <span>Full name</span>
                  <div className="field__control">
                    <FiUser />
                    <input {...register('name', { required: 'Name is required', minLength: 2 })} />
                  </div>
                  {errors.name && <small className="field__error">{errors.name.message}</small>}
                </label>
                <label className="field">
                  <span>Email address</span>
                  <div className="field__control">
                    <FiMail />
                    <input type="email" {...register('email')} />
                  </div>
                </label>
              </div>
              <label className="field">
                <span>Mobile number</span>
                <div className="field__control field__control--disabled">
                  <FiPhone />
                  <input value={`+91 ${user?.mobileNumber || ''}`} disabled readOnly />
                </div>
                <small className="field__hint">Contact support to change a verified mobile number.</small>
              </label>
              <div className="form-actions">
                <button
                  className="button button--primary"
                  disabled={!isDirty || updateProfile.isPending}
                >
                  {updateProfile.isPending ? <span className="button-spinner" /> : <FiSave />}
                  Save profile
                </button>
              </div>
            </form>
          </section>

          <section className="panel settings-card">
            <div className="panel__header panel__header--bordered">
              <div><span className="eyebrow">Preferences</span><h3>Language and display</h3></div>
            </div>
            <div className="preference-row">
              <span className="preference-row__icon"><FiGlobe /></span>
              <div>
                <strong>Preferred language</strong>
                <span>Controls supported backend messages and communication.</span>
              </div>
              <select
                value={user?.preferredLanguage || 'en'}
                disabled={updateLanguage.isPending}
                onChange={(event) => updateLanguage.mutate(event.target.value)}
              >
                <option value="en">English</option>
                <option value="hi">Hindi</option>
              </select>
            </div>
          </section>

          <section className="panel settings-card">
            <div className="panel__header panel__header--bordered">
              <div><span className="eyebrow">Account security</span><h3>Verification and password</h3></div>
            </div>

            <div className="form-stack">
              <div className="preference-row">
                <span className="preference-row__icon"><FiMail /></span>
                <div>
                  <strong>Email verification</strong>
                  <span>
                    {!user?.email
                      ? 'Add an email address before requesting verification.'
                      : user.emailVerified
                        ? 'Your email address is verified.'
                        : `Verify ${user.email} with a one-time code.`}
                  </span>
                </div>
                {user?.email && !user.emailVerified && !emailOtpSent && (
                  <button
                    type="button"
                    className="button button--ghost button--small"
                    disabled={sendEmailOtp.isPending}
                    onClick={() => sendEmailOtp.mutate()}
                  >
                    {sendEmailOtp.isPending ? <span className="button-spinner" /> : <FiSend />}
                    Send code
                  </button>
                )}
              </div>

              {emailOtpSent && !user?.emailVerified && (
                <div className="form-row">
                  <label className="field">
                    <span>Email verification code</span>
                    <div className="field__control field__control--otp">
                      <input
                        inputMode="numeric"
                        maxLength={6}
                        value={emailOtp}
                        onChange={(event) => setEmailOtp(event.target.value)}
                      />
                    </div>
                  </label>
                  <button
                    type="button"
                    className="button button--primary"
                    disabled={verifyEmailOtp.isPending || !/^\d{6}$/.test(emailOtp)}
                    onClick={() => verifyEmailOtp.mutate(emailOtp)}
                  >
                    {verifyEmailOtp.isPending ? <span className="button-spinner" /> : <FiCheckCircle />}
                    Verify email
                  </button>
                </div>
              )}

              <form
                className="form-stack"
                onSubmit={passwordForm.handleSubmit(submitPassword)}
              >
                <div className="form-grid form-grid--two">
                  <label className="field">
                    <span>Current password</span>
                    <div className="field__control">
                      <FiKey />
                      <input
                        type="password"
                        autoComplete="current-password"
                        {...passwordForm.register('currentPassword', {
                          required: 'Current password is required',
                        })}
                      />
                    </div>
                    {passwordForm.formState.errors.currentPassword && (
                      <small className="field__error">
                        {passwordForm.formState.errors.currentPassword.message}
                      </small>
                    )}
                  </label>
                  <label className="field">
                    <span>New password</span>
                    <div className="field__control">
                      <FiKey />
                      <input
                        type="password"
                        autoComplete="new-password"
                        {...passwordForm.register('newPassword', {
                          required: 'New password is required',
                          minLength: {
                            value: 8,
                            message: 'Use at least 8 characters',
                          },
                          pattern: {
                            value: /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d).+$/,
                            message: 'Include uppercase, lowercase and a number',
                          },
                        })}
                      />
                    </div>
                    {passwordForm.formState.errors.newPassword && (
                      <small className="field__error">
                        {passwordForm.formState.errors.newPassword.message}
                      </small>
                    )}
                  </label>
                </div>
                <label className="field">
                  <span>Confirm new password</span>
                  <div className="field__control">
                    <FiKey />
                    <input
                      type="password"
                      autoComplete="new-password"
                      {...passwordForm.register('confirmPassword', {
                        required: 'Confirm your new password',
                      })}
                    />
                  </div>
                  {passwordForm.formState.errors.confirmPassword && (
                    <small className="field__error">
                      {passwordForm.formState.errors.confirmPassword.message}
                    </small>
                  )}
                </label>
                <div className="form-actions">
                  <button
                    className="button button--primary"
                    disabled={changePassword.isPending}
                  >
                    {changePassword.isPending ? <span className="button-spinner" /> : <FiKey />}
                    Change password
                  </button>
                </div>
              </form>
            </div>
          </section>
        </div>
      </section>
    </div>
  )
}
