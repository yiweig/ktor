package kweet.dao

import kweet.model.*
import org.jetbrains.exposed.sql.*
import org.joda.time.*
import java.io.*

class DAO(val db: Database = Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")): Closeable {
    constructor(dir: File) : this(Database.connect("jdbc:h2:file:${dir.canonicalFile.absolutePath}", driver = "org.h2.Driver"))

    fun init() {
        db.transaction {
            create(Users, Kweets)
        }
    }

    fun countReplies(id: Int): Int {
        return db.transaction {
            Kweets.slice(Kweets.id.count()).select {
                Kweets.replyTo.eq(id)
            }.single()[Kweets.id.count()]
        }
    }

    fun createKweet(user: String, text: String, replyTo: Int? = null): Int {
        return db.transaction {
            Kweets.insert {
                it[Kweets.user] = user
                it[Kweets.date] = DateTime.now()
                it[Kweets.replyTo] = replyTo
                it[Kweets.text] = text
            }.generatedKey ?: throw IllegalStateException("No generated key returned")
        }
    }

    fun deleteKweet(id: Int) {
        db.transaction {
            Kweets.deleteWhere { Kweets.id.eq(id) }
        }
    }

    fun getKweet(id: Int) = db.transaction {
        val row = Kweets.select { Kweets.id.eq(id) }.single()
        Kweet(id, row[Kweets.user], row[Kweets.text], row[Kweets.date])
    }

    fun userKweets(userId: String) = db.transaction {
        Kweets.slice(Kweets.id).select { Kweets.user.eq(userId) }.orderBy(Kweets.date, false).limit(100).map { it[Kweets.id] }
    }

    fun user(userId: String, hash: String? = null) = db.transaction {
        Users.select { Users.id.eq(userId) }
                .map {
                    if (hash == null || it[Users.passwordHash] == hash) {
                        User(userId, it[Users.email], it[Users.displayName])
                    } else {
                        null
                    }
                }
                .singleOrNull()
    }

    fun createUser(user: User, hash: String) = db.transaction {
        Users.insert {
            it[Users.id] = user.userId
            it[Users.displayName] = user.displayName
            it[Users.email] = user.email
            it[Users.passwordHash] = hash
        }
        Unit
    }

    fun top(count: Int = 10): List<Int> = db.transaction {
        // note: in a real application you shouldn't do it like this
        //   as it may cause database outages on big data
        //   so this implementation is just for demo purposes

        val k2 = Kweets.alias("k2")
        Kweets.join(k2, JoinType.LEFT, Kweets.id, k2[Kweets.replyTo])
                .slice(Kweets.id, k2[Kweets.id].count())
                .selectAll()
                .groupBy(Kweets.id)
                .orderBy(k2[Kweets.id].count(), isAsc = false)
//                .having { k2[Kweets.id].count().greater(0) }
                .limit(count)
                .map { it[Kweets.id] }
    }

    fun latest(count: Int = 10): List<Int> = db.transaction {
        var attempt = 0
        var allCount: Int? = null

        for (minutes in generateSequence(2) { it * it }) {
            attempt++

            val dt = DateTime.now().minusMinutes(minutes)

            val all = Kweets.slice(Kweets.id)
                    .select { Kweets.date.greater(dt) }
                    .orderBy(Kweets.date, false)
                    .limit(count)
                    .map { it[Kweets.id] }

            if (all.size >= count) {
                return@transaction all
            }
            if (attempt > 10 && allCount == null) {
                allCount = Kweets.slice(Kweets.id.count()).selectAll().count()
                if (allCount <= count) {
                    return@transaction Kweets.slice(Kweets.id).selectAll().map { it[Kweets.id] }
                }
            }
        }

        emptyList()
    }

    override fun close() {
    }
}

fun IntProgression.z() {

}