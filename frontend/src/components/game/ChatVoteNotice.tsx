interface Props {
  channels: string[]
  /** How many viewers have been counted so far, all watched chats together. */
  viewers: number
}

/**
 * The instructions the chat is playing by, shown on the table itself so the streamer has
 * something to point at — and so the players know a voice other than theirs is coming.
 */
export function ChatVoteNotice({ channels, viewers }: Props) {
  if (channels.length === 0) return null

  return (
    <div className="sketch flex flex-col gap-1 bg-[#9146FF]/12 px-4 py-3 text-sm">
      <p className="font-display text-base font-black text-[#772ce8]">
        Le tchat vote&nbsp;! Tapez le numéro de la réponse.
      </p>
      <p className="text-ink/70">
        Sur {channels.length > 1 ? 'les tchats de' : 'le tchat de'}{' '}
        <span className="font-semibold">{channels.join(', ')}</span>. Chaque tchat pèse une
        voix&nbsp;: celle de sa majorité, un seul vote par spectateur. Le vote reste ouvert
        jusqu&apos;à la fin du temps.
      </p>
      <p aria-live="polite" className="font-semibold text-ink/60">
        {viewers === 0
          ? 'Aucun vote du tchat pour le moment.'
          : `${viewers} vote(s) du tchat comptabilisé(s).`}
      </p>
    </div>
  )
}
