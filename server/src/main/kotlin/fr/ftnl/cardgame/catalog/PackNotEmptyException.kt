package fr.ftnl.cardgame.catalog

/** Raised when an administrator tries to drop a pack that still holds cards. */
class PackNotEmptyException(packId: String) :
    IllegalStateException("Pack $packId still holds cards")
