import type { ChatVotesView } from '../../api/types'

interface Props {
  votes: ChatVotesView
}

/**
 * The viewers who picked this answer, faces first. The server only ever sends the first
 * few — the rest of a big chat is a number, because a thousand avatars would say less
 * than "+985 votes".
 */
export function ChatVoterFaces({ votes }: Props) {
  if (votes.count === 0) return null
  const hidden = votes.count - votes.voters.length

  return (
    <div className="mt-3 flex flex-wrap items-center gap-1">
      <div className="flex -space-x-2">
        {votes.voters.map((voter) => (
          <span
            key={voter.id}
            title={voter.name}
            className="inline-flex size-6 items-center justify-center overflow-hidden rounded-full border-2 border-paper bg-[#9146FF] text-[0.6rem] font-black text-white"
          >
            {voter.avatarUrl ? (
              <img src={voter.avatarUrl} alt={voter.name} className="size-full object-cover" />
            ) : (
              initialOf(voter.name)
            )}
          </span>
        ))}
      </div>
      <span className="text-xs font-semibold text-[#772ce8]">
        {hidden > 0 ? `+${hidden} votes` : `${votes.count} vote(s)`}
      </span>
    </div>
  )
}

function initialOf(name: string): string {
  return name.trim().charAt(0).toUpperCase() || '?'
}
