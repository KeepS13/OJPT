import { describe, expect, it } from 'vitest'
import router from '@/router'

describe('authenticated routes', () => {
  it('requires authentication for the user center routes', () => {
    const matched = router.resolve('/profile/security').matched

    expect(matched.some((route) => route.meta.requiresAuth)).toBe(true)
  })
})
