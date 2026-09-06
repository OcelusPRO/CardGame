import { useCallback, useEffect, useRef, useState } from 'react'
import type {
  ChannelRewardView,
  ChatCardAccess,
  ChatCardRewardView,
  ChatCardsInput,
  ChatCardsView,
} from '../../api/types'
import { sessionApi } from '../../api/session'
import { rememberReturnPath } from '../../session/authReturn'

interface Props {
  settings: ChatCardsView
  disabled: boolean
  lockedBecause: string | null
  /** The host's channel, which is the one the viewers write from. */
  hostTwitchLogin: string
  /** No extension configured on this server means no panel, and so no bits. */
  bitsAvailable: boolean
  /** Whether the host let the game own channel point rewards on their channel. */
  rewardsAuthorized: boolean
  onChange: (patch: ChatCardsInput) => void
}

const PAID: { value: ChatCardAccess; label: string }[] = [
  { value: 'EVERYONE', label: 'Ouvert à tous' },
  { value: 'CHANNEL_POINTS', label: 'Points de chaîne' },
  { value: 'BITS', label: 'Bits' },
]

/**
 * Lets the host open their chat to writing, and set what it costs.
 *
 * The door is a single checkbox, because that is the actual question: does this table take
 * cards from the viewers at all. Everything under it — who pays, how much — only appears
 * once the answer is yes, since a greyed out price for a closed door is just noise.
 *
 * The two prices are not set in the same place, and that is Twitch's doing rather than a
 * choice. A cheer is priced by the products declared on the extension, so all this asks
 * for is a floor. Channel point rewards are picked off the channel itself, or built here.
 */
export function ChatCardsForm({
  settings,
  disabled,
  lockedBecause,
  hostTwitchLogin,
  bitsAvailable,
  rewardsAuthorized,
  onChange,
}: Props) {
  const open = settings.access !== 'OFF'
  const points = settings.access === 'CHANNEL_POINTS'
  const ways = PAID.filter((way) => way.value !== 'BITS' || bitsAvailable)
  // Only the host is ever asked to authorise anything: the others are looking at the
  // host's channel, and what they need to see is what the rewards will be called.
  const askSignIn = points && !rewardsAuthorized && !disabled
  const showRewards = points && (rewardsAuthorized || disabled)
  const channel = useChannelRewards(points && rewardsAuthorized && !disabled)

  return (
    <div className="sketch flex flex-col gap-3 bg-[#9146FF]/10 px-4 py-3">
      <Check
        label={`Le tchat de ${hostTwitchLogin} peut créer des cartes`}
        checked={open}
        disabled={disabled}
        lockedBecause={lockedBecause}
        onChange={(wanted) => onChange({ access: wanted ? 'EVERYONE' : 'OFF' })}
      />

      {open && (
        <>
          <fieldset title={lockedBecause ?? undefined}>
            <legend className="mb-2 text-xs font-semibold uppercase tracking-wider text-ink/60">
              Ce qu'une carte coûte au spectateur
            </legend>
            <div className="flex flex-wrap gap-2">
              {ways.map((way) => (
                <button
                  key={way.value}
                  type="button"
                  disabled={disabled}
                  aria-pressed={settings.access === way.value}
                  onClick={() => onChange({ access: way.value })}
                  className={`sketch-pill px-4 py-2 text-sm font-semibold transition disabled:cursor-help disabled:opacity-40 ${
                    settings.access === way.value ? 'bg-[#772ce8] text-white' : 'bg-ink/5 hover:bg-ink/10'
                  }`}
                >
                  {way.label}
                </button>
              ))}
            </div>
          </fieldset>

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
            <NumberBox
              label="Bits minimum par carte"
              value={settings.minBits}
              min={1}
              max={100000}
              disabled={disabled}
              lockedBecause={lockedBecause}
              onChange={(minBits) => onChange({ minBits })}
            />
          )}

          {askSignIn && <Reconnect />}

          {showRewards && (
            <div className="flex flex-col gap-3">
              {channel.error && <p className="text-xs leading-relaxed text-honey">{channel.error}</p>}
              {settings.situations && (
                <Reward
                  pile="situation"
                  reward={settings.situationReward}
                  standing={channel.rewards}
                  disabled={disabled}
                  lockedBecause={lockedBecause}
                  onReload={channel.reload}
                  onChange={(situationReward) => onChange({ situationReward })}
                />
              )}
              {settings.punchlines && (
                <Reward
                  pile="réponse"
                  reward={settings.punchlineReward}
                  standing={channel.rewards}
                  disabled={disabled}
                  lockedBecause={lockedBecause}
                  onReload={channel.reload}
                  onChange={(punchlineReward) => onChange({ punchlineReward })}
                />
              )}
            </div>
          )}

          <p className="text-xs leading-relaxed text-ink/65">
            {explanation(settings, !askSignIn)}
          </p>
        </>
      )}
    </div>
  )
}

