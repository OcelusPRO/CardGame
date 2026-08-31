import type { ReactNode } from 'react'

interface Props {
  situation: ReactNode
  children: ReactNode
}

/**
 * The desktop layout of a round: the cards spread across the width on the left, the
 * situation and the action attached to it stay pinned on the right. On a phone the
 * situation comes first, because it has to be read before anything can be chosen.
 */
export function RoundStage({ situation, children }: Props) {
  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_minmax(17rem,22rem)] lg:items-start">
      <div className="order-2 min-w-0 lg:order-1">{children}</div>
      <div className="order-1 mx-auto flex w-full max-w-sm flex-col gap-4 lg:order-2 lg:mx-0 lg:max-w-none lg:sticky lg:top-6">
        {situation}
      </div>
    </div>
  )
}
