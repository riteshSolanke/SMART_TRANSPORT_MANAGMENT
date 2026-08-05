import { describe, expect, it } from 'vitest'
import { navigationForRole } from './navigation.jsx'

describe('role navigation', () => {
  it('gives conductors access to issued tickets and payments', () => {
    const paths = navigationForRole('CONDUCTOR').map((item) => item.path)

    expect(paths).toContain('/tickets')
    expect(paths).toContain('/payments')
    expect(paths).toContain('/routes')
  })
})
