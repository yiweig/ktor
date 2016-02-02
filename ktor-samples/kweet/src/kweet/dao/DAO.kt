package kweet.dao

import kweet.model.Kweet
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

    fun getKweet(id: Int) = db.transaction {
        val row = Kweets.select { Kweets.id.eq(id) }.single()
        Kweet(id, row[Kweets.user], row[Kweets.text], row[Kweets.date])
    }

    fun top(count: Int = 10): List<Int> = db.transaction {
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

    fun latest(count: Int = 10) = db.transaction {

    }

    override fun close() {
    }
}