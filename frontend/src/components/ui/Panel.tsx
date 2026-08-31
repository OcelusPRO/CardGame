import type { ReactNode } from 'react'

interface Props {
  children: ReactNode
  title?: string
  actions?: ReactNode
  className?: string
}

/** A frosted block, the only container shape used across the app. */
export function Panel({ children, title, actions, className = '' }: Props) {
  return (
    <section
      className={`rounded-3xl bg-white/5 p-5 ring-1 ring-white/10 backdrop-blur-sm sm:p-6 ${className}`}
    >
      {(title || actions) && (
        <header className="mb-4 flex flex-wrap items-center justify-between gap-3">
          {title && <h2 className="font-display text-xl font-bold">{title}</h2>}
          {actions}
        </header>
      )}
      {children}
    </section>
  )
}
