interface Props {
  /** Whether a chat is actually being read; the notice draws nothing otherwise. */
  active: boolean
  /** How many viewers have been counted so far, all watched chats together. */
  viewers: number
}

/**
 * The instructions the chat is playing by, shown on the table itself so the streamer has
 * something to point at — and so the players know a voice other than theirs is coming.
 *
 * The channel name stays off screen on purpose: it would out a streamer's identity to
 * every player at the table, including ones who found the link rather than being invited.
 */
export function ChatVoteNotice({ active, viewers }: Props) {
  if (!active) return null

  return (
    <div className="sketch flex flex-col gap-1 bg-[#9146FF]/12 px-4 py-3 text-sm">
      <p className="font-display text-base font-black text-[#772ce8]">
        Le tchat vote&nbsp;! Tapez le numéro de la réponse.
      </p>
      <p className="text-ink/70">
        Le tchat décidera du sort de ces réponses. Personne à la table ne vote&nbsp;: chaque
        spectateur pèse une voix, une seule, et la manche reste ouverte jusqu&apos;à la fin du
        temps.
      </p>
      <p aria-live="polite" className="font-semibold text-ink/60">
        {viewers === 0
          ? 'Aucun vote du tchat pour le moment.'
          : `${viewers} vote(s) du tchat comptabilisé(s).`}
      </p>
    </div>
  )
}