/**
 * What is already on the host's channel, asked for only when it can be. The reload is what
 * a host presses after deleting a reward on Twitch, so they never have to reload the lobby.
 */
function useChannelRewards(wanted: boolean) {
  const [rewards, setRewards] = useState<ChannelRewardView[]>([])
  const [error, setError] = useState<string | null>(null)

  const reload = useCallback(() => {
    if (!wanted) return
    sessionApi
      .twitchRewards()
      .then((found) => {
        setRewards(found)
        setError(null)
      })
      .catch(() =>
        setError(
          "Les récompenses de la chaîne n'ont pas pu être lues. Les points de chaîne demandent une chaîne affiliée ou partenaire.",
        ),
      )
  }, [wanted])

  useEffect(reload, [reload])

  return { rewards, error, reload }
}

/**
 * The right to manage the rewards is granted on the Twitch sign in itself, so a host who
 * is signed in already has it — unless they signed in before the game started asking for
 * it. Signing in again is the whole fix, and it is the only case this ever shows.
 */
function Reconnect() {
  return (
    <div className="sketch flex flex-col gap-2 bg-honey/15 px-3 py-2">
      <p className="text-xs leading-relaxed text-ink/75">
        Pour ce mode, le jeu doit pouvoir gérer les récompenses de votre chaîne. Votre session
        Twitch date d'avant cette autorisation : reconnectez-vous, et Twitch vous la demandera.
      </p>
      <a
        href="/auth/twitch"
        onClick={() => rememberReturnPath()}
        className="sketch-pill self-start bg-[#772ce8] px-4 py-2 text-sm font-semibold text-white transition hover:brightness-110"
      >
        Se reconnecter avec Twitch
      </a>
    </div>
  )
}

interface RewardProps {
  /** Which pile it feeds, said as the viewers would read it. */
  pile: string
  reward: ChatCardRewardView
  /** What is already standing on the channel, to pick from. */
  standing: ChannelRewardView[]
  disabled: boolean
  lockedBecause: string | null
  onReload: () => void
  onChange: (reward: ChatCardRewardView) => void
}

/** How long a host may keep typing a reward name before the table hears about it. */
const TYPING_PAUSE = 400

/**
 * Which reward feeds this pile: one already on the channel, or one the game builds.
 *
 * What is typed is held here and pushed after a pause, which is not a nicety: a reward is a
 * real thing standing on a real channel, and the server puts up a new one whenever its name
 * or its price changes. Sending a keystroke at a time would have the viewers watch a reward
 * flicker in and out twenty times while the host types its name. A pick, having no next
 * keystroke to wait for, goes straight through.
 */
