import { useMemo, useState } from 'react'
import type { CardAdminView, DeckImportInput, PackAdminView } from '../../api/adminTypes'
import { Button } from '../ui/Button'
import { CopyButton } from '../ui/CopyButton'
import { formatDeckText, parseDeckText } from './deckText'

interface Props {
  packs: PackAdminView[]
  situations: CardAdminView[]
  punchlines: CardAdminView[]
  onImport: (input: DeckImportInput) => Promise<PackAdminView>
}

/** Move a whole deck in or out of the catalogue as one plain-text list. */
export function DeckTransfer({ packs, situations, punchlines, onImport }: Props) {
  return (
    <div className="grid gap-8 lg:grid-cols-2">
      <ExportDeck packs={packs} situations={situations} punchlines={punchlines} />
      <ImportDeck packs={packs} onImport={onImport} />
    </div>
  )
}

function ExportDeck({ packs, situations, punchlines }: Omit<Props, 'onImport'>) {
  const [packId, setPackId] = useState(packs[0]?.id ?? '')
  const pack = packs.find((entry) => entry.id === packId)

  const text = useMemo(() => {
    if (!pack) return ''
    return formatDeckText(
      pack,
      situations.filter((card) => card.packId === pack.id).map((card) => card.text),
      punchlines.filter((card) => card.packId === pack.id).map((card) => card.text),
    )
  }, [pack, situations, punchlines])

  return (
    <div className="flex flex-col gap-3">
      <h3 className="font-display text-lg font-bold">Exporter</h3>
      <select
        value={packId}
        aria-label="Pack à exporter"
        onChange={(event) => setPackId(event.target.value)}
        className="rounded-full bg-white/10 px-4 py-2 text-sm outline-none ring-1 ring-white/15"
      >
        {packs.length === 0 && <option value="">Aucun pack</option>}
        {packs.map((entry) => (
          <option key={entry.id} value={entry.id} className="bg-ink-soft">
            {entry.name}
          </option>
        ))}
      </select>

      <textarea
        readOnly
        value={text}
        rows={12}
        aria-label="Deck exporté"
        className="w-full rounded-2xl bg-white/5 p-3 font-mono text-xs leading-relaxed outline-none ring-1 ring-white/15"
      />

      <div className="flex flex-wrap gap-2">
        <CopyButton value={text} label="Copier le deck" />
        <Button
          variant="ghost"
          disabled={!pack}
          onClick={() => pack && download(`${slug(pack.name)}.txt`, text)}
        >
          ⬇️ Télécharger
        </Button>
      </div>
    </div>
  )
}

function ImportDeck({ packs, onImport }: Pick<Props, 'packs' | 'onImport'>) {
  const [replaceId, setReplaceId] = useState('')
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [cards, setCards] = useState(true)
  const [freeText, setFreeText] = useState(true)
  const [text, setText] = useState('')
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const pickReplace = (id: string) => {
    setReplaceId(id)
    const pack = packs.find((entry) => entry.id === id)
    if (pack) {
      setName(pack.name)
      setDescription(pack.description)
      setCards(pack.answerModeCards)
      setFreeText(pack.answerModeFreeText)
    }
  }

  const run = async () => {
    setError(null)
    setMessage(null)
    const parsed = parseDeckText(text)
    const finalName = (name.trim() || parsed.name?.trim()) ?? ''
    if (!finalName) {
      setError('Donnez un nom au pack.')
      return
    }
    if (parsed.situations.length === 0 && parsed.punchlines.length === 0) {
      setError('Aucune carte trouvée : ajoutez les entêtes « ## Situations » et « ## Réponses ».')
      return
    }
    setBusy(true)
    try {
      const saved = await onImport({
        packId: replaceId || undefined,
        name: finalName,
        description: description.trim() || parsed.description?.trim() || '',
        answerModeCards: cards,
        answerModeFreeText: freeText,
        situations: parsed.situations,
        punchlines: parsed.punchlines,
      })
      setMessage(
        `Pack « ${saved.name} » importé : ${saved.situationCount} situations, ${saved.punchlineCount} réponses.`,
      )
      setText('')
      if (!replaceId) {
        setName('')
        setDescription('')
      }
    } catch {
      setError("L'import a échoué. Vérifiez le format du texte.")
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="flex flex-col gap-3">
      <h3 className="font-display text-lg font-bold">Importer</h3>

      <select
        value={replaceId}
        aria-label="Cible de l'import"
        onChange={(event) => pickReplace(event.target.value)}
        className="rounded-full bg-white/10 px-4 py-2 text-sm outline-none ring-1 ring-white/15"
      >
        <option value="">Nouveau pack</option>
        {packs.map((entry) => (
          <option key={entry.id} value={entry.id} className="bg-ink-soft">
            Remplacer : {entry.name}
          </option>
        ))}
      </select>

      <div className="flex flex-wrap gap-2">
        <input
          value={name}
          onChange={(event) => setName(event.target.value)}
          placeholder="Nom du pack"
          className="flex-1 rounded-full bg-white/10 px-4 py-2 text-sm outline-none ring-1 ring-white/15 focus:ring-2 focus:ring-punch"
        />
        <input
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          placeholder="Description"
          className="flex-1 rounded-full bg-white/10 px-4 py-2 text-sm outline-none ring-1 ring-white/15 focus:ring-2 focus:ring-punch"
        />
      </div>

      <fieldset className="flex flex-wrap items-center gap-4 text-sm">
        <legend className="mb-1 text-xs font-semibold uppercase tracking-wider text-white/50">
          Modes de jeu autorisés
        </legend>
        <label className="flex items-center gap-2">
          <input
            type="checkbox"
            checked={cards}
            onChange={(event) => setCards(event.target.checked)}
            className="size-4 accent-punch"
          />
          Cartes distribuées
        </label>
        <label className="flex items-center gap-2">
          <input
            type="checkbox"
            checked={freeText}
            onChange={(event) => setFreeText(event.target.checked)}
            className="size-4 accent-punch"
          />
          Sans limites
        </label>
      </fieldset>

      <textarea
        value={text}
        onChange={(event) => setText(event.target.value)}
        rows={12}
        aria-label="Deck à importer"
        placeholder={'## Situations\nLe pire cadeau, c’est ____.\n\n## Réponses\nun chat mouillé'}
        className="w-full rounded-2xl bg-white/5 p-3 font-mono text-xs leading-relaxed outline-none ring-1 ring-white/15 focus:ring-2 focus:ring-punch"
      />

      <div className="flex flex-wrap items-center gap-2">
        <Button variant="zap" disabled={busy || !text.trim()} onClick={run}>
          {busy ? 'Import…' : replaceId ? 'Remplacer le pack' : 'Importer'}
        </Button>
        {error && <p className="text-xs text-red-300">{error}</p>}
        {message && <p className="text-xs text-mint">{message}</p>}
      </div>
    </div>
  )
}

function download(filename: string, text: string) {
  const url = URL.createObjectURL(new Blob([text], { type: 'text/plain;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

function slug(name: string): string {
  const normalized = name.toLowerCase().normalize('NFD')
  return (
    Array.from(normalized)
      .filter((char) => char.charCodeAt(0) < 0x300 || char.charCodeAt(0) > 0x36f)
      .join('')
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '') || 'deck'
  )
}
