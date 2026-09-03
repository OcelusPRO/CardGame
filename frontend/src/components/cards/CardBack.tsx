import logo from '../../assets/logo.png'

/**
 * The face a card wears while it is still on its way to the hand — the back of the deck,
 * carrying the site's mark. Sized to fill whatever box it is dropped into.
 */
export function CardBack({ className = '' }: { className?: string }) {
  return (
    <div
      aria-hidden="true"
      className={`sketch relative flex h-full w-full items-center justify-center overflow-hidden bg-ink-soft p-5 shadow-card [--stroke:var(--color-paper)] ${className}`}
    >
      <span className="absolute inset-0 opacity-15 [background:radial-gradient(circle_at_50%_38%,var(--color-punch),transparent_62%)]" />
      <img
        src={logo}
        alt=""
        className="relative w-3/5 max-w-28 drop-shadow-[0_8px_20px_rgba(0,0,0,0.4)]"
      />
    </div>
  )
}
