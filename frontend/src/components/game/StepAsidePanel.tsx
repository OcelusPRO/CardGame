import { useState } from 'react'
import type { GameView } from '../../api/types'
import { Button } from '../ui/Button'
import { Panel } from '../ui/Panel'

interface Props {
  game: GameView
  /** Hands the crown to `heir`, frees this seat and turns the page into the stream view. */
  onStepAside: (heir: string) => void
}

/**
 * The host trading their seat for the stream view: a streamer who puts the table on the
 * computer they stream from, and plays from their phone under another account.
 *
 * The other device joins first, through the invitation like anybody else — this panel only
 * handles the swap. Whoever it names takes the crown, so the phone picked up next is the
 * one that starts the game; by default that is the last seat taken, which is the phone
 * that has just sat down.
 *
 * Folded behind one button: most hosts play from the screen they opened the table on.
 */
export function StepAsidePanel({ game, onStepAside }: Props) {
  const [open, setOpen] = useState(false)
  const others = game.players.filter((player) => player.id !== game.you.id)
  const [picked, setPicked] = useState<string | null>(null)
  const heir = others.find((player) => player.id === picked) ?? others[others.length - 1]
  const hostTwitch = game.players.find((player) => player.id === game.you.id)?.twitchLogin

  if (!open) {
    return (
      <div className="flex justify-end">
        <Button variant="ghost" onClick={() => setOpen(true)}>
          📺 Passer en spectateur
        </Button>
      </div>
    )
  }

  return (
    <Panel title="Jouer depuis un autre appareil">
      <div className="flex flex-col gap-4 text-sm text-ink/75">
        <p>
          Cet écran montrera la partie sans aucune main — de quoi la diffuser — pendant que vous jouez
          depuis un autre appareil. Rejoignez d&apos;abord la table depuis celui-ci avec le lien
          d&apos;invitation, puis choisissez qui devient l&apos;hôte.
        </p>

        {others.length === 0 ? (
          <p className="sketch bg-paper/70 px-4 py-3 text-ink/65">
            Personne d&apos;autre n&apos;est encore à la table : rejoignez-la d&apos;abord depuis
            votre autre appareil.
          </p>
        ) : (
          <fieldset className="flex flex-col gap-2">
            <legend className="mb-2 text-xs font-semibold uppercase tracking-wider text-ink/60">
              Nouvel hôte
            </legend>
            <div className="flex flex-wrap gap-2">
              {others.map((player) => (
                <button
                  key={player.id}
                  type="button"
                  aria-pressed={player.id === heir?.id}
                  onClick={() => setPicked(player.id)}
                  className={`sketch-pill px-4 py-2 font-display font-bold transition ${
                    player.id === heir?.id ? 'bg-mint text-on-accent' : 'bg-paper hover:bg-ink/8'
                  }`}
                >
                  {player.nickname}
                </button>
              ))}
            </div>
          </fieldset>
        )}

        {hostTwitch && heir && heir.twitchLogin !== hostTwitch && (
          <p className="text-xs text-ink/60">
            Le tchat Twitch suit l&apos;hôte : connectez aussi l&apos;autre appareil au compte {hostTwitch}{' '}
            pour garder votes et cartes du tchat.
          </p>
        )}

        <div className="flex flex-wrap justify-end gap-2">
          <Button variant="ghost" onClick={() => setOpen(false)}>
            Annuler
          </Button>
          <Button disabled={!heir} onClick={() => heir && onStepAside(heir.id)}>
            Passer en spectateur
          </Button>
        </div>
      </div>
    </Panel>
  )
}
