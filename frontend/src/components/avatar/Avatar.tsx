import { useId } from 'react'
import type { AvatarView } from '../../api/types'
import { AvatarBottom } from './AvatarBottom'
import { AvatarTop } from './AvatarTop'

interface Props {
  avatar: AvatarView
  size?: number
  className?: string
  title?: string
}

/**
 * The two halves stacked in one SVG. When the player signed in with Discord, their
 * picture replaces the face inside the head, which is the whole point of the option.
 */
export function Avatar({ avatar, size = 64, className = '', title }: Props) {
  const clipId = useId()
  return (
    <svg
      viewBox="0 0 100 120"
      width={size}
      height={size * 1.2}
      className={className}
      role="img"
      aria-label={title ?? 'Avatar'}
    >
      <AvatarBottom styleId={avatar.bottom.styleId} color={avatar.bottom.color} />
      <AvatarTop styleId={avatar.top.styleId} color={avatar.top.color} />
      {avatar.discordAvatarUrl && (
        <g>
          <defs>
            <clipPath id={clipId}>
              <circle cx="50" cy="46" r="20" />
            </clipPath>
          </defs>
          <image
            href={avatar.discordAvatarUrl}
            x="30"
            y="26"
            width="40"
            height="40"
            clipPath={`url(#${clipId})`}
            preserveAspectRatio="xMidYMid slice"
          />
          <circle cx="50" cy="46" r="20" fill="none" stroke={avatar.top.color} strokeWidth="4" />
        </g>
      )}
    </svg>
  )
}
