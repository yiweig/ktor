package org.jetbrains.ktor.tests.http

import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.content.*
import org.jetbrains.ktor.http.*
import org.jetbrains.ktor.testing.*
import org.jetbrains.ktor.tests.*
import org.junit.*
import java.io.*
import java.net.*
import kotlin.test.*

class FindContainingZipFileTest {
    @Test
    fun testSimpleJar() {
        assertEquals("/dist/app.jar", findContainingZipFile(URI("jar:file:/dist/app.jar/")).path.replace('\\', '/'))
    }

    @Test
    fun testNestedJar() {
        assertEquals("/dist/app.jar", findContainingZipFile(URI("jar:jar:file:/dist/app.jar!/my/jar.jar!/")).path.replace('\\', '/'))
    }

    @Test
    fun testEscapedChars() {
        assertEquals("/Program Files/app.jar", findContainingZipFile(URI("jar:file:/Program%20Files/app.jar/")).path.replace('\\', '/'))
    }
}

class StaticContentTest {
    @Test
    fun testStaticContent() {
        withTestApplication {
            application.intercept { next ->
                val resolved = sequenceOf(
                        { resolveClasspathResource("", "org.jetbrains.ktor.tests.http") },
                        { resolveClasspathResource("", "java.util") },
                        { resolveLocalFile("", listOf(File("test"), File("ktor-core/test")).first { it.exists() }) }
                ).map { it() }.firstOrNull { it != null }

                if (resolved == null) {
                    next()
                } else {
                    response.send(resolved)
                }
            }

            handleRequest(HttpMethod.Get, "/StaticContentTest.class").let { result ->
                assertEquals(ApplicationCallResult.Handled, result.requestResult)
            }
            handleRequest(HttpMethod.Get, "/ArrayList.class").let { result ->
                assertEquals(ApplicationCallResult.Handled, result.requestResult)
            }
            handleRequest(HttpMethod.Get, "/ArrayList.class2").let { result ->
                assertEquals(ApplicationCallResult.Unhandled, result.requestResult)
            }
            handleRequest(HttpMethod.Get, "/org/jetbrains/ktor/tests/http/StaticContentTest.kt").let { result ->
                assertEquals(ApplicationCallResult.Handled, result.requestResult)
            }
        }
    }
}
