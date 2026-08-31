import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { gamesApi } from '../api/games'
import type { GamePreview } from '../api/types'
import { Button } from '../components/ui/Button'
import { Panel } from '../components/ui/Panel'
import { TextField } from '../components/ui/TextField'
import { gamePath } from '../lib/gameLinks'

const CODE_LENGTH = 5

/**
 * The way in for somebody who was given a code rather than a link. It only resolves the
 * code, then hands over to the game address where taking a seat actually happens.
 */
export function JoinPage() {
  const navigate = useNavigate()
  const [code, setCode] = useState('')
  const [preview, setPreview] = useState<GamePreview | null>(null)

  useEffect(() => {
    if (code.length !== CODE_LENGTH) {
      setPreview(null)
      return
    }
    let cancelled = false
    gamesApi
      .preview(code)
      .then((value) => !cancelled && setPreview(value))
      .catch(() => !cancelled && setPreview(null))
    return () => {
      cancelled = true
    }
  }, [code])

  return (
    <div className="mx-auto flex max-w-xl flex-col gap-5 px-4 py-10">
      <h1 className="font-display text-4xl font-extrabold">Rejoindre une partie</h1>

      <Panel>
        <TextField
          label="Code de la partie"
          value={code}
          onChange={setCode}
          placeholder="ABCDE"
          maxLength={CODE_LENGTH}
          uppercase
          autoFocus
          hint={hintFor(preview, code)}
        />
      </Panel>

      <Button full disabled={!preview} onClick={() => navigate(gamePath(code))}>
        Continuer
      </Button>
    </div>
  )
}

function hintFor(preview: GamePreview | null, code: string): string {
  if (code.length !== CODE_LENGTH) return 'Cinq caractères, sans les lettres qui se confondent.'
  if (!preview) return 'Aucune partie trouvée avec ce code.'
  return `Table de ${preview.hostNickname} · ${preview.playerCount}/${preview.maxPlayers} joueurs`
}
