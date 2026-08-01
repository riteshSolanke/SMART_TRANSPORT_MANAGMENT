import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import {
  FiCheckCircle,
  FiMail,
  FiPhone,
  FiPlus,
  FiSearch,
  FiShield,
  FiUserCheck,
  FiUsers,
} from 'react-icons/fi'
import toast from 'react-hot-toast'
import EmptyState from '../components/ui/EmptyState.jsx'
import ErrorState from '../components/ui/ErrorState.jsx'
import Modal from '../components/ui/Modal.jsx'
import PageLoader from '../components/ui/PageLoader.jsx'
import SectionHeader from '../components/ui/SectionHeader.jsx'
import StatCard from '../components/ui/StatCard.jsx'
import StatusBadge from '../components/ui/StatusBadge.jsx'
import { confirmAction } from '../lib/alerts.js'
import { authApi } from '../lib/api.js'
import { getErrorMessage } from '../lib/apiClient.js'
import { humanize } from '../lib/formatters.js'

const roles = ['PASSENGER', 'CONDUCTOR', 'DISPATCHER', 'TRANSPORT_MANAGER', 'ADMIN']
const staffRoles = ['CONDUCTOR', 'DISPATCHER', 'TRANSPORT_MANAGER', 'ADMIN']

export default function UsersPage() {
  const queryClient = useQueryClient()
  const [query, setQuery] = useState('')
  const [roleFilter, setRoleFilter] = useState('ALL')
  const [staffModal, setStaffModal] = useState(false)
  const staffForm = useForm({
    defaultValues: {
      mobileNumber: '',
      name: '',
      email: '',
      password: '',
      role: 'CONDUCTOR',
    },
  })

  const usersQuery = useQuery({
    queryKey: ['admin', 'users'],
    queryFn: authApi.users,
  })
  const createStaff = useMutation({
    mutationFn: authApi.createStaff,
    onSuccess: () => {
      toast.success('Staff account created')
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
      staffForm.reset()
      setStaffModal(false)
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })
  const updateRole = useMutation({
    mutationFn: ({ userId, role }) => authApi.updateUserRole(userId, role),
    onSuccess: () => {
      toast.success('User role updated')
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })
  const updateStatus = useMutation({
    mutationFn: ({ userId, active }) => authApi.updateUserStatus(userId, active),
    onSuccess: () => {
      toast.success('User status updated')
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  const users = useMemo(() => usersQuery.data || [], [usersQuery.data])
  const filtered = useMemo(() => {
    const needle = query.toLowerCase().trim()
    return users.filter((user) => {
      const matchesRole = roleFilter === 'ALL' || user.role === roleFilter
      const matchesQuery =
        !needle ||
        user.name?.toLowerCase().includes(needle) ||
        user.email?.toLowerCase().includes(needle) ||
        user.mobileNumber?.includes(needle)
      return matchesRole && matchesQuery
    })
  }, [users, query, roleFilter])

  async function changeRole(user, nextRole) {
    if (user.role === nextRole) return
    const result = await confirmAction({
      title: 'Change user role?',
      text: `${user.name} will receive ${humanize(nextRole)} permissions immediately.`,
      confirmText: 'Change role',
      icon: 'warning',
    })
    if (result.isConfirmed) updateRole.mutate({ userId: user.userId, role: nextRole })
  }

  async function changeStatus(user, active) {
    if (user.active === active) return
    const result = await confirmAction({
      title: active ? 'Activate this account?' : 'Deactivate this account?',
      text: active
        ? `${user.name} will be able to sign in again.`
        : `${user.name} will be signed out and blocked from signing in.`,
      confirmText: active ? 'Activate account' : 'Deactivate account',
      icon: active ? 'question' : 'warning',
      confirmColor: active ? '#0d9488' : '#dc2626',
    })
    if (result.isConfirmed) updateStatus.mutate({ userId: user.userId, active })
  }

  if (usersQuery.isLoading) return <PageLoader label="Loading user access records" />
  if (usersQuery.isError) {
    return <ErrorState error={usersQuery.error} onRetry={usersQuery.refetch} />
  }

  return (
    <div className="page-stack">
      <SectionHeader
        eyebrow="Identity administration"
        title="Users and access"
        description="Create staff accounts and manage role-based access to the transport platform."
        actions={
          <button className="button button--primary" onClick={() => setStaffModal(true)}>
            <FiPlus /> Create staff
          </button>
        }
      />

      <section className="stat-grid stat-grid--four">
        <StatCard label="Total users" value={users.length} icon={FiUsers} tone="blue" />
        <StatCard
          label="Passengers"
          value={users.filter((user) => user.role === 'PASSENGER').length}
          icon={FiUserCheck}
          tone="teal"
        />
        <StatCard
          label="Operations staff"
          value={users.filter((user) => ['CONDUCTOR', 'DISPATCHER'].includes(user.role)).length}
          icon={FiShield}
          tone="amber"
        />
        <StatCard
          label="Verified email"
          value={users.filter((user) => user.emailVerified).length}
          icon={FiCheckCircle}
          tone="violet"
        />
      </section>

      <section className="panel">
        <div className="panel__header panel__header--bordered user-toolbar">
          <div>
            <span className="eyebrow">Directory</span>
            <h3>Platform users</h3>
          </div>
          <div className="user-toolbar__filters">
            <label className="compact-search">
              <FiSearch />
              <input
                placeholder="Search name, email or mobile"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
              />
            </label>
            <select value={roleFilter} onChange={(event) => setRoleFilter(event.target.value)}>
              <option value="ALL">All roles</option>
              {roles.map((role) => (
                <option value={role} key={role}>{humanize(role)}</option>
              ))}
            </select>
          </div>
        </div>

        {filtered.length ? (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>User</th>
                  <th>Contact</th>
                  <th>Verification</th>
                  <th>Status</th>
                  <th>Role</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((user) => (
                  <tr key={user.userId}>
                    <td>
                      <div className="user-cell">
                        <span className="avatar">
                          {user.name?.split(' ').slice(0, 2).map((part) => part[0]).join('').toUpperCase()}
                        </span>
                        <div><strong>{user.name}</strong><small>User #{user.userId}</small></div>
                      </div>
                    </td>
                    <td>
                      <span className="contact-line"><FiMail /> {user.email || 'No email'}</span>
                      <small className="contact-line"><FiPhone /> +91 {user.mobileNumber}</small>
                    </td>
                    <td>
                      <div className="verification-stack">
                        <StatusBadge status={user.mobileVerified ? 'ACTIVE' : 'PENDING'} />
                        <span>{user.emailVerified ? 'Email verified' : 'Email pending'}</span>
                      </div>
                    </td>
                    <td>
                      <select
                        className="role-select"
                        value={user.active ? 'ACTIVE' : 'INACTIVE'}
                        disabled={updateStatus.isPending}
                        onChange={(event) =>
                          changeStatus(user, event.target.value === 'ACTIVE')
                        }
                      >
                        <option value="ACTIVE">Active</option>
                        <option value="INACTIVE">Inactive</option>
                      </select>
                    </td>
                    <td>
                      <select
                        className="role-select"
                        value={user.role}
                        disabled={updateRole.isPending}
                        onChange={(event) => changeRole(user, event.target.value)}
                      >
                        {roles.map((role) => (
                          <option value={role} key={role}>{humanize(role)}</option>
                        ))}
                      </select>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState
            icon={FiUsers}
            title="No users match these filters"
            description="Try another name, contact detail or role."
          />
        )}
      </section>

      <Modal
        open={staffModal}
        onClose={() => setStaffModal(false)}
        title="Create staff account"
        description="The staff member can sign in immediately with these credentials."
      >
        <form
          className="form-stack"
          onSubmit={staffForm.handleSubmit((values) =>
            createStaff.mutate({ ...values, email: values.email || null }),
          )}
        >
          <div className="form-grid form-grid--two">
            <label className="field">
              <span>Full name</span>
              <div className="field__control">
                <FiUserCheck />
                <input {...staffForm.register('name', { required: true })} />
              </div>
            </label>
            <label className="field">
              <span>Role</span>
              <div className="field__control">
                <FiShield />
                <select {...staffForm.register('role')}>
                  {staffRoles.map((role) => (
                    <option value={role} key={role}>{humanize(role)}</option>
                  ))}
                </select>
              </div>
            </label>
          </div>
          <label className="field">
            <span>Mobile number</span>
            <div className="field__control">
              <FiPhone />
              <span className="field__prefix">+91</span>
              <input
                inputMode="numeric"
                maxLength={10}
                {...staffForm.register('mobileNumber', {
                  required: true,
                  pattern: /^[6-9]\d{9}$/,
                })}
              />
            </div>
          </label>
          <label className="field">
            <span>Email address <em>optional</em></span>
            <div className="field__control">
              <FiMail />
              <input type="email" {...staffForm.register('email')} />
            </div>
          </label>
          <label className="field">
            <span>Temporary password</span>
            <div className="field__control">
              <input
                type="password"
                autoComplete="new-password"
                {...staffForm.register('password', { required: true, minLength: 6 })}
              />
            </div>
          </label>
          <div className="modal__actions">
            <button type="button" className="button button--ghost" onClick={() => setStaffModal(false)}>
              Cancel
            </button>
            <button className="button button--primary" disabled={createStaff.isPending}>
              {createStaff.isPending && <span className="button-spinner" />}
              Create staff account
            </button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
