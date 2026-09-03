package fr.ftnl.cardgame.config

/**
 * How a host earns access to the packs marked "interdit aux mineurs" without being on the
 * allowlist: a Discord account at least [minAccountAgeDays] old is trusted as an adult.
 * The account's age comes for free from its snowflake id, so this needs no extra call and
 * no prompt. Set to `0` to switch the heuristic off and fall back to the allowlist alone.
 */
data class AdultAccessConfig(val minAccountAgeDays: Int) {
    val trustsAccountAge: Boolean get() = minAccountAgeDays > 0
    val minAccountAgeMillis: Long get() = minAccountAgeDays.toLong() * 24 * 60 * 60 * 1000
}
