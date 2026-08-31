import { useEffect, useState } from 'react'
import { sessionApi } from '../api/session'
import type { MeView } from '../api/types'

/** Who the browser is, according to the server. Null while the answer is on its way. */
export function useSession() {
  const [me, setMe] = useState<MeView | null>(null)

  useEffect(() => {
    let cancelled = false
    sessionApi
      .me()
      .then((value) => {
        if (!cancelled) setMe(value)
      })
      .catch(() => setMe(null))
    return () => {
      cancelled = true
    }
  }, [])

  return { me }
}
