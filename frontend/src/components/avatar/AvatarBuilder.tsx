import { motion } from 'motion/react'
import type { AvatarInput } from '../../api/types'
import { Avatar } from './Avatar'
import { avatarInputToView } from './avatarInputToView'
import { BOTTOM_STYLES, PALETTE, TOP_STYLES, randomAvatar } from './avatarCatalog'

interface Props {
  value: AvatarInput
  onChange: (avatar: AvatarInput) => void
  discordAvatarUrl?: string
}

/** Two rows of choices, one per half, plus a dice for the undecided. */
export function AvatarBuilder({ value, onChange, discordAvatarUrl }: Props) {
  return (
    <div className="flex flex-col gap-5 sm:flex-row sm:items-start">
      <motion.div
        className="mx-auto shrink-0 sketch bg-paper/70 p-3"
        animate={{ rotate: [-2, 2, -2] }}
        transition={{ duration: 6, repeat: Infinity, ease: 'easeInOut' }}
      >
        <Avatar avatar={avatarInputToView(value, discordAvatarUrl)} size={150} title="Votre avatar" />
      </motion.div>

      <div className="flex min-w-0 flex-1 flex-col gap-4">
        <StyleRow
          label="Tête"
          styles={TOP_STYLES}
          selected={value.topStyleId}
          onSelect={(topStyleId) => onChange({ ...value, topStyleId })}
        />
        <ColorRow
          label="Couleur de tête"
          selected={value.topColor}
          onSelect={(topColor) => onChange({ ...value, topColor })}
        />
        <StyleRow
          label="Corps"
          styles={BOTTOM_STYLES}
          selected={value.bottomStyleId}
          onSelect={(bottomStyleId) => onChange({ ...value, bottomStyleId })}
        />
        <ColorRow
          label="Couleur de corps"
          selected={value.bottomColor}
          onSelect={(bottomColor) => onChange({ ...value, bottomColor })}
        />
        <button
          type="button"
          onClick={() => onChange(randomAvatar())}
          className="sketch-pill self-start bg-paper px-4 py-2 text-sm font-semibold transition hover:bg-ink/10"
        >
          🎲 Au hasard
        </button>
      </div>
    </div>
  )
}

interface StyleRowProps {
  label: string
  styles: { id: string; label: string }[]
  selected: string
  onSelect: (id: string) => void
}

function StyleRow({ label, styles, selected, onSelect }: StyleRowProps) {
  return (
    <fieldset className="min-w-0">
      <legend className="mb-2 text-xs font-semibold uppercase tracking-wider text-ink/60">{label}</legend>
      <div className="flex flex-wrap gap-2">
        {styles.map((style) => (
          <button
            key={style.id}
            type="button"
            aria-pressed={selected === style.id}
            onClick={() => onSelect(style.id)}
            className={`sketch-pill px-3 py-1.5 text-sm font-medium transition ${
              selected === style.id
                ? 'bg-punch text-white shadow-glow'
                : 'bg-ink/5 text-ink/80 hover:bg-ink/10'
            }`}
          >
            {style.label}
          </button>
        ))}
      </div>
    </fieldset>
  )
}

interface ColorRowProps {
  label: string
  selected: string
  onSelect: (color: string) => void
}

function ColorRow({ label, selected, onSelect }: ColorRowProps) {
  return (
    <fieldset className="min-w-0">
      <legend className="mb-2 text-xs font-semibold uppercase tracking-wider text-ink/60">{label}</legend>
      <div className="flex flex-wrap gap-2">
        {PALETTE.map((color) => (
          <button
            key={color}
            type="button"
            aria-label={`${label} ${color}`}
            aria-pressed={selected === color}
            onClick={() => onSelect(color)}
            style={{ backgroundColor: color }}
            className={`size-8 rounded-full transition ${
              selected === color ? 'ring-3 ring-ink ring-offset-2 ring-offset-paper' : 'ring-1 ring-ink/25'
            }`}
          />
        ))}
      </div>
    </fieldset>
  )
}
