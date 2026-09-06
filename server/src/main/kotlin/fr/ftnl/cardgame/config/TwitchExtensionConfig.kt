package fr.ftnl.cardgame.config

/**
 * The Twitch extension: the panel under a stream, and the overlay on top of it, from which
 * a viewer proposes a card and pays for it in bits.
 *
 * An extension has its **own** client id and secret, issued on the extension's page of the
 * developer console — they are not the ones the sign in button uses. The secret is handed
 * over base64 encoded, and it is what both the viewer token and the bits receipt are
 * signed with; without it nothing the panel sends can be trusted, so the whole surface
 * stays closed.
 */
data class TwitchExtensionConfig(
    val clientId: String = "",
    /** Base64, exactly as the console shows it. */
    val secret: String = "",
    /**
     * Bits products, as `sku:amount` pairs — `card_100:100,card_500:500`. A receipt is
     * checked against this list rather than against whatever the panel claims it paid, so
     * renaming a product on Twitch cannot quietly make proposals free.
     */
    val products: Map<String, Int> = emptyMap(),
) {
    val enabled: Boolean get() = clientId.isNotBlank() && secret.isNotBlank()

    /** What a receipt for [sku] is worth, or null when it is not a product of ours. */
    fun bitsFor(sku: String): Int? = products[sku]

    companion object {
        /** Reads `sku:amount` pairs, ignoring anything that is not one. */
        fun productsOf(raw: String): Map<String, Int> = raw.split(',')
            .mapNotNull { entry ->
                val sku = entry.substringBefore(':').trim()
                val amount = entry.substringAfter(':', missingDelimiterValue = "").trim().toIntOrNull()
                if (sku.isBlank() || amount == null || amount <= 0) null else sku to amount
            }
            .toMap()
    }
}
