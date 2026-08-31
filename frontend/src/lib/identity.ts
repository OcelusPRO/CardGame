import type { AvatarInput } from '../api/types'
import { DEFAULT_AVATAR } from '../components/avatar/avatarCatalog'

/** What the browser remembers between two soirées, so nobody retypes their pseudo. */
export interface Identity {
  nickname: string
  avatar: AvatarInput
}

export const IDENTITY_KEY = 'cardgame.identity'

export const EMPTY_IDENTITY: Identity = {
  nickname: '',
  avatar: DEFAULT_AVATAR,
}
