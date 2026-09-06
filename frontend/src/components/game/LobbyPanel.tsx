import { useEffect, useState } from 'react'
import type {
  CardPackView,
  DeckInput,
  GameSettingsInput,
  GameView,
  MeView,
} from '../../api/types'
import { sessionApi } from '../../api/session'
import { Panel } from '../ui/Panel'
import { DeckBuilder } from '../deck/DeckBuilder'
import { ChatCardsForm } from './ChatCardsForm'
import { NOT_HOST, SettingsForm } from './SettingsForm'
import { SharePanel } from './SharePanel'

interface Props {
  game: GameView
  /** Who the browser is, and what this server can actually offer them. */
  me?: MeView | null
  onSettings: (patch: GameSettingsInput) => void
  onDeck: (deck: DeckInput) => void
}

/** The waiting room: invite, tune the rules, compose the paquet. */
export function LobbyPanel({ game, me, onSettings, onDeck }: Props) {
  const [packs, setPacks] = useState<CardPackView[]>([])
  const notHost = !game.you.isHost
  const answerMode = game.settings.answerMode
  const hostTwitchLogin = twitchLoginOf(game, game.hostId)

  // Packs can be restricted to a mode, so the list is refetched whenever the host
  // switches between "cartes distribuées" and "sans limites". A guest asks for the
  // paquet as the host built it (passing the game code), so the 18+ packs the host
  // has no access to never show up here.
  useEffect(() => {
    sessionApi
      .packs(answerMode, notHost ? game.code : undefined)
      .then(setPacks)
      .catch(() => setPacks([]))
  }, [answerMode, notHost, game.code])

  return (
    <div className="flex flex-col gap-5">
      {game.you.isHost && <SharePanel code={game.code} />}

      <div className="grid gap-5 xl:grid-cols-2 xl:items-start">
        <Panel title="Règles">
          <SettingsForm
            settings={game.settings}
            disabled={notHost}
            hostTwitchLogin={hostTwitchLogin}
            guestTwitchLogins={guestTwitchLogins(game)}
            onChange={onSettings}
          />
        </Panel>

        {/* The viewers writing cards belong here rather than in the rules: it is another
            way of filling the paquet, alongside the packs and the cards typed below. */}
        <Panel title="Paquet de cartes">
          <div className="flex flex-col gap-4">
            {hostTwitchLogin && (
              <ChatCardsForm
                settings={game.settings.chatCards}
                disabled={notHost}
                lockedBecause={notHost ? NOT_HOST : null}
                hostTwitchLogin={hostTwitchLogin}
                bitsAvailable={me?.twitchExtensionAvailable ?? false}
                rewardsAuthorized={me?.twitchRewardsAuthorized ?? false}
                onChange={(chatCards) => onSettings({ chatCards })}
              />
            )}
            <DeckBuilder packs={packs} disabled={notHost} onApply={onDeck} />
          </div>
        </Panel>
      </div>
    </div>
  )
}

function twitchLoginOf(game: GameView, playerId: string): string | undefined {
  return game.players.find((player) => player.id === playerId)?.twitchLogin
}

/** Every streamer at the table but the host, whose chats the host may pull in too. */
function guestTwitchLogins(game: GameView): string[] {
  return game.players
    .filter((player) => player.id !== game.hostId && player.twitchLogin)
    .map((player) => player.twitchLogin as string)
}
