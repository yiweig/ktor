package kweet

import com.mchange.v2.c3p0.*
import freemarker.cache.*
import freemarker.template.*
import kweet.dao.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.content.*
import org.jetbrains.ktor.features.*
import org.jetbrains.ktor.freemarker.*
import org.jetbrains.ktor.http.*
import org.jetbrains.ktor.locations.*
import org.jetbrains.ktor.routing.*
import java.io.*

@location("/")
class Index()

@location("/post-new")
data class PostNew(val text: String = "")

@location("/kweet/{id}")
data class ViewKweet(val id: Int)

class KweetApp(config: ApplicationConfig) : Application(config) {
    val dir = File("target/db")
    val pool = ComboPooledDataSource()

    init {
        pool.driverClass = org.h2.Driver::class.java.name
        pool.jdbcUrl = "jdbc:h2:file:${dir.canonicalFile.absolutePath}"
        pool.user = ""
        pool.password = ""
    }

    val dao = DAO(Database.connect(pool))

    init {
        dao.init()

        install(Locations)
        templating(freemarker {
            Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS).apply {
                templateLoader = ClassTemplateLoader(KweetApp::class.java.classLoader, "templates")
            }
        })

        routing {
            get<Index> {
                val top = dao.top(10).map { dao.getKweet(it) } // TODO pass through cache

                response.send(FreeMarkerContent("index.ftl", mapOf("top" to top), ""))
            }
            get<PostNew> {
                response.send(FreeMarkerContent("new-kweet.ftl", mapOf("" to 0), ""))
            }
            post<PostNew> {
                val id = dao.createKweet("temp", it.text, null)
                redirect(ViewKweet(id))
            }
            get<ViewKweet> {
                response.send(FreeMarkerContent("view-kweet.ftl", mapOf("kweet" to dao.getKweet(it.id)), ""))
            }
        }
    }

    override fun dispose() {
        pool.close()
    }

    private fun ApplicationCall.redirect(location: Any): ApplicationCallResult {
        val host = request.host() ?: "localhost"
        val portSpec = request.port().let { if (it == 80) "" else ":$it" }
        val address = host + portSpec

        return response.sendRedirect("http://$address${application.feature(Locations).href(location)}")
    }
}


fun main(args: Array<String>) {
    // Kweets k, Kweets k2 where k.id = k2.reply_to group by k.id
    val dir = File("target/db")
    val dao = DAO(dir)
    dao.db.connector().apply {
        val pst = prepareStatement("select k.id, count(k2.id) from Kweets k left join Kweets k2 on k.id = k2.reply_to group by k.id")
        //        val pst = prepareStatement("select k.id, k.id from Kweets k")
        val rs = pst.executeQuery()

        while (rs.next()) {
            println("${rs.getInt(1)}, ${rs.getInt(2)}")
        }

        rs.close()
        pst.close()
        close()
    }

    dao.db.transaction {
        logger.addLogger(StdOutSqlLogger())

        val k2 = Kweets.alias("k2")
        Kweets.join(k2, JoinType.LEFT, Kweets.id, k2[Kweets.replyTo])
                .slice(Kweets.id, k2[Kweets.id].count())
                .selectAll()
                .groupBy(Kweets.id)
                .orderBy(k2[Kweets.id].count())
//                .having { k2[Kweets.id].count().greater(0) }
                .forEach {
                    println(it)
                }
    }

}