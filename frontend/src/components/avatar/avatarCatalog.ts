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

/** Poster colours: light enough to sit on paper, saturated enough to stay readable. */
export const PALETTE: string[] = [
  '#ff9ec0',
  '#ffd98a',
  '#8fe3c4',
  '#9fd8f2',
  '#c3a8f5',
  '#ffb389',
  '#fffcf5',
  '#a99ec4',
]

export const DEFAULT_AVATAR: AvatarInput = {
  topStyleId: 'head-round',
  topColor: '#ffd98a',
  bottomStyleId: 'body-tee',
  bottomColor: '#ff9ec0',
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
