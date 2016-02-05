package kweet

import com.mchange.v2.c3p0.*
import freemarker.cache.*
import freemarker.template.*
import kweet.dao.*
import kweet.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.content.*
import org.jetbrains.ktor.features.*
import org.jetbrains.ktor.freemarker.*
import org.jetbrains.ktor.http.*
import org.jetbrains.ktor.locations.*
import org.jetbrains.ktor.routing.*
import org.jetbrains.ktor.sessions.*
import org.jetbrains.ktor.util.*
import java.io.*
import javax.crypto.*
import javax.crypto.spec.*

@location("/")
class Index()

@location("/post-new")
data class PostNew(val text: String = "")

@location("/kweet/{id}/delete")
data class KweetDelete(val id: Int)

@location("/kweet/{id}")
data class ViewKweet(val id: Int)

@location("/user/{user}")
data class UserPage(val user: String)

@location("/register")
data class Register(val userId: String = "", val displayName: String = "", val email: String = "", val password: String = "", val error: String = "")

@location("/login")
data class Login(val userId: String = "", val password: String = "", val error: String = "")

@location("/logout")
class Logout()

data class Session(val userId: String)

class KweetApp(config: ApplicationConfig) : Application(config) {
    val key = hex("6819b57a326945c1968f45236589")
    val dir = File("target/db")
    val pool = ComboPooledDataSource()
    val hmacKey = SecretKeySpec(key, "HmacSHA1")

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
        withSessions<Session> {
            withCookieByValue {
                settings = SessionCookiesSettings(transformers = listOf(SessionCookieTransformerMessageAuthentication(key)))
            }
        }

        routing {
            get<Index> {
                val user = sessionOrNull<Session>()?.let { dao.user(it.userId) }
                val top = dao.top(10).map { dao.getKweet(it) } // TODO pass through cache
                val latest = dao.latest(10).map { dao.getKweet(it) }
                val etagString = user?.userId.toString() + "," + top.joinToString { it.id.toString() } + latest.joinToString { it.id.toString() }
                val etag = etagString.hashCode()

                response.send(FreeMarkerContent("index.ftl", mapOf("top" to top, "latest" to latest, "user" to user), etag.toString()))
            }
            get<PostNew> {
                val user = sessionOrNull<Session>()?.let { dao.user(it.userId) }

                if (user == null) {
                    redirect(Login())
                } else {
                    response.send(FreeMarkerContent("new-kweet.ftl", mapOf("user" to user), user?.userId ?: ""))
                }
            }
            post<PostNew> {
                val user = sessionOrNull<Session>()?.let { dao.user(it.userId) }
                if (user == null) {
                    redirect(Login())
                } else {
                    val id = dao.createKweet(user.userId, it.text, null)
                    redirect(ViewKweet(id))
                }
            }
            post<KweetDelete> {
                val user = sessionOrNull<Session>()?.let { dao.user(it.userId) }
                val kweet = dao.getKweet(it.id)

                if (kweet.userId != user?.userId) {
                    redirect(ViewKweet(it.id))
                } else {
                    dao.deleteKweet(it.id)
                    redirect(Index())
                }
            }
            get<UserPage> {
                val user = sessionOrNull<Session>()?.let { dao.user(it.userId) }
                val pageUser = dao.user(it.user)

                if (pageUser == null) {
                    response.sendError(HttpStatusCode.NotFound, "User ${it.user} doesn't exist")
                } else {
                    val kweets = dao.userKweets(it.user).map { dao.getKweet(it) }

                    response.send(FreeMarkerContent("user.ftl", mapOf("user" to user, "pageUser" to pageUser, "kweets" to kweets), user?.userId ?: ""))
                }
            }
            get<ViewKweet> {
                val user = sessionOrNull<Session>()?.let { dao.user(it.userId) }

                response.send(FreeMarkerContent("view-kweet.ftl", mapOf("user" to user, "kweet" to dao.getKweet(it.id)), user?.userId ?: ""))
            }

            get<Login> {
                val user = sessionOrNull<Session>()?.let { dao.user(it.userId) }

                if (user != null) {
                    redirect(UserPage(user.userId))
                } else {
                    response.send(FreeMarkerContent("login.ftl", mapOf("userId" to it.userId, "error" to it.error), ""))
                }
            }
            post<Login> {
                val user = sessionOrNull<Session>()?.let { dao.user(it.userId) }

                if (user != null) {
                    redirect(UserPage(user.userId))
                } else {
                    val login = when {
                        it.userId.length < 4 -> null
                        it.password.length < 6 -> null
                        !userNameValid(it.userId) -> null
                        else -> dao.user(it.userId, hash(it.password))
                    }

                    if (login == null) {
                        redirect(it.copy(password = "", error = "Invalid username or password"))
                    } else {
                        session(Session(login.userId))
                        redirect(UserPage(login.userId))
                    }
                }
            }
            get<Logout> {
                clearSession()
                redirect(Index())
            }

            method(HttpMethod.Post) {
                post<Register> {
                    val user = sessionOrNull<Session>()?.let { dao.user(it.userId) }
                    if (user != null) {
                        redirect(UserPage(user.userId))
                    } else {
                        if (it.password.length < 6) {
                            redirect(it.copy(error = "Password should be at least 6 characters long", password = ""))
                        } else if (it.userId.length < 4) {
                            redirect(it.copy(error = "Login should be at least 4 characters long", password = ""))
                        } else if (!userNameValid(it.userId)) {
                            redirect(it.copy(error = "Login should be consists of digits, letters, dots or underscores", password = ""))
                        } else if (dao.user(it.userId) != null) {
                            redirect(it.copy(error = "User with the following login is already registered", password = ""))
                        } else {
                            val newUser = User(it.userId, it.email, it.displayName)
                            val hash = hash(it.password)

                            try {
                                dao.createUser(newUser, hash)

                                session(Session(newUser.userId))
                                redirect(UserPage(newUser.userId))
                            } catch (e: Throwable) {
                                if (dao.user(it.userId) != null) {
                                    redirect(it.copy(error = "User with the following login is already registered", password = ""))
                                } else {
                                    application.config.log.error("Failed to register user", e)
                                    redirect(it.copy(error = "Failed to register", password = ""))
                                }
                            }
                        }
                    }
                }
            }
            method(HttpMethod.Get) {
                get<Register> {
                    val user = sessionOrNull<Session>()?.let { dao.user(it.userId) }
                    if (user != null) {
                        redirect(UserPage(user.userId))
                    } else {
                        response.send(FreeMarkerContent("register.ftl", mapOf("pageUser" to User(it.userId, it.email, it.displayName), "error" to it.error), ""))
                    }
                }
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

    private fun hash(password: String): String {
        val hmac = Mac.getInstance("HmacSHA1")
        hmac.init(hmacKey)
        return hex(hmac.doFinal(password.toByteArray(Charsets.UTF_8)))
    }

    private val userIdPattern = "[a-zA-Z0-9_\\.]+".toRegex()
    private fun userNameValid(userId: String) = userId.matches(userIdPattern)
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