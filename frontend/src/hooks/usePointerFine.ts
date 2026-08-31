import { useEffect, useState } from 'react'

/**
 * True on a device with a real pointer. Tilting a card under a finger feels wrong,
 * so the 3D effect is reserved for mice and trackpads.
 */
export function usePointerFine(): boolean {
  const [fine, setFine] = useState(false)

  useEffect(() => {
    const query = window.matchMedia('(hover: hover) and (pointer: fine)')
    const update = () => setFine(query.matches)
    update()
    query.addEventListener('change', update)
    return () => query.removeEventListener('change', update)
  }, [])

  return fine
}
