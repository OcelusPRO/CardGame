import { useCallback, useEffect, useState } from 'react'

/**
 * A piece of state kept in the browser. Every access is guarded because a private
 * window, or a browser blocking site data, makes localStorage throw.
 */
export function useLocalStorage<T>(key: string, initial: T): [T, (value: T) => void] {
  const [value, setValue] = useState<T>(() => read(key, initial))

  useEffect(() => {
    try {
      window.localStorage.setItem(key, JSON.stringify(value))
    } catch {
      // Storage unavailable: the app keeps working, it just forgets.
    }
  }, [key, value])

  const update = useCallback((next: T) => setValue(next), [])

  return [value, update]
}

function read<T>(key: string, fallback: T): T {
  try {
    const raw = window.localStorage.getItem(key)
    return raw === null ? fallback : (JSON.parse(raw) as T)
  } catch {
    return fallback
  }
}
