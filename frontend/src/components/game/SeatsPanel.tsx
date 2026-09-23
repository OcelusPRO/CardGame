import { useState, type FormEvent } from 'react'
import type { AvatarInput, GameView } from '../../api/types'
import { spectateUrl } from '../../lib/gameLinks'
import { randomAvatar } from '../avatar/avatarCatalog'
import { Button } from '../ui/Button'
import { CopyButton } from '../ui/CopyButton'
import { Panel } from '../ui/Panel'

interface Props {
  game: GameView
  onAddSeat: (nickname: string, avatar: AvatarInput) => void
}

const MIN_NICKNAME = 2
const MAX_NICKNAME = 20

/**
 * The guest list of a shared device, standing where the invitation sits on an online
 * table: nobody is invited anywhere, the players are on the sofa, and the host types
 * their names. Each gets a random face — nobody wants to wait while four people design an
 * avatar on one phone — and a seat is removed from the player list like any other.
 *
 * The stream view still has a use here: cast on the TV, it is the table everybody reads
 * while the phone goes round.
 */
export function SeatsPanel({ game, onAddSeat }: Props) {
  const [nickname, setNickname] = useState('')
  const trimmed = nickname.trim().replace(/\s+/g, ' ')
  const taken = game.players.some((player) => player.nickname.toLowerCase() === trimmed.toLowerCase())
  const full = game.players.length >= game.settings.maxPlayers
  const valid = trimmed.length >= MIN_NICKNAME && !taken && !full

  const submit = (event: FormEvent) => {
    event.preventDefault()
    if (!valid) return
    onAddSeat(trimmed, randomAvatar())
    setNickname('')
  }

  return (
    <Panel title="Qui joue ?">
      <p className="mb-4 text-sm text-ink/70">
        Tout le monde joue sur cet appareil : ajoutez chaque joueur, puis passez-vous le téléphone
        quand l&apos;écran le demande. Ce qui est dans une main ne s&apos;affiche qu&apos;à son
        propriétaire.
      </p>
      <form onSubmit={submit} className="flex flex-wrap items-end gap-2">
        <label className="flex min-w-48 flex-1 flex-col gap-1.5">
          <span className="text-xs font-semibold uppercase tracking-wider text-ink/60">Pseudo</span>
          <input
            value={nickname}
            maxLength={MAX_NICKNAME}
            placeholder="Le prochain joueur"
            onChange={(event) => setNickname(event.target.value)}
            className="sketch-input bg-paper px-4 py-3 font-display text-lg outline-none transition placeholder:text-ink/35 focus:border-punch"
          />
        </label>
        <Button type="submit" disabled={!valid}>
          Ajouter
        </Button>
      </form>
      {taken && <p className="mt-2 text-xs text-punch">Ce pseudo est déjà à la table.</p>}
      {full && <p className="mt-2 text-xs text-ink/60">La table est complète.</p>}
      <div className="mt-4 flex flex-wrap items-center gap-2 text-xs text-ink/60">
        <CopyButton value={spectateUrl(game.code)} label="Copier la vue spectateur" />
        <span>à ouvrir sur une télé, pour que tout le monde suive la partie.</span>
      </div>
    </Panel>
  )
}
