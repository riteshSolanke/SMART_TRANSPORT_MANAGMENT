import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import {
  FiBarChart2,
  FiCalendar,
  FiCheckCircle,
  FiDollarSign,
  FiFileText,
  FiRefreshCw,
  FiUsers,
} from 'react-icons/fi'
import toast from 'react-hot-toast'
import EmptyState from '../components/ui/EmptyState.jsx'
import ErrorState from '../components/ui/ErrorState.jsx'
import PageLoader from '../components/ui/PageLoader.jsx'
import SectionHeader from '../components/ui/SectionHeader.jsx'
import StatCard from '../components/ui/StatCard.jsx'
import { confirmAction } from '../lib/alerts.js'
import { analyticsApi } from '../lib/api.js'
import { getErrorMessage } from '../lib/apiClient.js'
import { formatCurrency, formatDate, formatNumber } from '../lib/formatters.js'

const colors = ['#0d9488', '#2563eb', '#f59e0b', '#7c3aed', '#e11d48']

function defaultRange() {
  const now = new Date()
  const to = now.toISOString().slice(0, 10)
  const from = new Date(now.getFullYear(), now.getMonth(), 1)
    .toISOString()
    .slice(0, 10)
  return { from, to }
}

export default function AnalyticsPage() {
  const queryClient = useQueryClient()
  const [draftRange, setDraftRange] = useState(defaultRange)
  const [range, setRange] = useState(defaultRange)
  const params = { from: range.from, to: range.to }

  const usageQuery = useQuery({
    queryKey: ['analytics', 'usage', range],
    queryFn: () => analyticsApi.usage(params),
  })
  const revenueQuery = useQuery({
    queryKey: ['analytics', 'revenue', range],
    queryFn: () => analyticsApi.revenue(params),
  })
  const performanceQuery = useQuery({
    queryKey: ['analytics', 'performance', range],
    queryFn: () => analyticsApi.performance(params),
  })
  const reportsQuery = useQuery({
    queryKey: ['analytics', 'reports'],
    queryFn: analyticsApi.reports,
  })
  const generateReport = useMutation({
    mutationFn: () => analyticsApi.generate(params),
    onSuccess: (report) => {
      toast.success(`Service report #${report.reportId} generated`)
      queryClient.invalidateQueries({ queryKey: ['analytics', 'reports'] })
    },
    onError: (error) => toast.error(getErrorMessage(error, 'Unable to generate report')),
  })

  const usage = usageQuery.data
  const revenue = revenueQuery.data
  const performance = performanceQuery.data
  const reports = reportsQuery.data || []
  const loading =
    usageQuery.isLoading || revenueQuery.isLoading || performanceQuery.isLoading
  const firstError = usageQuery.error || revenueQuery.error || performanceQuery.error

  const ticketMix = usage
    ? [
        { name: 'Confirmed', value: usage.confirmedTickets || 0 },
        { name: 'Cancelled', value: usage.cancelledTickets || 0 },
        { name: 'Expired', value: usage.expiredTickets || 0 },
        { name: 'Payment failed', value: usage.paymentFailedTickets || 0 },
      ].filter((item) => item.value > 0)
    : []

  const paymentMethods = (revenue?.paymentMethods || []).map((method) => ({
    name: method.paymentMethod || method.method,
    attempts: method.totalAttempts || method.attempts || 0,
    revenue: Number(method.netRevenue || method.amount || 0),
  }))

  async function handleGenerate() {
    const result = await confirmAction({
      title: 'Generate a service report?',
      text: `A permanent snapshot for ${formatDate(range.from)} to ${formatDate(range.to)} will be saved.`,
      confirmText: 'Generate report',
      icon: 'question',
    })
    if (result.isConfirmed) generateReport.mutate()
  }

  function refreshAll() {
    usageQuery.refetch()
    revenueQuery.refetch()
    performanceQuery.refetch()
    reportsQuery.refetch()
  }

  return (
    <div className="page-stack">
      <SectionHeader
        eyebrow="Performance intelligence"
        title="Service analytics"
        description="Trusted metrics aggregated securely from ticketing, payments, vehicles and schedules."
        actions={
          <>
            <button className="button button--ghost" onClick={refreshAll}>
              <FiRefreshCw /> Refresh
            </button>
            <button
              className="button button--primary"
              onClick={handleGenerate}
              disabled={loading || generateReport.isPending}
            >
              {generateReport.isPending ? <span className="button-spinner" /> : <FiFileText />}
              Generate report
            </button>
          </>
        }
      />

      <section className="analytics-filter">
        <div>
          <FiCalendar />
          <span>Reporting period</span>
        </div>
        <label>
          <span>From</span>
          <input
            type="date"
            value={draftRange.from}
            onChange={(event) =>
              setDraftRange((current) => ({ ...current, from: event.target.value }))
            }
          />
        </label>
        <span className="analytics-filter__divider">to</span>
        <label>
          <span>To</span>
          <input
            type="date"
            value={draftRange.to}
            onChange={(event) =>
              setDraftRange((current) => ({ ...current, to: event.target.value }))
            }
          />
        </label>
        <button
          className="button button--secondary"
          onClick={() => {
            if (!draftRange.from || !draftRange.to || draftRange.from > draftRange.to) {
              toast.error('Choose a valid reporting date range')
              return
            }
            setRange(draftRange)
          }}
        >
          Apply period
        </button>
      </section>

      {loading ? (
        <PageLoader label="Aggregating service performance" />
      ) : firstError ? (
        <ErrorState error={firstError} onRetry={refreshAll} />
      ) : (
        <>
          <section className="stat-grid stat-grid--four">
            <StatCard
              label="Passengers"
              value={formatNumber(usage?.totalPassengers)}
              helper={`${formatNumber(usage?.confirmedTickets)} confirmed tickets`}
              icon={FiUsers}
              tone="blue"
            />
            <StatCard
              label="Net revenue"
              value={formatCurrency(revenue?.netRevenue)}
              helper={`${formatNumber(revenue?.successfulPayments)} successful payments`}
              icon={FiDollarSign}
              tone="teal"
            />
            <StatCard
              label="On-time performance"
              value={`${Number(performance?.onTimePerformance || 0).toFixed(2)}%`}
              helper={`${formatNumber(performance?.onTimeAssignments)} on-time operations`}
              icon={FiCheckCircle}
              tone="amber"
            />
            <StatCard
              label="Assignments"
              value={formatNumber(performance?.totalAssignments)}
              helper={`${formatNumber(performance?.completedAssignments)} completed`}
              icon={FiBarChart2}
              tone="violet"
            />
          </section>

          <section className="analytics-grid">
            <article className="panel chart-panel chart-panel--wide">
              <div className="panel__header panel__header--bordered">
                <div>
                  <span className="eyebrow">Route demand</span>
                  <h3>Passenger usage by route</h3>
                </div>
              </div>
              {usage?.routeUsage?.length ? (
                <div className="chart-wrap">
                  <ResponsiveContainer width="100%" height={300}>
                    <BarChart data={usage.routeUsage} margin={{ left: 4, right: 12, top: 16 }}>
                      <CartesianGrid strokeDasharray="4 4" vertical={false} stroke="#e2e8f0" />
                      <XAxis dataKey="routeName" tickLine={false} axisLine={false} />
                      <YAxis allowDecimals={false} tickLine={false} axisLine={false} />
                      <Tooltip
                        cursor={{ fill: '#f1f5f9' }}
                        contentStyle={{ borderRadius: 12, border: '1px solid #e2e8f0' }}
                      />
                      <Bar dataKey="confirmedTickets" name="Tickets" fill="#0d9488" radius={[8, 8, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              ) : (
                <EmptyState
                  icon={FiBarChart2}
                  title="No route usage in this period"
                  description="Confirmed passenger activity will populate this chart."
                />
              )}
            </article>

            <article className="panel chart-panel">
              <div className="panel__header panel__header--bordered">
                <div>
                  <span className="eyebrow">Ticket health</span>
                  <h3>Status distribution</h3>
                </div>
              </div>
              {ticketMix.length ? (
                <>
                  <div className="chart-wrap chart-wrap--pie">
                    <ResponsiveContainer width="100%" height={220}>
                      <PieChart>
                        <Pie
                          data={ticketMix}
                          dataKey="value"
                          nameKey="name"
                          innerRadius={60}
                          outerRadius={88}
                          paddingAngle={4}
                        >
                          {ticketMix.map((item, index) => (
                            <Cell key={item.name} fill={colors[index % colors.length]} />
                          ))}
                        </Pie>
                        <Tooltip />
                      </PieChart>
                    </ResponsiveContainer>
                  </div>
                  <div className="chart-legend">
                    {ticketMix.map((item, index) => (
                      <span key={item.name}>
                        <i style={{ background: colors[index % colors.length] }} />
                        {item.name}
                        <strong>{item.value}</strong>
                      </span>
                    ))}
                  </div>
                </>
              ) : (
                <EmptyState
                  title="No ticket status data"
                  description="Ticket distribution will appear after passenger activity."
                />
              )}
            </article>

            <article className="panel chart-panel chart-panel--wide">
              <div className="panel__header panel__header--bordered">
                <div>
                  <span className="eyebrow">Collection channels</span>
                  <h3>Payment method performance</h3>
                </div>
              </div>
              {paymentMethods.length ? (
                <div className="chart-wrap">
                  <ResponsiveContainer width="100%" height={280}>
                    <BarChart data={paymentMethods} layout="vertical" margin={{ left: 18, right: 24 }}>
                      <CartesianGrid strokeDasharray="4 4" horizontal={false} stroke="#e2e8f0" />
                      <XAxis type="number" tickLine={false} axisLine={false} />
                      <YAxis type="category" dataKey="name" tickLine={false} axisLine={false} />
                      <Tooltip />
                      <Bar dataKey="attempts" name="Attempts" fill="#2563eb" radius={[0, 8, 8, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              ) : (
                <EmptyState
                  icon={FiDollarSign}
                  title="No payment activity"
                  description="Payment method performance will appear here."
                />
              )}
            </article>

            <article className="panel performance-panel">
              <span className="eyebrow">Operational SLA</span>
              <h3>On-time completion</h3>
              <div
                className="performance-ring"
                style={{
                  '--progress': `${Math.min(Number(performance?.onTimePerformance || 0), 100) * 3.6}deg`,
                }}
              >
                <div>
                  <strong>{Number(performance?.onTimePerformance || 0).toFixed(1)}%</strong>
                  <span>on time</span>
                </div>
              </div>
              <dl>
                <div><dt>Grace window</dt><dd>{performance?.graceMinutes || 15} min</dd></div>
                <div><dt>Completed</dt><dd>{formatNumber(performance?.completedAssignments)}</dd></div>
                <div><dt>Cancelled</dt><dd>{formatNumber(performance?.cancelledAssignments)}</dd></div>
              </dl>
            </article>
          </section>
        </>
      )}

      <section className="panel">
        <div className="panel__header panel__header--bordered">
          <div>
            <span className="eyebrow">Audit snapshots</span>
            <h3>Generated service reports</h3>
          </div>
          <span className="record-count">{reports.length} reports</span>
        </div>
        {reportsQuery.isLoading ? (
          <div className="table-skeleton" />
        ) : reportsQuery.isError ? (
          <ErrorState error={reportsQuery.error} onRetry={reportsQuery.refetch} compact />
        ) : reports.length ? (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Report</th>
                  <th>Period</th>
                  <th>Passengers</th>
                  <th>Revenue</th>
                  <th>On-time</th>
                  <th>Generated</th>
                </tr>
              </thead>
              <tbody>
                {reports.map((report) => (
                  <tr key={report.reportId}>
                    <td><strong>Service report #{report.reportId}</strong><small>By user #{report.generatedBy}</small></td>
                    <td>{formatDate(report.periodStart)} – {formatDate(report.periodEnd)}</td>
                    <td>{formatNumber(report.totalPassengers)}</td>
                    <td><strong>{formatCurrency(report.totalRevenue)}</strong></td>
                    <td>{Number(report.onTimePerformance || 0).toFixed(2)}%</td>
                    <td>{formatDate(report.generatedAt, 'dd MMM yyyy, hh:mm a')}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState
            icon={FiFileText}
            title="No saved service reports"
            description="Generate a report to preserve the current period’s metrics."
          />
        )}
      </section>
    </div>
  )
}
