import { useEffect, useMemo, useState } from 'react'
import type { CSSProperties } from 'react'
import axios from 'axios'
import { History, RefreshCcw, X } from 'lucide-react'
import PageLayout from '../../../layouts/PageLayout'
import { fetchAdminUserActivityHistory, fetchAdminUserActivitySummaries } from '../api/activityApi'
import type {
  AdminActivityHistoryItem,
  AdminActivityHistoryPageResponse,
  AdminUserActivitySummary,
} from '../types/activityTypes'
import type { ApiErrorResponse } from '../../../types/api'
import './UserActivityPage.css'

const historyPageSize = 10
const userSummaryPageSize = 10

function formatName(user: AdminUserActivitySummary) {
  const fullName = `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim()
  return fullName || user.email
}

function formatDateTime(value: string | null) {
  if (!value) {
    return 'No recorded activity'
  }
  return new Date(value).toLocaleString()
}

function ratioBarStyle(successRatioPercentage: number): CSSProperties {
  return {
    '--success-width': `${successRatioPercentage}%`,
  } as CSSProperties
}

function UserActivityPage() {
  const [users, setUsers] = useState<AdminUserActivitySummary[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const [selectedUser, setSelectedUser] = useState<AdminUserActivitySummary | null>(null)
  const [summaryPage, setSummaryPage] = useState(0)
  const [historyPage, setHistoryPage] = useState(0)
  const [historyData, setHistoryData] = useState<AdminActivityHistoryPageResponse | null>(null)
  const [isHistoryLoading, setIsHistoryLoading] = useState(false)
  const [historyErrorMessage, setHistoryErrorMessage] = useState('')

  const sortedUsers = useMemo(
    () =>
      [...users].sort((left, right) => {
        if (right.successRatioPercentage !== left.successRatioPercentage) {
          return right.successRatioPercentage - left.successRatioPercentage
        }
        return formatName(left).localeCompare(formatName(right))
      }),
    [users],
  )

  const summaryTotalPages = Math.max(1, Math.ceil(sortedUsers.length / userSummaryPageSize))
  const paginatedUsers = useMemo(() => {
    const startIndex = summaryPage * userSummaryPageSize
    return sortedUsers.slice(startIndex, startIndex + userSummaryPageSize)
  }, [sortedUsers, summaryPage])

  const loadUsers = async () => {
    setIsLoading(true)
    setErrorMessage('')

    try {
      const response = await fetchAdminUserActivitySummaries()
      setUsers(response)
    } catch (error) {
      if (axios.isAxiosError<ApiErrorResponse>(error)) {
        setErrorMessage(error.response?.data?.message ?? 'Unable to load user activity.')
      } else {
        setErrorMessage('Unable to load user activity.')
      }
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    void loadUsers()
  }, [])

  useEffect(() => {
    setSummaryPage(0)
  }, [users.length])

  useEffect(() => {
    setSummaryPage((current) => Math.min(current, Math.max(summaryTotalPages - 1, 0)))
  }, [summaryTotalPages])

  useEffect(() => {
    if (!selectedUser) {
      return
    }

    let active = true
    setIsHistoryLoading(true)
    setHistoryErrorMessage('')

    const loadHistory = async () => {
      try {
        const response = await fetchAdminUserActivityHistory(
          selectedUser.userId,
          historyPage,
          historyPageSize,
        )
        if (active) {
          setHistoryData(response)
        }
      } catch (error) {
        if (!active) {
          return
        }
        if (axios.isAxiosError<ApiErrorResponse>(error)) {
          setHistoryErrorMessage(error.response?.data?.message ?? 'Unable to load activity history.')
        } else {
          setHistoryErrorMessage('Unable to load activity history.')
        }
      } finally {
        if (active) {
          setIsHistoryLoading(false)
        }
      }
    }

    void loadHistory()
    return () => {
      active = false
    }
  }, [historyPage, selectedUser])

  const openUserHistory = (user: AdminUserActivitySummary) => {
    setSelectedUser(user)
    setHistoryPage(0)
    setHistoryData(null)
    setHistoryErrorMessage('')
  }

  const historyItems: AdminActivityHistoryItem[] = historyData?.content ?? []

  return (
    <PageLayout
      eyebrow="Admin"
      title="User Activity"
      description="Review recent user actions, last activity time, and booking follow-through ratios."
    >
      <section className="user-activity-page">
        <div className="user-activity-panel app-card">
          <div className="user-activity-panel__header">
            <div>
              <h2>User Activity History</h2>
              <p className="user-activity-muted">Admin-only view of user activity and booking dedication.</p>
            </div>
            <button className="app-button-secondary" type="button" onClick={() => void loadUsers()}>
              <RefreshCcw size={16} /> Refresh
            </button>
          </div>

          {errorMessage ? <p className="user-activity-feedback user-activity-feedback--error">{errorMessage}</p> : null}
          {isLoading ? <p className="user-activity-feedback">Loading user activity...</p> : null}

          {!isLoading ? (
            <>
              <div className="user-activity-table-wrap app-table-wrap">
                <table className="user-activity-table app-table">
                  <thead>
                    <tr>
                      <th>User</th>
                      <th>Role</th>
                      <th>Last Activity</th>
                      <th>Last Action</th>
                      <th>Booking Success Ratio</th>
                    </tr>
                  </thead>
                  <tbody>
                    {paginatedUsers.map((user) => (
                      <tr key={user.userId} className="user-activity-row">
                        <td>
                          <button
                            className="user-activity-row-button"
                            type="button"
                            onClick={() => openUserHistory(user)}
                          >
                            <span className="user-activity-user">
                              <strong>{formatName(user)}</strong>
                              <span>{user.email}</span>
                            </span>
                          </button>
                        </td>
                        <td>{user.role}</td>
                        <td>{formatDateTime(user.lastActivityAt)}</td>
                        <td>{user.lastActivityAction ?? 'No recorded activity'}</td>
                        <td>
                          <div className="user-activity-ratio">
                            <div
                              className="user-activity-ratio__bar"
                              style={ratioBarStyle(user.successRatioPercentage)}
                              aria-label={`Success ratio ${user.successRatioPercentage}%`}
                            />
                            <div className="user-activity-ratio__meta">
                              <span>{user.successRatioPercentage}% success</span>
                              <span>
                                {user.successfulBookings} success / {user.failedBookings} failed
                              </span>
                            </div>
                          </div>
                        </td>
                      </tr>
                    ))}
                    {sortedUsers.length === 0 ? (
                      <tr>
                        <td colSpan={5}>
                          <p className="user-activity-feedback">No users found.</p>
                        </td>
                      </tr>
                    ) : null}
                  </tbody>
                </table>
              </div>
              <div className="user-activity-list-pagination" aria-label="User activity pagination">
                <span className="user-activity-muted">
                  {sortedUsers.length} users | Page {summaryPage + 1} of {summaryTotalPages}
                </span>
                <div className="user-activity-modal__pager">
                  <button
                    className="app-button-secondary"
                    type="button"
                    disabled={summaryPage === 0}
                    onClick={() => setSummaryPage((current) => Math.max(0, current - 1))}
                  >
                    Previous
                  </button>
                  <button
                    className="app-button-secondary"
                    type="button"
                    disabled={summaryPage >= summaryTotalPages - 1}
                    onClick={() => setSummaryPage((current) => current + 1)}
                  >
                    Next
                  </button>
                </div>
              </div>
            </>
          ) : null}
        </div>
      </section>

      {selectedUser ? (
        <div className="app-modal-backdrop" role="presentation" onClick={() => setSelectedUser(null)}>
          <section
            className="user-activity-modal app-modal"
            aria-label="User activity history"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="user-activity-modal__header">
              <div>
                <span className="app-eyebrow">User Activity</span>
                <h2>{formatName(selectedUser)}</h2>
                <p className="user-activity-muted">{selectedUser.email}</p>
              </div>
              <button
                className="app-close-button"
                type="button"
                aria-label="Close activity history"
                onClick={() => setSelectedUser(null)}
              >
                <X size={18} />
              </button>
            </div>

            <div className="user-activity-modal__summary">
              <strong>Booking ratio</strong>
              <div className="user-activity-ratio">
                <div
                  className="user-activity-ratio__bar"
                  style={ratioBarStyle(selectedUser.successRatioPercentage)}
                />
                <div className="user-activity-ratio__meta">
                  <span>{selectedUser.successRatioPercentage}% success</span>
                  <span>
                    {selectedUser.successfulBookings} success / {selectedUser.failedBookings} failed / {selectedUser.totalBookings} total
                  </span>
                </div>
              </div>
            </div>

            <div className="user-activity-modal__history">
              <div className="user-activity-history-item__top">
                <strong><History size={16} /> Activity History</strong>
                <span className="user-activity-muted">Page {historyData ? historyData.page + 1 : historyPage + 1}</span>
              </div>

              {historyErrorMessage ? <p className="user-activity-feedback user-activity-feedback--error">{historyErrorMessage}</p> : null}
              {isHistoryLoading ? <p className="user-activity-feedback">Loading activity history...</p> : null}

              {!isHistoryLoading && historyItems.length === 0 ? (
                <p className="user-activity-feedback">No activity records found for this user.</p>
              ) : null}

              {!isHistoryLoading
                ? historyItems.map((item) => (
                    <article key={item.id} className="user-activity-history-item">
                      <div className="user-activity-history-item__top">
                        <strong>{item.action}</strong>
                        <span
                          className={`user-activity-history-item__status ${
                            item.status === 'SUCCESS'
                              ? 'user-activity-history-item__status--success'
                              : 'user-activity-history-item__status--fail'
                          }`}
                        >
                          {item.status}
                        </span>
                      </div>
                      <span className="user-activity-muted">{new Date(item.createdAt).toLocaleString()}</span>
                    </article>
                  ))
                : null}
            </div>

            <div className="user-activity-modal__actions">
              <span className="user-activity-muted">
                {historyData ? `${historyData.totalElements} records` : '0 records'}
              </span>
              <div className="user-activity-modal__pager">
                <button
                  className="app-button-secondary"
                  type="button"
                  disabled={historyPage === 0 || isHistoryLoading}
                  onClick={() => setHistoryPage((current) => Math.max(0, current - 1))}
                >
                  Previous
                </button>
                <button
                  className="app-button-secondary"
                  type="button"
                  disabled={
                    isHistoryLoading ||
                    !historyData ||
                    historyData.totalPages === 0 ||
                    historyPage >= historyData.totalPages - 1
                  }
                  onClick={() => setHistoryPage((current) => current + 1)}
                >
                  Next
                </button>
              </div>
            </div>
          </section>
        </div>
      ) : null}
    </PageLayout>
  )
}

export default UserActivityPage
