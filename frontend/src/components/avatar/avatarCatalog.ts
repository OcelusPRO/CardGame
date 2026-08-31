import type { AvatarInput } from '../../api/types'

export interface AvatarStyle {
  id: string
  label: string
}

/** The two halves are picked independently, exactly like dressing a paper doll. */
export const TOP_STYLES: AvatarStyle[] = [
  { id: 'head-round', label: 'Rond' },
  { id: 'head-square', label: 'Carré' },
  { id: 'head-egg', label: 'Œuf' },
  { id: 'head-blob', label: 'Blob' },
  { id: 'head-punk', label: 'Punk' },
  { id: 'head-alien', label: 'Alien' },
]

export const BOTTOM_STYLES: AvatarStyle[] = [
  { id: 'body-tee', label: 'T-shirt' },
  { id: 'body-hoodie', label: 'Hoodie' },
  { id: 'body-suit', label: 'Costume' },
  { id: 'body-tank', label: 'Débardeur' },
  { id: 'body-cape', label: 'Cape' },
  { id: 'body-stripes', label: 'Marinière' },
]

export const PALETTE: string[] = [
  '#ff2e88',
  '#ffd23f',
  '#2ee6a8',
  '#4cc9f0',
  '#9b5de5',
  '#ff8c42',
  '#f6f2ee',
  '#4a3f6b',
]

export const DEFAULT_AVATAR: AvatarInput = {
  topStyleId: 'head-round',
  topColor: '#ffd23f',
  bottomStyleId: 'body-tee',
  bottomColor: '#ff2e88',
}

/** Used by the "surprise me" button, and to give newcomers a non-empty avatar. */
export function randomAvatar(): AvatarInput {
  return {
    topStyleId: pick(TOP_STYLES).id,
    topColor: pick(PALETTE),
    bottomStyleId: pick(BOTTOM_STYLES).id,
    bottomColor: pick(PALETTE),
  }
}

function pick<T>(values: T[]): T {
  return values[Math.floor(Math.random() * values.length)]
}
