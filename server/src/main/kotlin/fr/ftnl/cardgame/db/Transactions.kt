package fr.ftnl.cardgame.db

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

/** Runs blocking JDBC work off the event loop, so a slow query never stalls a socket. */
suspend fun <T> dbQuery(block: JdbcTransaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }
