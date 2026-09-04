package fr.ftnl.cardgame.config

/**
 * How a host earns access to the packs marked "interdit aux mineurs" without being on the
 * allowlist: an account at least [minAccountAgeDays] old — three years by default — is
 * trusted as an adult, whether it is a Discord or a Twitch one. The age comes for free
 * with the sign in, so this needs no extra call and no prompt. Set to `0` to switch the
 * heuristic off and fall back to the allowlist alone.
 */
data class AdultAccessConfig(val minAccountAgeDays: Int) {
    val trustsAccountAge: Boolean get() = minAccountAgeDays > 0
    val minAccountAgeMillis: Long get() = minAccountAgeDays.toLong() * 24 * 60 * 60 * 1000
}
