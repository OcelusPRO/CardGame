package fr.ftnl.cardgame.config

/**
 * How the server sits behind whatever is in front of it.
 *
 * [trustProxyHeaders] decides whether `X-Forwarded-For` names the caller. It is off by
 * default on purpose: a client can write that header itself, so trusting it on a server
 * reachable directly hands every rate limit a free bypass. Turn it on only when a proxy
 * you control is the one and only way in, and is overwriting the header.
 */
data class HttpConfig(val trustProxyHeaders: Boolean = false)

/**
 * The Prometheus endpoint. Left open when [token] is blank, since it carries counters and
 * nothing else; set the token to require `Authorization: Bearer <token>` on a scrape.
 */
data class MetricsConfig(val token: String = "") {
    val guarded: Boolean get() = token.isNotBlank()
}
