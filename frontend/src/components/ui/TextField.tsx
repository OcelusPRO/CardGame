import { useId } from 'react'

interface Props {
  label: string
  value: string
  onChange: (value: string) => void
  placeholder?: string
  maxLength?: number
  hint?: string
  autoFocus?: boolean
  uppercase?: boolean
}

/** A labelled input, because a placeholder alone is not a label. */
export function TextField({
  label,
  value,
  onChange,
  placeholder,
  maxLength,
  hint,
  autoFocus,
  uppercase = false,
}: Props) {
  const id = useId()
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-xs font-semibold uppercase tracking-wider text-ink/60">
        {label}
      </label>
      <input
        id={id}
        value={value}
        autoFocus={autoFocus}
        maxLength={maxLength}
        placeholder={placeholder}
        onChange={(event) => onChange(uppercase ? event.target.value.toUpperCase() : event.target.value)}
        className={`sketch-input bg-paper px-4 py-3 font-display text-lg outline-none transition placeholder:text-ink/35 focus:border-punch ${
          uppercase ? 'tracking-[0.35em]' : ''
        }`}
      />
      {hint && <p className="text-xs text-ink/55">{hint}</p>}
    </div>
  )
}
