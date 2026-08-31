import { useId } from 'react'
import { GameCard } from './GameCard'

interface Props {
  value: string
  onChange: (value: string) => void
  placeholder?: string
  maxLength?: number
  label: string
  disabled?: boolean
}

/**
 * A blank card you write on. No visible input frame: the handwriting font and the
 * ruled line are the whole point, so it feels like scribbling on real cardboard.
 */
export function WritableCard({
  value,
  onChange,
  placeholder = 'Écrivez votre réponse…',
  maxLength = 120,
  label,
  disabled = false,
}: Props) {
  const id = useId()
  const remaining = maxLength - value.length

  return (
    <GameCard
      tone="punchline"
      shape="flexible"
      className="min-h-64 sm:min-h-72"
      footer={`${remaining} caractères restants`}
    >
      <label htmlFor={id} className="sr-only">
        {label}
      </label>
      <textarea
        id={id}
        value={value}
        disabled={disabled}
        maxLength={maxLength}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        rows={4}
        className="h-full w-full resize-none border-none bg-transparent font-hand text-3xl leading-tight text-ink outline-none placeholder:text-ink/30 disabled:opacity-50 sm:text-4xl"
        style={{
          backgroundImage:
            'repeating-linear-gradient(transparent, transparent 2.1rem, rgba(21,7,38,0.12) 2.1rem, rgba(21,7,38,0.12) calc(2.1rem + 1px))',
        }}
      />
    </GameCard>
  )
}