function Reward({ pile, reward, standing, disabled, lockedBecause, onReload, onChange }: RewardProps) {
  const [draft, setDraft] = useState(reward)
  const timer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined)

  // A guest is watching the host choose, so their boxes follow the table. The host's own do
  // not: a snapshot landing mid-word would take the cursor with it.
  useEffect(() => {
    if (disabled) setDraft(reward)
  }, [disabled, reward])
  useEffect(() => () => clearTimeout(timer.current), [])

  const push = (next: ChatCardRewardView, now = false) => {
    setDraft(next)
    clearTimeout(timer.current)
    if (now) onChange(next)
    else timer.current = setTimeout(() => onChange(next), TYPING_PAUSE)
  }

  const picked = standing.find((it) => it.id === draft.id)
  const ours = standing.filter((it) => it.managed)
  const theirs = standing.filter((it) => !it.managed)
  // The host asked to rebuild a reward whose original is still standing. Twitch refuses two
  // rewards of the same name, so nothing can happen until they delete it themselves.
  const clashing = draft.id === '' && theirs.find((it) => it.title === draft.title.trim())

  return (
    <fieldset title={lockedBecause ?? undefined} className="flex flex-col gap-2">
      <legend className="mb-2 text-xs font-semibold uppercase tracking-wider text-ink/60">
        La récompense « {pile} »
      </legend>

      <div className="flex flex-wrap items-end gap-3">
        <label className={`flex flex-col gap-1 ${lockedBecause ? 'cursor-help' : ''}`}>
          <span className="text-xs font-semibold uppercase tracking-wider text-ink/60">
            Laquelle
          </span>
          <select
            aria-label={`Récompense « ${pile} »`}
            value={draft.id}
            disabled={disabled}
            onChange={(event) => {
              const chosen = standing.find((it) => it.id === event.target.value)
              push(
                chosen
                  ? { id: chosen.id, title: chosen.title, cost: chosen.cost }
                  : { id: '', title: reward.title, cost: reward.cost },
                true,
              )
            }}
            className="sketch-input w-64 bg-paper px-3 py-2 text-sm outline-none focus:border-punch disabled:opacity-40"
          >
            <option value="">Une nouvelle, créée par le jeu</option>
            {ours.length > 0 && (
              <optgroup label="Déjà gérées par le jeu">
                {ours.map((it) => (
                  <option key={it.id} value={it.id}>
                    {it.title} — {it.cost} points
                  </option>
                ))}
              </optgroup>
            )}
            {theirs.length > 0 && (
              <optgroup label="Créées sur Twitch">
                {theirs.map((it) => (
                  <option key={it.id} value={it.id}>
                    {it.title} — {it.cost} points
                  </option>
                ))}
              </optgroup>
            )}
          </select>
        </label>

        {draft.id === '' && (
          <>
            <label className={`flex flex-col gap-1 ${lockedBecause ? 'cursor-help' : ''}`}>
              <span className="text-xs font-semibold uppercase tracking-wider text-ink/60">Nom</span>
              <input
                type="text"
                aria-label={`Nom de la récompense « ${pile} »`}
                value={draft.title}
                maxLength={45}
                disabled={disabled}
                onChange={(event) => push({ ...draft, title: event.target.value })}
                className="sketch-input w-56 bg-paper px-3 py-2 text-sm outline-none focus:border-punch disabled:opacity-40"
              />
            </label>
            <NumberBox
              label="Points"
              name={`Points de la récompense « ${pile} »`}
              value={draft.cost}
              min={1}
              max={1000000}
              disabled={disabled}
              lockedBecause={lockedBecause}
              onChange={(cost) => push({ ...draft, cost })}
            />
          </>
        )}
      </div>

      {picked?.managed && (
        <p className="text-xs leading-relaxed text-ink/65">
          Le jeu a créé cette récompense : il suivra les échanges, validera les cartes prises
          et rendra les points de celles qui sont refusées. Elle restera sur votre chaîne
          après la partie.
        </p>
      )}

      {picked && !picked.managed && (
        <Unmanaged
          reward={picked}
          disabled={disabled}
          onRebuild={() => push({ id: '', title: picked.title, cost: picked.cost }, true)}
        />
      )}

      {clashing && <Clash title={clashing.title} disabled={disabled} onReload={onReload} />}
    </fieldset>
  )
}

/**
 * The warning that matters most, and the choice that goes with it.
 *
 * Twitch only lets the application that created a reward fulfil or cancel its redemptions.
 * The game can still *read* one the streamer built themselves — the card reaches the table
 * all the same — it simply cannot answer for it. So the host picks: keep it and validate by
 * hand, or have it rebuilt as the game's own and let the refunds happen on their own.
 */
