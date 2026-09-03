import { useEffect, useState } from 'react'
import type { CardPackView, DeckInput, GameSettingsInput, GameView } from '../../api/types'
import { sessionApi } from '../../api/session'
import { Panel } from '../ui/Panel'
import { DeckBuilder } from '../deck/DeckBuilder'
import { SettingsForm } from './SettingsForm'
import { SharePanel } from './SharePanel'

interface Props {
  game: GameView
  onSettings: (patch: GameSettingsInput) => void
  onDeck: (deck: DeckInput) => void
}

/** The waiting room: invite, tune the rules, compose the paquet. */
export function LobbyPanel({ game, onSettings, onDeck }: Props) {
  const [packs, setPacks] = useState<CardPackView[]>([])
  const notHost = !game.you.isHost
  const answerMode = game.settings.answerMode

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
          <SettingsForm settings={game.settings} disabled={notHost} onChange={onSettings} />
        </Panel>

        <Panel title="Paquet de cartes">
          <DeckBuilder packs={packs} disabled={notHost} onApply={onDeck} />
        </Panel>
      </div>
    </div>
  )
}
