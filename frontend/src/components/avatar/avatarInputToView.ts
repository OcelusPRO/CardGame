import type { AvatarInput, AvatarView } from '../../api/types'

/** The builder edits an input shape, the renderer wants a view shape. */
export function avatarInputToView(input: AvatarInput, discordAvatarUrl?: string): AvatarView {
  return {
    top: { styleId: input.topStyleId, color: input.topColor },
    bottom: { styleId: input.bottomStyleId, color: input.bottomColor },
    discordAvatarUrl,
  }
}
