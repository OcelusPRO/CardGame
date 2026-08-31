import type { GameSettingsInput, GameSettingsView } from '../../api/types'

interface Props {
  settings: GameSettingsView
  disabled: boolean
  onChange: (patch: GameSettingsInput) => void
}

const NOT_HOST = "Seul l'hôte peut changer les règles."
const NO_CARDS = 'En mode sans limites, personne ne reçoit de cartes.'
const NO_VOTE = 'Le maître du jeu tranche seul : personne ne vote.'
const VOTE_MODE = 'Réservé au mode maître du jeu tournant.'

/** The host control panel of the lobby. Every change is pushed live to the table. */
export function SettingsForm({ settings, disabled, onChange }: Props) {
  const freeText = settings.answerMode === 'FREE_TEXT'
  const czar = settings.selectionMode === 'CZAR'

  /** Why a field is greyed out, shown on hover. Null means the field is usable. */
  const lockedBecause = (ownReason: string | null): string | null =>
    disabled ? NOT_HOST : ownReason

  return (
    <div className="flex flex-col gap-4">
      <Choice
        label="Qui désigne la meilleure réponse ?"
        value={settings.selectionMode}
        lockedBecause={lockedBecause(null)}
        options={[
          { value: 'VOTE', label: 'Tout le monde vote' },
          { value: 'CZAR', label: 'Maître du jeu tournant' },
        ]}
        onSelect={(selectionMode) => onChange({ selectionMode: selectionMode as 'VOTE' | 'CZAR' })}
      />

      <Choice
        label="D'où viennent les réponses ?"
        value={settings.answerMode}
        lockedBecause={lockedBecause(null)}
        options={[
          { value: 'CARDS', label: 'Cartes distribuées' },
          { value: 'FREE_TEXT', label: 'Sans limites (on écrit)' },
        ]}
        onSelect={(answerMode) => onChange({ answerMode: answerMode as 'CARDS' | 'FREE_TEXT' })}
      />

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-4">
        <NumberBox
          label="Nombre de manches"
          value={settings.rounds}
          min={1}
          max={50}
          lockedBecause={lockedBecause(null)}
          onChange={(rounds) => onChange({ rounds })}
        />
        <NumberBox
          label="Cartes en main"
          value={settings.handSize}
          min={4}
          max={15}
          lockedBecause={lockedBecause(freeText ? NO_CARDS : null)}
          onChange={(handSize) => onChange({ handSize })}
        />
        <NumberBox
          label="Temps de réponse"
          value={settings.submitSeconds}
          min={15}
          max={300}
          lockedBecause={lockedBecause(null)}
          onChange={(submitSeconds) => onChange({ submitSeconds })}
        />
        <NumberBox
          label="Temps de vote"
          value={settings.selectSeconds}
          min={15}
          max={300}
          lockedBecause={lockedBecause(null)}
          onChange={(selectSeconds) => onChange({ selectSeconds })}
        />
        <NumberBox
          label="Points par vote"
          value={settings.pointsPerVote}
          min={1}
          max={20}
          lockedBecause={lockedBecause(null)}
          onChange={(pointsPerVote) => onChange({ pointsPerVote })}
        />
        <NumberBox
          label="Bonus unanimité"
          value={settings.unanimityBonus}
          min={0}
          max={20}
          lockedBecause={lockedBecause(czar ? NO_VOTE : null)}
          onChange={(unanimityBonus) => onChange({ unanimityBonus })}
        />
        <NumberBox
          label="Joueurs minimum"
          value={settings.minPlayers}
          min={2}
          max={settings.maxPlayers}
          lockedBecause={lockedBecause(null)}
          onChange={(minPlayers) => onChange({ minPlayers })}
        />
      </div>

      <Toggle
        label="Autoriser à voter pour sa propre carte"
        checked={settings.allowSelfVote}
        lockedBecause={lockedBecause(czar ? NO_VOTE : null)}
        onChange={(allowSelfVote) => onChange({ allowSelfVote })}
      />

      <Toggle
        label="Le maître du jeu répond aussi"
        checked={settings.czarAnswers}
        lockedBecause={lockedBecause(czar ? null : VOTE_MODE)}
        onChange={(czarAnswers) => onChange({ czarAnswers })}
      />

      <p className="rounded-2xl bg-white/5 px-4 py-3 text-xs leading-relaxed text-white/55">
        {czar
          ? `Le maître du jeu choisit, et la réponse retenue rapporte ${settings.pointsPerVote} point(s). ${settings.rounds} manches, et le meilleur score l'emporte.`
          : `Chaque vote reçu rapporte ${settings.pointsPerVote} point(s). Une réponse choisie par tous ceux qui pouvaient la choisir gagne ${settings.unanimityBonus} point(s) de plus ; un seul vote ailleurs et le bonus tombe à zéro. ${settings.rounds} manches, et le meilleur score l'emporte.`}
      </p>
    </div>
  )
}

