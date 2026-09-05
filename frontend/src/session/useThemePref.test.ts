import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { useThemePref } from './useThemePref'

function mockPrefersDark(dark: boolean) {
  window.matchMedia = ((query: string) => ({
    matches: query.includes('prefers-color-scheme: dark') ? dark : false,
    media: query,
    onchange: null,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
    addListener: () => undefined,
    removeListener: () => undefined,
    dispatchEvent: () => false,
  })) as unknown as typeof window.matchMedia
}

describe('useThemePref', () => {
  const realMatchMedia = window.matchMedia

  beforeEach(() => {
    window.localStorage.clear()
  })

  afterEach(() => {
    window.matchMedia = realMatchMedia
  })

  it('follows a dark operating system out of the box', () => {
    mockPrefersDark(true)
    const { result } = renderHook(() => useThemePref())
    expect(result.current.dark).toBe(true)
  })

  it('stays light when the OS asks for light', () => {
    mockPrefersDark(false)
    const { result } = renderHook(() => useThemePref())
    expect(result.current.dark).toBe(false)
  })

  it('remembers a manual switch to dark against a light OS', () => {
    mockPrefersDark(false)
    const { result } = renderHook(() => useThemePref())

    act(() => result.current.toggle())

    expect(result.current.dark).toBe(true)
    expect(window.localStorage.getItem('sansfiltres:theme')).toContain('dark')
  })

  it('lets someone on a dark OS pull the site back into daylight', () => {
    mockPrefersDark(true)
    const { result } = renderHook(() => useThemePref())

    act(() => result.current.toggle())

    expect(result.current.dark).toBe(false)
    expect(window.localStorage.getItem('sansfiltres:theme')).toContain('light')
  })
})
