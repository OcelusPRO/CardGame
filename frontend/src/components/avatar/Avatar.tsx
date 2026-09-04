import { useId } from 'react'
import type { AvatarView } from '../../api/types'
import { AvatarBottom } from './AvatarBottom'
import { AvatarTop } from './AvatarTop'
import { sketchSeed } from './sketchFilter'

interface Props {
  avatar: AvatarView
  size?: number
  className?: string
  title?: string
}

/**
 * The two halves stacked in one SVG, drawn through a wobble filter so the outlines look
 * traced by hand rather than plotted. The profile picture stays out of the filter: a
 * displaced photo reads as a glitch, not as a drawing.
 */
export function Avatar({ avatar, size = 64, className = '', title }: Props) {
  const uid = useId()
  const clipId = `${uid}-clip`
  const roughId = `${uid}-rough`
  const seed = sketchSeed(avatar.top.styleId, avatar.top.color, avatar.bottom.styleId)
  // The same seed also decides how coarse and how strong the wobble is, so two avatars
  // are not merely displaced differently, they look drawn with different pens.
  const frequency = (0.022 + (seed % 13) * 0.0022).toFixed(4)
  const strength = (2.4 + (seed % 7) * 0.32).toFixed(2)

  return (
    <svg
      viewBox="0 0 100 120"
      width={size}
      height={size * 1.2}
      className={className}
      role="img"
      aria-label={title ?? 'Avatar'}
    >
      <defs>
        <filter id={roughId} x="-15%" y="-15%" width="130%" height="130%">
          <feTurbulence type="fractalNoise" baseFrequency={frequency} numOctaves="2" seed={seed} result="noise" />
          <feDisplacementMap in="SourceGraphic" in2="noise" scale={strength} xChannelSelector="R" yChannelSelector="G" />
        </filter>
        <clipPath id={clipId}>
          <circle cx="50" cy="46" r="20" />
        </clipPath>
      </defs>

      <g filter={`url(#${roughId})`}>
        <AvatarBottom styleId={avatar.bottom.styleId} color={avatar.bottom.color} />
        <AvatarTop styleId={avatar.top.styleId} color={avatar.top.color} faceless={Boolean(avatar.pictureUrl)} />
      </g>

      {avatar.pictureUrl && (
        <g>
          <image
            href={avatar.pictureUrl}
            x="30"
            y="26"
            width="40"
            height="40"
            clipPath={`url(#${clipId})`}
            preserveAspectRatio="xMidYMid slice"
          />
          <circle cx="50" cy="46" r="20" fill="none" stroke="#2b1e3f" strokeWidth="3" />
        </g>
      )}
    </svg>
  )
}
