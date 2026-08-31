package fr.ftnl.cardgame.api.view

import fr.ftnl.cardgame.api.dto.AvatarInput
import fr.ftnl.cardgame.api.dto.AvatarPartView
import fr.ftnl.cardgame.api.dto.AvatarView
import fr.ftnl.cardgame.domain.player.Avatar
import fr.ftnl.cardgame.domain.player.AvatarPart

/** Translates avatars between the browser payload and the domain value. */
object AvatarMapper {

    fun toDomain(input: AvatarInput, discordAvatarUrl: String?): Avatar = Avatar(
        top = AvatarPart(input.topStyleId, input.topColor),
        bottom = AvatarPart(input.bottomStyleId, input.bottomColor),
        discordAvatarUrl = discordAvatarUrl,
    )

    fun toView(avatar: Avatar): AvatarView = AvatarView(
        top = AvatarPartView(avatar.top.styleId, avatar.top.color),
        bottom = AvatarPartView(avatar.bottom.styleId, avatar.bottom.color),
        discordAvatarUrl = avatar.discordAvatarUrl,
    )
}
