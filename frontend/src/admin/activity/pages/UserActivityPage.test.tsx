import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'
import UserActivityPage from './UserActivityPage'

const fetchAdminUserActivitySummariesMock = vi.fn()
const fetchAdminUserActivityHistoryMock = vi.fn()

vi.mock('../api/activityApi', () => ({
  fetchAdminUserActivitySummaries: (...args: unknown[]) => fetchAdminUserActivitySummariesMock(...args),
  fetchAdminUserActivityHistory: (...args: unknown[]) => fetchAdminUserActivityHistoryMock(...args),
}))

describe('UserActivityPage', () => {
  beforeEach(() => {
    fetchAdminUserActivitySummariesMock.mockReset()
    fetchAdminUserActivityHistoryMock.mockReset()
  })

  it('supports pagination controls for user activity summaries', async () => {
    fetchAdminUserActivitySummariesMock.mockResolvedValue(
      Array.from({ length: 11 }, (_, index) => ({
        userId: index + 1,
        email: `testGenerated${index + 1}@generated.com`,
        firstName: `User${index + 1}`,
        lastName: 'Generated',
        role: 'PLAYER',
        lastActivityAt: '2026-05-27T09:00:00Z',
        lastActivityAction: 'LOGIN',
        successfulBookings: 2,
        failedBookings: 1,
        totalBookings: 3,
        successRatioPercentage: index === 10 ? 10 : 67,
      })),
    )
    fetchAdminUserActivityHistoryMock.mockResolvedValue({
      content: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    })

    render(
      <MemoryRouter>
        <UserActivityPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('User1 Generated')).toBeInTheDocument()
    expect(screen.queryByText('User11 Generated')).not.toBeInTheDocument()
    expect(screen.getByText(/11 users \| Page 1 of 2/i)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Next' }))

    await waitFor(() => {
      expect(screen.getByText('User11 Generated')).toBeInTheDocument()
    })
    expect(screen.getByText(/11 users \| Page 2 of 2/i)).toBeInTheDocument()
  })
})
