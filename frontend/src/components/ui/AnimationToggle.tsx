interface Props {
  enabled: boolean
  onToggle: () => void
}

/**
 * A header switch for anyone the movement bothers: one tap silences every animation on
 * the site, OS setting or not, and the choice is remembered.
 */
export function AnimationToggle({ enabled, onToggle }: Props) {
  return (
    <button
      type="button"
      onClick={onToggle}
      aria-pressed={enabled}
      title={enabled ? 'Animations activées' : 'Animations désactivées'}
      aria-label={enabled ? 'Désactiver les animations' : 'Activer les animations'}
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
        <path d="M12 3.2l1.9 4.7 4.7 1.9-4.7 1.9L12 16.4l-1.9-4.7L5.4 9.8l4.7-1.9z" />
        <path d="M18.5 14.5l.9 2.1 2.1.9-2.1.9-.9 2.1-.9-2.1-2.1-.9 2.1-.9z" />
        {!enabled && <line x1="3" y1="21" x2="21" y2="3" stroke="var(--color-punch)" />}
      </svg>
    </button>
  )
}
