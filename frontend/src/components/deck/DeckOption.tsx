interface Props {
  name: string
  detail: string
  official: boolean
  selected: boolean
  disabled: boolean
  onToggle: () => void
}

/**
 * One deck in the selector. Official decks carry a star so a player can tell at a glance
 * what comes from the site and what comes from their own browser.
 */
export function DeckOption({ name, detail, official, selected, disabled, onToggle }: Props) {
  return (
    <button
      type="button"
      disabled={disabled}
      aria-pressed={selected}
      onClick={onToggle}
      className={`sketch-alt flex min-w-40 flex-col px-4 py-2 text-left text-sm transition disabled:opacity-40 ${
        selected ? 'bg-mint text-ink' : 'bg-ink/5 hover:bg-ink/10'
      }`}
    >
      <span className="flex items-center gap-1.5 font-bold">
        <span aria-hidden>{official ? '⭐' : '💾'}</span>
        <span className="sr-only">{official ? 'Deck officiel :' : 'Votre deck :'}</span>
        {name}
      </span>
      <span className="text-xs opacity-70">{detail}</span>
    </button>
  )
}
