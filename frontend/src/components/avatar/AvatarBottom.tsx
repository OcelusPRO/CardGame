interface Props {
  styleId: string
  color: string
}

const INK = '#2b1e3f'
const PAPER = '#fffcf5'

/** The same pen as the head, so both halves look drawn in one sitting. */
const PEN = {
  stroke: INK,
  strokeWidth: 3,
  strokeLinejoin: 'round',
  strokeLinecap: 'round',
} as const

/** The lower half of an avatar: shoulders and outfit. */
export function AvatarBottom({ styleId, color }: Props) {
  switch (styleId) {
    case 'body-hoodie':
      return (
        <g>
          <path d="M22 120v-22c0-14 12-22 28-22s28 8 28 22v22z" fill={color} {...PEN} />
          <path d="M36 76q14 14 28 0" fill="none" stroke={INK} strokeWidth="3" strokeLinecap="round" />
        </g>
      )
    case 'body-suit':
      return (
        <g>
          <path d="M22 120v-20c0-14 12-24 28-24s28 10 28 24v20z" fill={color} {...PEN} />
          <path d="M50 76 41 92l9 6 9-6z" fill={PAPER} {...PEN} />
          <path d="M50 92l4 22h-8z" fill={INK} {...PEN} />
        </g>
      )
    case 'body-tank':
      return (
        <g>
          <path d="M28 120v-20c0-12 10-20 22-20s22 8 22 20v20z" fill={color} {...PEN} />
          <path d="M42 82v-8M58 82v-8" fill="none" stroke={INK} strokeWidth="4" strokeLinecap="round" />
        </g>
      )
    case 'body-cape':
      return (
        <g>
          <path d="M14 120 26 82q24 16 48 0l12 38z" fill={color} opacity="0.75" {...PEN} />
          <path d="M28 120v-20c0-12 10-20 22-20s22 8 22 20v20z" fill={color} {...PEN} />
        </g>
      )
    case 'body-stripes':
      return (
        <g>
          <path d="M22 120v-22c0-13 12-22 28-22s28 9 28 22v22z" fill={color} {...PEN} />
          <g fill="none" stroke={INK} strokeWidth="3" strokeLinecap="round" opacity="0.7">
            <path d="M24 90h52M23 102h54M22 114h56" />
          </g>
        </g>
      )
    default:
      return <path d="M22 120v-22c0-13 12-22 28-22s28 9 28 22v22z" fill={color} {...PEN} />
  }
}
