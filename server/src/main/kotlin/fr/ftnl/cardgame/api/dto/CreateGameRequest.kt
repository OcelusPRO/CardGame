package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** Payload of `POST /api/games`. */
@Serializable
data class CreateGameRequest(
    val nickname: String,
    val avatar: AvatarInput,
    val settings: GameSettingsInput? = null,
)
