package fr.ftnl.cardgame.api.dto

import kotlinx.serialization.Serializable

/** Payload of `POST /api/games/{code}/players`. */
@Serializable
data class JoinGameRequest(
    val nickname: String,
    val avatar: AvatarInput,
)
