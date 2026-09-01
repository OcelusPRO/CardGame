import { useId, type KeyboardEvent } from 'react'
import { GameCard } from './GameCard'

interface Props {
  value: string
  onChange: (value: string) => void
  placeholder?: string
  maxLength?: number
  label: string
  disabled?: boolean
  /** Called when the player presses Enter. Left out, Enter just breaks the line. */
  onSubmit?: () => void
}

/**
 * A blank card you write on. No visible input frame: the handwriting font and the
 * ruled line are the whole point, so it feels like scribbling on real cardboard.
 *
 * Enter sends the answer and Shift+Enter breaks the line, which is the wrong way round
 * for a textarea and the right way round here: an answer is one line far more often than
 * it is two, and the round is on a timer.
 */
export function WritableCard({
  value,
  onChange,
  placeholder = 'Écrivez votre réponse…',
  maxLength = 120,
  label,
  disabled = false,
  onSubmit,
}: Props) {
  const id = useId()
  const remaining = maxLength - value.length

  const onKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (!onSubmit || event.key !== 'Enter' || event.shiftKey) return
    // Enter also closes the suggestion list of an input method; that keystroke belongs to
    // the keyboard, not to us.
    if (event.nativeEvent.isComposing) return
    event.preventDefault()
    onSubmit()
  }

  return (
    <GameCard
      tone="punchline"
      shape="flexible"
      className="min-h-64 sm:min-h-72"
      footer={
        onSubmit
          ? `${remaining} caractères restants · Entrée pour envoyer, Maj+Entrée pour aller à la ligne`
          : `${remaining} caractères restants`
      }
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
        onKeyDown={onKeyDown}
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
