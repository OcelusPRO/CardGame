import { useState } from 'react'
import { gameUrl, spectateUrl } from '../../lib/gameLinks'
import { Button } from '../ui/Button'
import { CopyButton } from '../ui/CopyButton'
import { Panel } from '../ui/Panel'
import { QrCode } from '../ui/QrCode'

interface Props {
  code: string
}

/**
 * The links to this table, which belong to the host alone — the lobby leaves the panel out
 * entirely for everybody else, rather than handing a guest a half-empty one.
 *
 * Two of them lead here. The invitation opens a seat; the stream view opens the same table
 * with nobody's hand in it, which is what a streamer puts on screen so their chat can
 * follow the game without reading the cards being held. It sits in the same row as the
 * rest, unexplained: a lobby is read by everyone who opens a table, and only a handful of
 * them stream — a paragraph about OBS would cost every host the space it saves one.
 *
 * The code and the QR stay out of sight until asked for, because a lobby is often on a
 * screen other people can see. The block keeps its footprint whether it shows or not:
 * hiding it with `visibility` rather than unmounting it means the panel never jumps when
 * the button is pressed.
 */
export function SharePanel({ code }: Props) {
  const [revealed, setRevealed] = useState(false)
  const url = gameUrl(code)

  return (
    <Panel>
      <div className="flex flex-col items-center gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="text-center sm:text-left">
          <p className="text-xs font-semibold uppercase tracking-wider text-ink/60">Inviter du monde</p>
          <p className="mt-1 max-w-sm text-sm text-ink/70">
            Le lien de cette page suffit pour rejoindre : il n&apos;y a rien d&apos;autre à envoyer.
          </p>
          <div className="mt-3 flex flex-wrap justify-center gap-2 sm:justify-start">
            <CopyButton value={url} />
            <CopyButton value={spectateUrl(code)} label="Copier la vue spectateur" />
            <Button variant="ghost" onClick={() => setRevealed(!revealed)}>
              {revealed ? '🙈 Masquer le code' : '👁️ Afficher le code et le QR'}
            </Button>
          </div>
        </div>

        <div
          aria-hidden={!revealed}
          className={`flex h-40 w-80 shrink-0 items-center justify-end gap-4 transition-opacity duration-200 ${
            revealed ? 'opacity-100' : 'invisible opacity-0'
          }`}
        >
          <div className="flex flex-col items-center gap-2">
            <p className="font-display text-4xl font-extrabold tracking-[0.15em] text-punch">{code}</p>
            <CopyButton value={code} label="Copier le code" />
          </div>
          <QrCode value={url} size={150} />
        </div>
      </div>
    </Panel>
  )
}
