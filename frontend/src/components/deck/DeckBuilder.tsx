import { useEffect, useState } from 'react'
import type { CardPackView, DeckInput } from '../../api/types'
import { Button } from '../ui/Button'
import { DeckOption } from './DeckOption'
import { linesToCards } from './linesToCards'
import { useSavedDecks } from './useSavedDecks'

interface Props {
  packs: CardPackView[]
  disabled: boolean
  onApply: (deck: DeckInput) => void
}

/**
 * Where the host composes the paquet: one selector holding the official packs and the
 * decks kept in this browser, plus a place to type cards on the spot.
 */
export function DeckBuilder({ packs, disabled, onApply }: Props) {
  const { decks, save, remove } = useSavedDecks()
  const [selectedPacks, setSelectedPacks] = useState<string[]>([])
  const [selectedDecks, setSelectedDecks] = useState<string[]>([])
  const [situations, setSituations] = useState('')
  const [punchlines, setPunchlines] = useState('')
  const [deckName, setDeckName] = useState('')

  // A brand new table plays with everything the site offers, exactly like the server default.
  useEffect(() => setSelectedPacks(packs.map((pack) => pack.id)), [packs])

  const chosenDecks = decks.filter((deck) => selectedDecks.includes(deck.id))

  const apply = () => {
    onApply({
      packIds: selectedPacks,
      customSituations: [...chosenDecks.flatMap((deck) => deck.situations), ...linesToCards(situations)],
      customPunchlines: [...chosenDecks.flatMap((deck) => deck.punchlines), ...linesToCards(punchlines)],
    })
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap gap-2">
        {packs.map((pack) => (
          <DeckOption
            key={pack.id}
            name={pack.name}
            detail={`${pack.situationCount} situations · ${pack.punchlineCount} réponses`}
            official
            selected={selectedPacks.includes(pack.id)}
            disabled={disabled}
            onToggle={() => setSelectedPacks(toggle(selectedPacks, pack.id))}
          />
        ))}
        {decks.map((deck) => (
          <DeckOption
            key={deck.id}
            name={deck.name}
            detail={`${deck.situations.length} situations · ${deck.punchlines.length} réponses`}
            official={false}
            selected={selectedDecks.includes(deck.id)}
            disabled={disabled}
            onToggle={() => setSelectedDecks(toggle(selectedDecks, deck.id))}
          />
        ))}
        {packs.length === 0 && decks.length === 0 && (
          <p className="text-sm text-white/50">Aucun deck disponible : écrivez les vôtres ci-dessous.</p>
        )}
      </div>

      {selectedPacks.length === 0 && selectedDecks.length === 0 && (
        <p className="text-xs text-zap">
          Aucun deck sélectionné : la partie se jouera uniquement sur les cartes écrites ici.
        </p>
      )}

      <CardTextArea
        label="Vos situations (une par ligne, utilisez ____ pour les trous)"
        value={situations}
        onChange={setSituations}
        disabled={disabled}
        placeholder={'Chez moi, on ne parle jamais de ____.\nLe secret de ma réussite : ____.'}
      />
      <CardTextArea
        label="Vos réponses (une par ligne)"
        value={punchlines}
        onChange={setPunchlines}
        disabled={disabled}
        placeholder={'un poulet rôti mal cuit\nla honte de ma vie'}
      />

      <div className="flex flex-wrap items-center gap-2">
        <Button onClick={apply} disabled={disabled} variant="zap">
          Appliquer le paquet
        </Button>
        <input
          value={deckName}
          onChange={(event) => setDeckName(event.target.value)}
          placeholder="Nom du deck"
          className="rounded-full bg-white/10 px-4 py-2 text-sm outline-none ring-1 ring-white/15 focus:ring-2 focus:ring-punch"
        />
        <Button
          variant="ghost"
          disabled={!deckName.trim()}
          onClick={() => {
            save(deckName.trim(), linesToCards(situations), linesToCards(punchlines))
            setDeckName('')
          }}
        >
          💾 Enregistrer
        </Button>
        {decks.length > 0 && (
          <select
            aria-label="Supprimer un deck enregistré"
            value=""
            onChange={(event) => event.target.value && remove(event.target.value)}
            className="rounded-full bg-white/10 px-3 py-2 text-sm outline-none ring-1 ring-white/15"
          >
            <option value="">Supprimer un deck…</option>
            {decks.map((deck) => (
              <option key={deck.id} value={deck.id} className="bg-ink-soft">
                {deck.name}
              </option>
            ))}
          </select>
        )}
      </div>
    </div>
  )
}

function toggle(values: string[], id: string): string[] {
  return values.includes(id) ? values.filter((value) => value !== id) : [...values, id]
}

interface CardTextAreaProps {
  label: string
  value: string
  onChange: (value: string) => void
  disabled: boolean
  placeholder: string
}

function CardTextArea({ label, value, onChange, disabled, placeholder }: CardTextAreaProps) {
  return (
    <label className="flex flex-col gap-1.5">
      <span className="text-xs font-semibold uppercase tracking-wider text-white/50">{label}</span>
      <textarea
        value={value}
        disabled={disabled}
        placeholder={placeholder}
        rows={4}
        onChange={(event) => onChange(event.target.value)}
        className="rounded-2xl bg-white/10 px-4 py-3 font-hand text-2xl leading-tight outline-none ring-1 ring-white/15 transition placeholder:text-white/25 focus:ring-2 focus:ring-punch disabled:opacity-40"
      />
    </label>
  )
}
