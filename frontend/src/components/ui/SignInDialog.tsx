import { useEffect, useId } from 'react'
import { motion } from 'motion/react'
import { rememberReturnPath } from '../../session/authReturn'

export interface SignInProvider {
  id: string
  label: string
  href: string
  colour: string
}

interface Props {
  providers: SignInProvider[]
  onClose: () => void
}

/**
 * One account, chosen from the ones the server actually offers. Signing in is a full page
 * redirect, so each choice is a plain link: the dialog only exists to put them side by
 * side, and to note where the player was before leaving.
 */
export function SignInDialog({ providers, onClose }: Props) {
  const titleId = useId()

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [onClose])

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <motion.div
        aria-hidden
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        onClick={onClose}
        className="absolute inset-0 bg-ink/55"
      />
      <motion.div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        initial={{ opacity: 0, y: 12, scale: 0.96 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        exit={{ opacity: 0, y: 12, scale: 0.96 }}
        transition={{ type: 'spring', stiffness: 420, damping: 30 }}
        className="sketch relative w-full max-w-sm bg-paper p-6 shadow-card"
      >
        <h2 id={titleId} className="font-display text-2xl font-black">
          Se connecter
        </h2>
        <p className="mt-1 text-sm text-ink/70">Choisissez comment vous connecter&nbsp;:</p>

        <div className="mt-5 flex flex-col gap-2">
          {providers.map((provider) => (
            <a
              key={provider.id}
              href={provider.href}
              onClick={() => rememberReturnPath()}
              className={`sketch-pill px-4 py-3 text-center font-bold text-white transition hover:brightness-110 ${provider.colour}`}
            >
              {provider.label}
            </a>
          ))}
        </div>

        <button
          type="button"
          onClick={onClose}
          className="mt-4 w-full rounded-lg px-3 py-2 text-sm font-semibold text-ink/70 transition hover:bg-ink/8"
        >
          Plus tard
        </button>
      </motion.div>
    </div>
  )
}
