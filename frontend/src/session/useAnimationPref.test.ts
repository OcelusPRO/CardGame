import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { useAnimationPref } from './useAnimationPref'

function mockPrefersReducedMotion(reduced: boolean) {
  window.matchMedia = ((query: string) => ({
    matches: query.includes('prefers-reduced-motion') ? reduced : false,
    media: query,
    onchange: null,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
    addListener: () => undefined,
    removeListener: () => undefined,
    dispatchEvent: () => false,
  })) as unknown as typeof window.matchMedia
}

describe('useAnimationPref', () => {
  const realMatchMedia = window.matchMedia

  beforeEach(() => {
    window.localStorage.clear()
  })

  afterEach(() => {
    window.matchMedia = realMatchMedia
  })

  it('follows the OS setting until told otherwise', () => {
    mockPrefersReducedMotion(true)
    const { result } = renderHook(() => useAnimationPref())
    expect(result.current.enabled).toBe(false)
  })

  it('animates by default when the OS has no objection', () => {
    mockPrefersReducedMotion(false)
    const { result } = renderHook(() => useAnimationPref())
    expect(result.current.enabled).toBe(true)
  })

  it('remembers a manual switch-off even against the OS', () => {
    mockPrefersReducedMotion(false)
    const { result } = renderHook(() => useAnimationPref())

    act(() => result.current.toggle())

    expect(result.current.enabled).toBe(false)
    expect(window.localStorage.getItem('sansfiltres:animations')).toContain('off')
  })

  it('lets a reduced-motion user opt back into animations', () => {
    mockPrefersReducedMotion(true)
    const { result } = renderHook(() => useAnimationPref())

    act(() => result.current.toggle())

    expect(result.current.enabled).toBe(true)
  })
})
