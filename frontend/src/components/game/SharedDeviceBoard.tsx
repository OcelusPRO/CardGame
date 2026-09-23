import { useEffect } from 'react'
import { motion } from 'motion/react'
import type { GameView, MeView, PlayerView } from '../../api/types'
import type { LiveChatVotes } from '../../game/gameStore'
import { messages, type ClientMessage } from '../../game/messages'
import { Avatar } from '../avatar/Avatar'
import { SituationCard } from '../cards/SituationCard'
import { Button } from '../ui/Button'
import { Panel } from '../ui/Panel'
import { GameBoard } from './GameBoard'

interface Props {
  game: GameView
  me?: MeView | null
  send: (message: ClientMessage) => void
  liveChatVotes?: LiveChatVotes
}

/**
 * The table of a shared device: one phone, passed round the room.
 *
 * The server says who the step still waits for (`awaiting`) and whose eyes the snapshot
 * was projected for (`seat`). Everything here follows from those two:
 *
 * - the phone is in the hands of somebody the step waits for → their screen, hand and all;
 * - the step waits for somebody else → a screen that only says who to pass it to, and
 *   shows nothing a neighbour should not read over a shoulder;
 * - the step waits for nobody (the lobby, the results, a chat judging) → the table screen
 *   the whole room reads together, which the server projects without anybody's cards.
 *
 * Nobody's hand ever appears without its owner tapping their own name first, which is the
 * whole promise of playing a hidden-hand game on one screen.
 */
export function SharedDeviceBoard({ game, me, send, liveChatVotes }: Props) {
  const awaiting = game.awaiting ?? []
  const holder = game.seat
  const facingTable = awaiting.length === 0

  // Once the step waits for nobody, the phone turns back to the room: the result is read
  // together, and a hand left on screen would be read with it.
  useEffect(() => {
    if (facingTable && holder) send(messages.seat(null))
  }, [facingTable, holder, send])

  if (holder && awaiting.includes(holder)) {
    return (
      <div className="flex flex-col gap-4">
        <TurnBanner player={playerOf(game, holder)} />
        <GameBoard game={game} me={me} send={send} liveChatVotes={liveChatVotes} />
      </div>
    )
  }

  if (!facingTable) {
    return (
      <HandOff
        game={game}
        awaiting={awaiting}
        done={holder ? playerOf(game, holder) : undefined}
        onTake={(playerId) => send(messages.seat(playerId))}
      />
    )
  }

  // The table view is on its way; the last hand must not be the thing that fills the gap.
  if (holder) return <p className="py-16 text-center font-display text-xl text-ink/60">…</p>

  return <GameBoard game={game} me={me} send={send} liveChatVotes={liveChatVotes} />
}

function playerOf(game: GameView, id: string): PlayerView | undefined {
  return game.players.find((player) => player.id === id)
}

/** Whose screen this is, in case the phone went to the wrong neighbour. */
function TurnBanner({ player }: { player?: PlayerView }) {
  if (!player) return null
  return (
    <div className="sketch-alt flex items-center gap-3 bg-mint/15 px-4 py-2">
      <Avatar avatar={player.avatar} size={36} title={player.nickname} />
      <p className="text-sm">
        C&apos;est au tour de <span className="font-display font-bold">{player.nickname}</span> —
        les autres, on ne regarde pas&nbsp;!
      </p>
    </div>
  )
}

interface HandOffProps {
  game: GameView
  awaiting: string[]
  /** Whoever just handed the phone back, thanked on the way. */
  done?: PlayerView
  onTake: (playerId: string) => void
}

/**
 * "Pass the phone to X." The situation is on it, because everyone may read that; nothing
 * else is. X taps their own name, and only then does their hand come up.
 *
 * The next player is the first one the step waits for, in seat order — but whoever is
 * closest may take it instead, since nobody walks round a sofa to respect a list.
 */
function HandOff({ game, awaiting, done, onTake }: HandOffProps) {
  const [next, ...others] = awaiting
    .map((id) => playerOf(game, id))
    .filter((player): player is PlayerView => player !== undefined)
  if (!next) return null
  const judging = game.phase === 'SELECTING'

  return (
    <div className="flex flex-col gap-5">
      {game.round && !judging && <SituationCard card={game.round.situation} />}

      <Panel>
        <motion.div
          key={next.id}
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex flex-col items-center gap-4 py-4 text-center"
        >
          {done && (
            <p className="text-sm font-semibold text-mint">
              C&apos;est noté, {done.nickname}&nbsp;! Passez la main.
            </p>
          )}
          <Avatar avatar={next.avatar} size={96} title={next.nickname} />
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-ink/60">
              Passez l&apos;appareil à
            </p>
            <p className="font-display text-4xl font-extrabold">{next.nickname}</p>
            <p className="mt-1 text-sm text-ink/65">{roleOf(game, next)}</p>
          </div>
          <Button onClick={() => onTake(next.id)}>C&apos;est moi, {next.nickname}&nbsp;!</Button>

          {others.length > 0 && (
            <div className="flex flex-wrap items-center justify-center gap-2 text-xs text-ink/60">
              <span>Ou plutôt&nbsp;:</span>
              {others.map((player) => (
                <button
                  key={player.id}
                  type="button"
                  onClick={() => onTake(player.id)}
                  className="sketch-pill bg-paper px-3 py-1 font-display font-bold text-ink transition hover:bg-ink/8"
                >
                  {player.nickname}
                </button>
              ))}
            </div>
          )}
          <p className="text-xs text-ink/50">
            {awaiting.length === 1 ? 'Dernier joueur attendu.' : `Encore ${awaiting.length} joueurs attendus.`}
          </p>
        </motion.div>
      </Panel>
    </div>
  )
}

function roleOf(game: GameView, player: PlayerView): string {
  if (game.phase === 'SUBMITTING') return 'À son tour de répondre, en secret.'
  if (player.isCzar) return 'Maître du jeu : son tour de trancher.'
  return game.settings.selectionFormat === 'DUELS'
    ? 'À son tour de départager ce duel.'
    : 'À son tour de voter, en secret.'
}