function Unmanaged({
  reward,
  disabled,
  onRebuild,
}: {
  reward: ChannelRewardView
  disabled: boolean
  onRebuild: () => void
}) {
  return (
    <div className="sketch flex flex-col gap-2 bg-honey/15 px-3 py-2">
      <p className="text-xs leading-relaxed text-ink/75">
        <strong>« {reward.title} » n'a pas été créée par le jeu</strong>, et Twitch réserve la
        validation d'un échange à l'application qui a créé la récompense. Le jeu lira les
        échanges et prendra les cartes normalement, mais <strong>il ne pourra ni valider ni
        rembourser</strong> : les échanges resteront dans votre file d'attente Twitch, à
        valider ou à refuser vous-même — y compris ceux d'une carte que la table a refusée.
      </p>
      <p className="text-xs leading-relaxed text-ink/75">
        Vous pouvez la garder ainsi, ou la faire recréer à l'identique par le jeu, qui s'en
        occupera alors seul. Le jeu ne peut pas supprimer la vôtre — c'est la même règle
        Twitch —, ce sera donc à vous de le faire dans votre tableau de bord.
      </p>
      <button
        type="button"
        disabled={disabled}
        onClick={onRebuild}
        className="sketch-pill self-start bg-ink/5 px-4 py-2 text-sm font-semibold transition hover:bg-ink/10 disabled:opacity-40"
      >
        La recréer à l'identique
      </button>
    </div>
  )
}

/** The reward being rebuilt is still on the channel, and Twitch refuses two of a name. */
function Clash({
  title,
  disabled,
  onReload,
}: {
  title: string
  disabled: boolean
  onReload: () => void
}) {
  return (
    <div className="sketch flex flex-col gap-2 bg-punch/10 px-3 py-2">
      <p className="text-xs leading-relaxed text-ink/75">
        « {title} » est encore sur votre chaîne, et Twitch refuse deux récompenses du même
        nom : supprimez-la dans votre tableau de bord Twitch, puis revenez ici. Vous pouvez
        aussi simplement en changer le nom ci-dessus.
      </p>
      <button
        type="button"
        disabled={disabled}
        onClick={onReload}
        className="sketch-pill self-start bg-ink/5 px-4 py-2 text-sm font-semibold transition hover:bg-ink/10 disabled:opacity-40"
      >
        C'est fait, revérifier
      </button>
    </div>
  )
}

function explanation(settings: ChatCardsView, rewardsAuthorized: boolean): string {
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
    if (!rewardsAuthorized) {
      return `Une fois reconnecté, vous pourrez choisir une récompense déjà sur votre chaîne, ou en faire créer une par le jeu le temps de la partie.`
    }
    return `Vos spectateurs écrivent ${what} directement dans la case de la récompense — rien à taper dans le tchat. Une récompense créée par le jeu est retirée à la fin de la partie ; une récompense que vous avez choisie reste où elle est.`
  }
  return `N'importe qui dans votre tchat peut écrire ${what} avec ${commands}.${shortcuts} Une carte par personne toutes les quelques secondes, et 200 au maximum pour toute la partie.`
}

interface NumberBoxProps {
  label: string
  /** The accessible name, when the visible label is too terse to tell two boxes apart. */
  name?: string
  value: number
  min: number
  max: number
  disabled: boolean
  lockedBecause: string | null
  onChange: (value: number) => void
}

function NumberBox({ label, name, value, min, max, disabled, lockedBecause, onChange }: NumberBoxProps) {
  return (
    <label
      title={lockedBecause ?? undefined}
      className={`flex flex-col gap-1 ${lockedBecause ? 'cursor-help' : ''}`}
    >
      <span className="text-xs font-semibold uppercase tracking-wider text-ink/60">{label}</span>
      <input
        type="number"
        aria-label={name}
        value={value}
        min={min}
        max={max}
        disabled={disabled}
        onChange={(event) => {
          const next = Number(event.target.value)
          if (next >= min && next <= max) onChange(next)
        }}
        className="sketch-input w-36 bg-paper px-3 py-2 font-display text-lg tabular-nums outline-none focus:border-punch disabled:opacity-40"
      />
    </label>
  )
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
