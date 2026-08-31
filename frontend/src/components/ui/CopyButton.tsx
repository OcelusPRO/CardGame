import { useState } from 'react'
import { Button } from './Button'

interface Props {
  value: string
  label?: string
}

/** Copies a value and says so, falling back silently when the clipboard is blocked. */
export function CopyButton({ value, label = 'Copier le lien' }: Props) {
  const [copied, setCopied] = useState(false)

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(value)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch {
      setCopied(false)
    }
  }

  return (
    <Button variant="ghost" onClick={copy}>
      {copied ? '✅ Copié' : `🔗 ${label}`}
    </Button>
  )
}
