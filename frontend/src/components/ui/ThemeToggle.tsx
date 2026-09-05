interface Props {
  dark: boolean
  onToggle: () => void
}

/**
 * The light switch, third in the row. It shows where one tap would take you — a moon
 * while the room is lit, a sun once it is dark — because a switch that pictures the
 * state you are already in has nothing to offer.
 */
export function ThemeToggle({ dark, onToggle }: Props) {
  return (
    <button
      type="button"
      onClick={onToggle}
      aria-pressed={dark}
      title={dark ? 'Thème sombre' : 'Thème clair'}
      aria-label={dark ? 'Passer au thème clair' : 'Passer au thème sombre'}
      className="sketch-pill flex size-9 shrink-0 items-center justify-center bg-paper text-ink/70 transition hover:bg-ink/8"
    >
      <svg
        viewBox="0 0 24 24"
        aria-hidden="true"
        className="size-4"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        {dark ? (
          <>
            <circle cx="12" cy="12" r="4.2" />
            <path d="M12 2.6v2.2M12 19.2v2.2M4.2 12H2M22 12h-2.2M6.3 6.3 4.8 4.8M19.2 19.2l-1.5-1.5M17.7 6.3l1.5-1.5M4.8 19.2l1.5-1.5" />
          </>
        ) : (
          <path d="M20.4 14.2A8.4 8.4 0 0 1 9.8 3.6a8.4 8.4 0 1 0 10.6 10.6z" />
        )}
      </svg>
    </button>
  )
}
