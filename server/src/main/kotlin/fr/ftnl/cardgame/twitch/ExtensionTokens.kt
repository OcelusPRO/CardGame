package fr.ftnl.cardgame.twitch

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import fr.ftnl.cardgame.config.TwitchExtensionConfig
import org.slf4j.LoggerFactory
import java.util.Base64

/** Who the panel says is talking, once the signature says the panel is telling the truth. */
data class ExtensionViewer(
    /** The channel the panel is displayed on: the streamer, not the viewer. */
    val channelId: String,
    /**
     * The viewer themselves. Null when they have not granted the extension their identity,
     * which is the default and perfectly normal — an opaque id still tells two viewers
     * apart, and telling them apart is all this needs.
     */
    val userId: String?,
    /** Always present: `U…` for a signed in viewer, `A…` for an anonymous one. */
    val opaqueId: String,
    val role: String,
) {
    val isBroadcaster: Boolean get() = role == "broadcaster"

    /** The handle a proposal is attributed to, which never needs to be the real account. */
    val handle: String get() = userId ?: opaqueId
}

/** A bits purchase Twitch says actually happened. */
data class BitsReceipt(
    val sku: String,
    val transactionId: String,
    val userId: String?,
)

/**
 * Checks what the extension panel sends.
 *
 * Everything arriving from a panel is a claim by code running in a viewer's browser, so
 * none of it counts until Twitch's own signature backs it: the viewer token that says
 * which channel this is, and the bits receipt that says the cheer was really paid. Both
 * are signed with the extension secret, which only Twitch and this server hold.
 *
 * `HMAC256` is pinned deliberately. Reading the algorithm out of the token is the classic
 * way these checks get walked past, so the algorithm is decided here and the token gets
 * no say in it.
 */
class ExtensionTokens(private val config: TwitchExtensionConfig) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val verifier by lazy {
        val secret = runCatching { Base64.getDecoder().decode(config.secret) }.getOrNull()
        secret?.let { JWT.require(Algorithm.HMAC256(it)).build() }
    }

    /** The viewer behind an `Authorization: Bearer` token, or null when it does not check out. */
    fun viewer(token: String?): ExtensionViewer? {
        val decoded = decode(token) ?: return null
        val channelId = decoded.getClaim("channel_id").asString()?.takeIf { it.isNotBlank() }
            ?: return null
        val opaqueId = decoded.getClaim("opaque_user_id").asString()?.takeIf { it.isNotBlank() }
            ?: return null
        return ExtensionViewer(
            channelId = channelId,
            userId = decoded.getClaim("user_id").asString()?.takeIf { it.isNotBlank() },
            opaqueId = opaqueId,
            role = decoded.getClaim("role").asString().orEmpty(),
        )
    }

    /**
     * The purchase behind a bits receipt. Twitch signs the receipt with the same secret,
     * so a viewer cannot mint one — and the amount is looked up from the configured
     * products rather than read out of the token.
     */
    fun receipt(token: String?): BitsReceipt? {
        val decoded = decode(token) ?: return null
        val product = decoded.getClaim("data").asMap()?.get("product") as? Map<*, *> ?: return null
        val sku = product["sku"] as? String ?: return null
        val transactionId = (decoded.getClaim("data").asMap()?.get("transactionId") as? String)
            ?: return null
        return BitsReceipt(
            sku = sku,
            transactionId = transactionId,
            userId = decoded.getClaim("data").asMap()?.get("userId") as? String,
        )
    }

    private fun decode(token: String?): DecodedJWT? {
        if (!config.enabled) return null
        val raw = token?.removePrefix("Bearer ")?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val checker = verifier ?: run {
            log.error("The Twitch extension secret is not valid base64; the panel stays closed")
            return null
        }
        return runCatching { checker.verify(raw) }.getOrNull()
    }
}
