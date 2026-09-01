import { AnimatePresence, motion } from 'motion/react'
import { useEffect } from 'react'

interface Props {
  code: string | null
  message: string
  onDismiss: () => void
}

/** A short lived banner for a refused action, dismissed on its own after a few seconds. */
export function Toast({ code, message, onDismiss }: Props) {
  useEffect(() => {
    if (!code) return
    const timer = setTimeout(onDismiss, 4000)
    return () => clearTimeout(timer)
  }, [code, onDismiss])

  return (
    <AnimatePresence>
      {code && (
        <motion.div
          role="status"
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: 30 }}
          className="sketch fixed inset-x-4 bottom-4 z-50 mx-auto max-w-md [--stroke:var(--color-punch)] bg-ink-soft px-5 py-4 text-center font-semibold text-paper shadow-card"
        >
          {message}
        </motion.div>
      )}
    </AnimatePresence>
  )
}
