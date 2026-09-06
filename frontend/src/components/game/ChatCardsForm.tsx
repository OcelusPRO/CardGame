import type { ChatCardAccess, ChatCardsInput, ChatCardsView } from '../../api/types'

interface Props {
  settings: ChatCardsView
  disabled: boolean
  lockedBecause: string | null
  /** The host's channel, which is the one the viewers write from. */
  hostTwitchLogin: string
  onChange: (patch: ChatCardsInput) => void
}

const ACCESS: { value: ChatCardAccess; label: string }[] = [
  { value: 'OFF', label: 'Fermé' },
  { value: 'EVERYONE', label: 'Ouvert à tous' },
  { value: 'CHANNEL_POINTS', label: 'Points de chaîne' },
  { value: 'BITS', label: 'Bits' },
]

/**
 * Lets the host open their chat to writing, and set what it costs.
 *
 * The price itself lives on Twitch — a streamer prices their own reward, and their own
 * cheer — so there is nothing to type here but the floor: this table wants at least that
 * many bits. Everything below the first row disappears when the door is shut, because a
 * greyed out price for a closed door is just noise.
 */
export function ChatCardsForm({
  settings,
  disabled,
  lockedBecause,
  hostTwitchLogin,
  onChange,
}: Props) {
  const open = settings.access !== 'OFF'

  return (
    <div className="sketch flex flex-col gap-3 bg-[#9146FF]/10 px-4 py-3">
      <fieldset title={lockedBecause ?? undefined}>
        <legend className="mb-2 text-xs font-semibold uppercase tracking-wider text-ink/60">
          Le tchat de {hostTwitchLogin} écrit des cartes
        </legend>
        <div className="flex flex-wrap gap-2">
          {ACCESS.map((option) => (
            <button
              key={option.value}
              type="button"
              disabled={disabled}
              aria-pressed={settings.access === option.value}
              onClick={() => onChange({ access: option.value })}
              className={`sketch-pill px-4 py-2 text-sm font-semibold transition disabled:cursor-help disabled:opacity-40 ${
                settings.access === option.value ? 'bg-[#772ce8] text-white' : 'bg-ink/5 hover:bg-ink/10'
              }`}
            >
              {option.label}
            </button>
          ))}
        </div>
      </fieldset>

      {open && (
        <>
          <div className="flex flex-wrap gap-4">
            <Check
              label="Des situations"
              checked={settings.situations}
              disabled={disabled}
              lockedBecause={lockedBecause}
              onChange={(situations) => onChange({ situations })}
            />
            <Check
              label="Des réponses"
              checked={settings.punchlines}
              disabled={disabled}
              lockedBecause={lockedBecause}
              onChange={(punchlines) => onChange({ punchlines })}
            />
          </div>

          {settings.access === 'BITS' && (
            <label
              title={lockedBecause ?? undefined}
              className={`flex flex-col gap-1 ${lockedBecause ? 'cursor-help' : ''}`}
            >
              <span className="text-xs font-semibold uppercase tracking-wider text-ink/60">
                Bits minimum par carte
              </span>
              <input
                type="number"
                value={settings.minBits}
                min={1}
                max={100000}
                disabled={disabled}
                onChange={(event) => {
                  const next = Number(event.target.value)
                  if (next >= 1 && next <= 100000) onChange({ minBits: next })
                }}
                className="sketch-input w-36 bg-paper px-3 py-2 font-display text-lg tabular-nums outline-none focus:border-punch disabled:opacity-40"
              />
            </label>
          )}

          <p className="text-xs leading-relaxed text-ink/65">{explanation(settings)}</p>
        </>
      )}
    </div>
  )
}

function explanation(settings: ChatCardsView): string {
  const what =
    settings.situations && settings.punchlines
      ? 'des situations et des réponses'
      : settings.situations
        ? 'des situations'
        : 'des réponses'

  const commands =
    settings.situations && settings.punchlines
      ? '« !situation … » et « !réponse … »'
      : settings.situations
        ? '« !situation … »'
        : '« !réponse … »'

  const shortcuts =
    settings.situations && settings.punchlines
      ? ' Les raccourcis « !situ » et « !rep » font la même chose.'
      : settings.situations
        ? ' Le raccourci « !situ » fait la même chose.'
        : ' Le raccourci « !rep » fait la même chose.'

  if (settings.access === 'BITS') {
    return `Vos spectateurs écrivent ${what} depuis le panneau de l'extension, à partir de ${settings.minBits} bits la carte. Les cartes rejoignent le paquet mélangées, et disparaissent avec la partie.`
  }
  if (settings.access === 'CHANNEL_POINTS') {
    return `Vos spectateurs écrivent ${what} en échangeant une récompense de points de chaîne qui demande un message, puis en tapant ${commands} dans le tchat.${shortcuts} C'est vous qui en fixez le prix, sur votre chaîne.`
  }
  return `N'importe qui dans votre tchat peut écrire ${what} avec ${commands}.${shortcuts} Une carte par personne toutes les quelques secondes, et 200 au maximum pour toute la partie.`
}

interface CheckProps {
  label: string
  checked: boolean
  disabled: boolean
  lockedBecause: string | null
  onChange: (checked: boolean) => void
}

function Check({ label, checked, disabled, lockedBecause, onChange }: CheckProps) {
  return (
    <label
      title={lockedBecause ?? undefined}
      className={`flex items-center gap-2 text-sm font-semibold ${
        lockedBecause ? 'cursor-help opacity-40' : ''
      }`}
    >
      <input
        type="checkbox"
        checked={checked}
        disabled={disabled}
        onChange={(event) => onChange(event.target.checked)}
        className="size-5 accent-punch"
      />
      {label}
    </label>
  )
}
