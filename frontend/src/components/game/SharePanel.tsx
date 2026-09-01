import { useState } from 'react'
import { gameUrl } from '../../lib/gameLinks'
import { Button } from '../ui/Button'
import { CopyButton } from '../ui/CopyButton'
import { Panel } from '../ui/Panel'
import { QrCode } from '../ui/QrCode'

interface Props {
  code: string
}

/**
 * The invitation, which belongs to the host alone — the lobby leaves it out entirely for
 * everybody else, rather than handing a guest a half-empty panel.
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
            <Button variant="ghost" onClick={() => setRevealed(!revealed)}>
              {revealed ? '🙈 Masquer le code' : '👁️ Afficher le code et le QR'}
            </Button>
          </div>
        </div>

        <div
          aria-hidden={!revealed}
          className={`flex h-60 w-52 shrink-0 flex-col items-center justify-between transition-opacity duration-200 ${
            revealed ? 'opacity-100' : 'invisible opacity-0'
          }`}
        >
          <p className="font-display text-4xl font-extrabold tracking-[0.2em] text-punch">{code}</p>
          <QrCode value={url} size={150} />
          <CopyButton value={code} label="Copier le code" />
        </div>
      </div>
    </Panel>
  )
}
