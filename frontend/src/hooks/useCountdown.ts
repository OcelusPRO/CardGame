import { useEffect, useState } from 'react'

/**
 * Seconds left before [deadlineMillis], corrected by the gap between the server clock
 * and the browser one, so a phone with a wrong time still shows the right countdown.
 */
export function useCountdown(deadlineMillis: number | undefined, serverTimeMillis: number): number | null {
  const [remaining, setRemaining] = useState<number | null>(null)

  useEffect(() => {
    if (!deadlineMillis) {
      setRemaining(null)
      return
    }
    const offset = serverTimeMillis - Date.now()
    const tick = () => setRemaining(Math.max(0, Math.round((deadlineMillis - (Date.now() + offset)) / 1000)))
    tick()
    const timer = setInterval(tick, 250)
    return () => clearInterval(timer)
  }, [deadlineMillis, serverTimeMillis])

  return remaining
}
