interface Props {
  enabled: boolean
  onToggle: () => void
}

/**
 * The mute switch, next to the animation one. A card game played in an open office, or
 * next to a sleeping flatmate, has to be silenceable in one tap — and the choice sticks.
 */
export function SoundToggle({ enabled, onToggle }: Props) {
  return (
    <button
      type="button"
      onClick={onToggle}
      aria-pressed={enabled}
      title={enabled ? 'Sons activés' : 'Sons coupés'}
      aria-label={enabled ? 'Couper les sons' : 'Activer les sons'}
      className="sketch-pill flex size-9 shrink-0 items-center justify-center bg-paper text-ink/70 transition hover:bg-ink/8"
    >
      <svg
        viewBox="0 0 24 24"
        aria-hidden="true"
        className={`size-4 transition-opacity ${enabled ? '' : 'opacity-45'}`}
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <path d="M4 9.5h3.2L12 5.4v13.2L7.2 14.5H4z" />
        {enabled ? (
          <>
            <path d="M15.6 9.2a4 4 0 0 1 0 5.6" />
            <path d="M18.2 6.6a7.6 7.6 0 0 1 0 10.8" />
          </>
        ) : (
          <path d="M16 9.5l5 5m0-5l-5 5" stroke="var(--color-punch)" />
        )}
      </svg>
    </button>
  )
}
