import type { ReactNode } from 'react'
import type { GameSettingsInput, GameSettingsView, SelectionMode } from '../../api/types'

interface Props {
  settings: GameSettingsView
  disabled: boolean
  /** The host's Twitch channel, when they signed in with it; nothing to read without it. */
  hostTwitchLogin?: string
  /** The other players who signed in with Twitch, whose chats can join the vote. */
  guestTwitchLogins?: string[]
  onChange: (patch: GameSettingsInput) => void
}

const NOT_HOST = "Seul l'hôte peut changer les règles."

/**
 * The host control panel of the lobby. Every change is pushed live to the table.
 *
 * A rule that cannot apply is not greyed out, it is simply absent: no card count in the
 * write-your-own mode, no unanimity bonus when a single czar decides, and nothing about
 * Twitch until the host has actually signed in with it.
 */
export function SettingsForm({
  settings,
  disabled,
  hostTwitchLogin,
  guestTwitchLogins = [],
  onChange,
}: Props) {
  const freeText = settings.answerMode === 'FREE_TEXT'
  const czar = settings.selectionMode === 'CZAR'
  const chatVotes = settings.selectionMode === 'CHAT'
  const lockedBecause = disabled ? NOT_HOST : null

  // The chat only shows up as a choice once the host has a channel to be read — but a
  // game already set on it keeps showing it, so the pressed button is never a ghost.
  const judging = [
    { value: 'VOTE', label: 'Tout le monde vote' },
    { value: 'CZAR', label: 'Maître du jeu tournant' },
    ...(hostTwitchLogin || chatVotes
      ? [{ value: 'CHAT', label: hostTwitchLogin ? `Le tchat de ${hostTwitchLogin} vote` : 'Le tchat vote' }]
      : []),
  ]

  return (
    <div className="flex flex-col gap-4">
      <Choice
        label="Qui désigne la meilleure réponse ?"
        value={settings.selectionMode}
        lockedBecause={lockedBecause}
        options={judging}
        onSelect={(mode) => onChange({ selectionMode: mode as SelectionMode })}
      />

      {chatVotes && (
        <Toggle
          label={
            guestTwitchLogins.length > 0
              ? `Inclure les tchats des autres joueurs (${guestTwitchLogins.join(', ')})`
              : 'Inclure les tchats des autres joueurs connectés à Twitch'
          }
          checked={settings.twitchGuestChats}
          lockedBecause={lockedBecause}
          onChange={(twitchGuestChats) => onChange({ twitchGuestChats })}
        />
      )}

      <Choice
        label="D'où viennent les réponses ?"
        value={settings.answerMode}
        lockedBecause={lockedBecause}
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
          lockedBecause={lockedBecause}
          onChange={(rounds) => onChange({ rounds })}
        />
        {!freeText && (
          <NumberBox
            label="Cartes en main"
            value={settings.handSize}
            min={4}
            max={15}
            lockedBecause={lockedBecause}
            onChange={(handSize) => onChange({ handSize })}
          />
        )}
        <NumberBox
          label="Temps de réponse"
          value={settings.submitSeconds}
          min={15}
          max={300}
          lockedBecause={lockedBecause}
          onChange={(submitSeconds) => onChange({ submitSeconds })}
        />
        <NumberBox
          label="Temps de vote"
          value={settings.selectSeconds}
          min={15}
          max={300}
          lockedBecause={lockedBecause}
          onChange={(selectSeconds) => onChange({ selectSeconds })}
        />
        {!chatVotes && (
          <NumberBox
            label="Points par vote"
            value={settings.pointsPerVote}
            min={1}
            max={20}
            lockedBecause={lockedBecause}
            onChange={(pointsPerVote) => onChange({ pointsPerVote })}
          />
        )}
        {!czar && !chatVotes && (
          <NumberBox
            label="Bonus unanimité"
            value={settings.unanimityBonus}
            min={0}
            max={20}
            lockedBecause={lockedBecause}
            onChange={(unanimityBonus) => onChange({ unanimityBonus })}
          />
        )}
        <NumberBox
          label="Joueurs maximum"
          value={settings.maxPlayers}
          min={2}
          max={24}
          lockedBecause={lockedBecause}
          onChange={(maxPlayers) => onChange({ maxPlayers })}
        />
      </div>

      {!czar && !chatVotes && (
        <Toggle
          label="Autoriser à voter pour sa propre carte"
          checked={settings.allowSelfVote}
          lockedBecause={lockedBecause}
          onChange={(allowSelfVote) => onChange({ allowSelfVote })}
        />
      )}

      {czar && (
        <Toggle
          label="Le maître du jeu répond aussi"
          checked={settings.czarAnswers}
          lockedBecause={lockedBecause}
          onChange={(czarAnswers) => onChange({ czarAnswers })}
        />
      )}

      <p className="sketch bg-paper/70 px-4 py-3 text-xs leading-relaxed text-ink/65">
        {chatVotes
          ? `Personne à la table ne vote : la réponse que les spectateurs ont le plus choisie remporte la manche, et une manche vaut 1 point — une communauté de trois mille personnes ne rapporte pas plus qu'une de trente. La manche va au bout de son chrono pour laisser aux tchats le temps de répondre. ${settings.rounds} manches, et le meilleur score l'emporte.`
          : czar
            ? `Le maître du jeu choisit, et la réponse retenue rapporte ${settings.pointsPerVote} point(s). ${settings.rounds} manches, et le meilleur score l'emporte.`
            : `Chaque voix reçue rapporte ${settings.pointsPerVote} point(s). Une réponse choisie par tous ceux qui pouvaient la choisir gagne ${settings.unanimityBonus} point(s) de plus ; une seule voix ailleurs et le bonus tombe à zéro. ${settings.rounds} manches, et le meilleur score l'emporte.`}
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
      <legend className="mb-2 text-xs font-semibold uppercase tracking-wider text-ink/60">{label}</legend>
      <div className="flex flex-wrap gap-2">
        {options.map((option) => (
          <button
            key={option.value}
            type="button"
            disabled={Boolean(lockedBecause)}
            aria-pressed={value === option.value}
            onClick={() => onSelect(option.value)}
            className={`sketch-pill px-4 py-2 text-sm font-semibold transition disabled:cursor-help disabled:opacity-40 ${
              value === option.value ? 'bg-grape text-white' : 'bg-ink/5 hover:bg-ink/10'
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
  label: ReactNode
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
      <span className="text-xs font-semibold uppercase tracking-wider text-ink/60">{label}</span>
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
        className="mt-auto sketch-input bg-paper px-3 py-2 font-display text-lg tabular-nums outline-none focus:border-punch disabled:opacity-40"
      />
    </label>
  )
}
