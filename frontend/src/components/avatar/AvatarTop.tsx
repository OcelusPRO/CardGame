interface Props {
  styleId: string
  color: string
}

const INK = '#150726'

/** The upper half of an avatar: the head shape and its face. */
export function AvatarTop({ styleId, color }: Props) {
  return (
    <g>
      {shapeOf(styleId, color)}
      <g fill={INK}>
        <circle cx="41" cy="46" r="3.4" />
        <circle cx="59" cy="46" r="3.4" />
      </g>
      <path
        d="M41 57 q9 8 18 0"
        fill="none"
        stroke={INK}
        strokeWidth="3"
        strokeLinecap="round"
      />
    </g>
  )
}

function shapeOf(styleId: string, color: string) {
  switch (styleId) {
    case 'head-square':
      return <rect x="26" y="20" width="48" height="50" rx="12" fill={color} />
    case 'head-egg':
      return <ellipse cx="50" cy="45" rx="22" ry="27" fill={color} />
    case 'head-blob':
      return (
        <path
          d="M50 17c17 0 26 12 26 26 0 16-11 28-26 28S24 59 24 43c0-15 9-26 26-26z"
          fill={color}
        />
      )
    case 'head-punk':
      return (
        <g>
          <path d="M32 24 38 8 44 24 50 6 56 24 62 10 68 26z" fill={color} />
          <circle cx="50" cy="46" r="24" fill={color} />
        </g>
      )
    case 'head-alien':
      return (
        <g>
          <path d="M50 12v-8M34 18l-6-8M66 18l6-8" stroke={color} strokeWidth="4" strokeLinecap="round" />
          <ellipse cx="50" cy="46" rx="25" ry="23" fill={color} />
        </g>
      )
    default:
      return <circle cx="50" cy="46" r="24" fill={color} />
  }
}