interface ChoiceProps {
  label: string
  value: string
  lockedBecause: string | null
  options: { value: string; label: string }[]
  onSelect: (value: string) => void
}

function Choice({ label, value, lockedBecause, options, onSelect }: ChoiceProps) {
  return (
    <fieldset title={lockedBecause ?? undefined}>
      <legend className="mb-2 text-xs font-semibold uppercase tracking-wider text-white/50">{label}</legend>
      <div className="flex flex-wrap gap-2">
        {options.map((option) => (
          <button
            key={option.value}
            type="button"
            disabled={Boolean(lockedBecause)}
            aria-pressed={value === option.value}
            onClick={() => onSelect(option.value)}
            className={`rounded-full px-4 py-2 text-sm font-semibold transition disabled:cursor-help disabled:opacity-40 ${
              value === option.value ? 'bg-grape text-white' : 'bg-white/10 hover:bg-white/20'
            }`}
          >
            {option.label}
          </button>
        ))}
      </div>
    </fieldset>
  )
}

interface ToggleProps {
  label: string
  checked: boolean
  lockedBecause: string | null
  onChange: (checked: boolean) => void
}

function Toggle({ label, checked, lockedBecause, onChange }: ToggleProps) {
  return (
    <label
      title={lockedBecause ?? undefined}
      className={`flex items-center gap-3 text-sm font-semibold ${
        lockedBecause ? 'cursor-help opacity-40' : ''
      }`}
    >
      <input
        type="checkbox"
        checked={checked}
        disabled={Boolean(lockedBecause)}
        onChange={(event) => onChange(event.target.checked)}
        className="size-5 accent-punch"
      />
      {label}
    </label>
  )
}

interface NumberBoxProps {
  label: string
  value: number
  min: number
  max: number
  lockedBecause: string | null
  onChange: (value: number) => void
}

/**
 * Label on top, input pinned to the bottom. Labels wrap onto two lines at some widths,
 * and without that the inputs of a row would sit at different heights.
 */
function NumberBox({ label, value, min, max, lockedBecause, onChange }: NumberBoxProps) {
  return (
    <label
      title={lockedBecause ?? undefined}
      className={`flex h-full flex-col gap-1 ${lockedBecause ? 'cursor-help' : ''}`}
    >
      <span className="text-xs font-semibold uppercase tracking-wider text-white/50">{label}</span>
      <input
        type="number"
        value={value}
        min={min}
        max={max}
        disabled={Boolean(lockedBecause)}
        onChange={(event) => {
          const next = Number(event.target.value)
          if (next >= min && next <= max) onChange(next)
        }}
        className="mt-auto rounded-xl bg-white/10 px-3 py-2 font-display text-lg tabular-nums outline-none ring-1 ring-white/15 focus:ring-2 focus:ring-punch disabled:opacity-40"
      />
    </label>
  )
}
