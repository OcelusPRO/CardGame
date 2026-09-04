import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { gamesApi } from '../api/games'
import { useGameSounds } from '../audio/useGameSounds'
import type { GamePreview } from '../api/types'
import { ConnectionBadge } from '../components/game/ConnectionBadge'
import { GameBoard } from '../components/game/GameBoard'
import { GameJoinCard } from '../components/game/GameJoinCard'
import { PhaseTimer } from '../components/game/PhaseTimer'
import { PlayerList } from '../components/game/PlayerList'
import { StartGameBar } from '../components/game/StartGameBar'
import { Toast } from '../components/ui/Toast'
import { useGameStore } from '../game/gameStore'
import { messages } from '../game/messages'
import { errorMessage } from '../lib/errorMessages'
import { gamePath } from '../lib/gameLinks'
import { useIdentity } from '../session/useIdentity'
import { useSession } from '../session/useSession'

type Lookup = 'loading' | 'missing' | GamePreview

/**
 * The single address of a game. A player who already has a seat gets the table, anybody
 * else gets the form to take one — which is what makes the page URL the whole invitation.
 */
export function GamePage() {
  const { code = '' } = useParams()
  const navigate = useNavigate()
  const { me } = useSession()
  const [identity, setIdentity] = useIdentity(me)
  const { game, status, lastError, connect, disconnect, send, dismissError } = useGameStore()
  const [lookup, setLookup] = useState<Lookup>('loading')
  useGameSounds(game)

  const refresh = useCallback(() => {
    gamesApi
      .preview(code)
      .then(setLookup)
      .catch(() => setLookup('missing'))
  }, [code])

  useEffect(() => {
    setLookup('loading')
    refresh()
  }, [refresh])

  const seated = typeof lookup === 'object' && lookup.youArePlaying

  useEffect(() => {
    if (!seated) return
    connect(code)
    return () => disconnect()
  }, [seated, code, connect, disconnect])

  // A finished or forgotten game must not be a dead end: whoever followed the stale link
  // gets a fresh table of their own so the invitation still leads somewhere playable.
  // Without a remembered pseudo there is nothing to open a table with, so send them to
  // the create form instead.
  const recovering = useRef(false)
  useEffect(() => {
    if (lookup !== 'missing' || recovering.current) return
    recovering.current = true
    if (identity.nickname.trim().length < 2) {
      navigate('/create', { replace: true })
      return
    }
    gamesApi
      .create(identity.nickname, identity.avatar)
      .then((ticket) => navigate(gamePath(ticket.code), { replace: true }))
      .catch(() => navigate('/create', { replace: true }))
  }, [lookup, identity, navigate])

  if (lookup === 'loading') return <Centered>On cherche la table…</Centered>

  if (lookup === 'missing') {
    return <Centered>Cette partie n&apos;existe plus — on vous ouvre une nouvelle table…</Centered>
  }

  if (!seated) {
    return (
      <GameJoinCard
        preview={lookup}
        identity={identity}
        onIdentityChange={setIdentity}
        me={me}
        onJoined={refresh}
      />
    )
  }

  if (!game) {
    return (
      <Centered>
        {status === 'closed' ? 'Connexion perdue…' : 'On installe la table…'}
        <span className="mt-3 block">
          <ConnectionBadge status={status} />
        </span>
      </Centered>
    )
  }

  const inLobby = game.phase === 'LOBBY'

  return (
    <div
      className={`mx-auto flex w-full max-w-[110rem] flex-col gap-6 px-4 py-6 lg:flex-row lg:px-8 ${
        needsBottomRoom(game) ? 'pb-44 lg:pb-6' : ''
      }`}
    >
      <main className="order-1 flex min-w-0 flex-1 flex-col gap-5 lg:order-2">
        <header className="flex flex-wrap items-center justify-between gap-3">
          <p className="font-display text-sm font-bold uppercase tracking-wider text-ink/60">
            {inLobby ? 'Salon' : `Manche ${game.round?.number ?? 0} / ${game.settings.rounds}`}
          </p>
          <ConnectionBadge status={status} />
        </header>

        <PhaseTimer
          deadlineMillis={game.deadlineMillis}
          serverTimeMillis={game.serverTimeMillis}
          totalSeconds={timerLength(game.phase, game.settings)}
          label="Temps restant"
          chime={game.phase === 'SUBMITTING' || game.phase === 'SELECTING'}
        />

        <GameBoard game={game} send={send} />
      </main>

      <aside className="order-2 w-full lg:order-1 lg:w-72 lg:shrink-0 xl:w-80">
        <div className="flex flex-col gap-4 lg:sticky lg:top-6">
          <PlayerList game={game} onKick={(playerId) => send(messages.kick(playerId))} />
          {inLobby && (
            <div className="fixed inset-x-4 bottom-4 z-40 lg:static">
              <StartGameBar game={game} onStart={() => send(messages.start())} />
            </div>
          )}
        </div>
      </aside>

      <Toast code={lastError} message={errorMessage(lastError)} onDismiss={dismissError} />
    </div>
  )
}

/** A phone pins the host or send button to the bottom, so the page must not end under it. */
function needsBottomRoom(game: { phase: string; you: { mustAnswer: boolean } }): boolean {
  return game.phase === 'LOBBY' || (game.phase === 'SUBMITTING' && game.you.mustAnswer)
}

function Centered({ children }: { children: React.ReactNode }) {
  return (
    <div className="mx-auto max-w-md px-4 py-24 text-center font-display text-2xl">{children}</div>
  )
}

function timerLength(
  phase: string,
  settings: { submitSeconds: number; selectSeconds: number; resultSeconds: number },
): number {
  if (phase === 'SUBMITTING') return settings.submitSeconds
  if (phase === 'SELECTING') return settings.selectSeconds
  return settings.resultSeconds
}
