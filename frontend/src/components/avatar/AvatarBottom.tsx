interface Props {
  styleId: string
  color: string
}

/** The lower half of an avatar: shoulders and outfit. */
export function AvatarBottom({ styleId, color }: Props) {
  switch (styleId) {
    case 'body-hoodie':
      return (
        <g>
          <path d="M22 120v-22c0-14 12-22 28-22s28 8 28 22v22z" fill={color} />
          <path d="M36 76q14 14 28 0" fill="none" stroke="#150726" strokeWidth="3" opacity="0.35" />
        </g>
      )
    case 'body-suit':
      return (
        <g>
          <path d="M22 120v-20c0-14 12-24 28-24s28 10 28 24v20z" fill={color} />
          <path d="M50 76 41 92l9 6 9-6z" fill="#fdf8f3" />
          <path d="M50 92l4 22h-8z" fill="#150726" />
        </g>
      )
    case 'body-tank':
      return (
        <g>
          <path d="M28 120v-20c0-12 10-20 22-20s22 8 22 20v20z" fill={color} />
          <path d="M42 82v-8M58 82v-8" stroke={color} strokeWidth="6" strokeLinecap="round" />
        </g>
      )
    case 'body-cape':
      return (
        <g>
          <path d="M14 120 26 82q24 16 48 0l12 38z" fill={color} opacity="0.65" />
          <path d="M28 120v-20c0-12 10-20 22-20s22 8 22 20v20z" fill={color} />
        </g>
      )
    case 'body-stripes':
      return (
        <g>
          <path d="M22 120v-22c0-13 12-22 28-22s28 9 28 22v22z" fill={color} />
          <g stroke="#fdf8f3" strokeWidth="4" opacity="0.75">
            <path d="M22 90h56M22 102h56M22 114h56" />
          </g>
        </g>
      )
    default:
      return <path d="M22 120v-22c0-13 12-22 28-22s28 9 28 22v22z" fill={color} />
  }
}
