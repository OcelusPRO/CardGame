import { useEffect, useState } from 'react'
import type { LiveStatsView } from '../../api/adminTypes'

/** Subscribes to the administration socket, which pushes counters every two seconds. */
export function useLiveStats(): LiveStatsView | null {
  const [stats, setStats] = useState<LiveStatsView | null>(null)

  useEffect(() => {
    const url = new URL(window.location.origin)
    url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
    url.pathname = '/ws/admin/stats'
    const socket = new WebSocket(url.toString())

    socket.onmessage = (event) => {
      try {
        setStats(JSON.parse(String(event.data)) as LiveStatsView)
      } catch {
        // A malformed frame simply keeps the previous numbers on screen.
      }
    }

    return () => socket.close()
  }, [])

  return stats
}
