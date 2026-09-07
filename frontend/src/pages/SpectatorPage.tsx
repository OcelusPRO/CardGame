import { useEffect } from 'react'
import { useLocation, useParams } from 'react-router-dom'
import { ConnectionBadge } from '../components/game/ConnectionBadge'
import { PhaseTimer } from '../components/game/PhaseTimer'
import { PlayerList } from '../components/game/PlayerList'
import { SpectatorBoard } from '../components/game/SpectatorBoard'
import { useGameStore } from '../game/gameStore'
import { phaseLengthSeconds } from '../game/phaseLength'
import { isOverlay } from '../lib/gameLinks'

/**
 * The stream page: the table with nobody's hand in it.
 *
 * A streamer plays on `/game/CODE` and shows this instead, either as a window on a second
 * monitor or as a browser source laid over the video with `?overlay=1`. What makes it work
 * is entirely on the server — the snapshot arriving here was projected without a hand,
 * without an author before the reveal, and without anything to act on — so there is no
 * secret on this page that a determined viewer could read out of the payload.
 *
 * No seat is taken and no session is asked for. Anybody holding the link watches, which is
 * the only way a capture source can: OBS has no cookies to sign in with.
 */
export function SpectatorPage() {
  const { code } = useParams()
  const overlay = isOverlay(useLocation().search)
  const { game, chatVotes, status, rejection, spectate, disconnect } = useGameStore()

  useEffect(() => {
    if (!code) return
    spectate(code)
    return () => disconnect()
  }, [code, spectate, disconnect])

  // The overlay is layered over video: the page has to stop painting a ground of its own,
  // and the whole type scale is lifted so it survives being read from a sofa. Both hang
  // off one attribute, which `index.css` answers — and which is cleared on the way out so
  // an ordinary page never inherits it.
  useEffect(() => {
    if (!overlay) return
    document.documentElement.dataset.overlay = 'on'
    return () => {
      delete document.documentElement.dataset.overlay
    }
  }, [overlay])

  if (status === 'rejected') {
    return <Centered>{rejection || "Cette table n'est plus là."}</Centered>
  }

  if (!game) {
    return (
      <Centered>
        {status === 'closed' ? 'Connexion perdue…' : 'On rejoint la table…'}
        <span className="mt-3 block">
          <ConnectionBadge status={status} />
        </span>
      </Centered>
    )
  }

  const inLobby = game.phase === 'LOBBY'
  const board = <SpectatorBoard game={game} liveChatVotes={chatVotes} />
  // The clock is drawn, never heard: a browser source that started ticking into a live
  // stream would be a surprise nobody asked for, and the player's own page already does it.

  if (overlay) {
    return (
      <div className="mx-auto flex w-full max-w-6xl flex-col gap-4 px-4 py-4">
        {!inLobby && (
          <PhaseTimer
            deadlineMillis={game.deadlineMillis}
            serverTimeMillis={game.serverTimeMillis}
            totalSeconds={phaseLengthSeconds(game.phase, game.settings)}
            label="Temps restant"
            chime={false}
          />
        )}
        {board}
      </div>
    )
  }

  return (
    <div className="mx-auto flex w-full max-w-[110rem] flex-col gap-6 px-4 py-6 lg:flex-row lg:px-8">
      <main className="order-1 flex min-w-0 flex-1 flex-col gap-5 lg:order-2">
        <header className="flex flex-wrap items-center justify-between gap-3">
          <p className="font-display text-sm font-bold uppercase tracking-wider text-ink/60">
            {inLobby ? 'Salon · vue spectateur' : `Manche ${game.round?.number ?? 0} / ${game.settings.rounds}`}
          </p>
          <ConnectionBadge status={status} />
        </header>

        <PhaseTimer
          deadlineMillis={game.deadlineMillis}
          serverTimeMillis={game.serverTimeMillis}
          totalSeconds={phaseLengthSeconds(game.phase, game.settings)}
          label="Temps restant"
          chime={false}
        />

        {board}
      </main>

      <aside className="order-2 w-full lg:order-1 lg:w-72 lg:shrink-0 xl:w-80">
        <div className="lg:sticky lg:top-6">
          <PlayerList game={game} />
        </div>
      </aside>
    </div>
  )
}

function Centered({ children }: { children: React.ReactNode }) {
  return (
    <div className="mx-auto max-w-md px-4 py-24 text-center font-display text-2xl">{children}</div>
  )
}
