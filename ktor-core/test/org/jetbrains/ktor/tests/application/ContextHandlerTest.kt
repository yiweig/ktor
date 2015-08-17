package org.jetbrains.ktor.tests.application

import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.context.*
import org.jetbrains.ktor.routing.*
import org.jetbrains.ktor.tests.*
import org.junit.*
import kotlin.test.*

class ContextHandlerTest {

    context class EmptyContext()

    Test fun `handler with empty context`() = withTestApplication {
        application.routing {
            addHandler<EmptyContext> {
                assertTrue(this is EmptyContext, "it should be empty")
                ApplicationRequestStatus.Handled
            }
        }
        on("making a request") {
            val request = handleRequest { }
            it("should be handled") {
                assertEquals(ApplicationRequestStatus.Handled, request.requestResult)
            }
            it("should not contain response") {
                assertNull(request.response)
            }
        }
    }

    context class ParameterContext(val name: String)

    Test fun `handler with parameter context`() = withTestApplication {
        application.routing {
            addHandler<ParameterContext> {
                assertTrue(this is ParameterContext, "it should be ParameterContext")
                assertEquals(name, "John")
                ApplicationRequestStatus.Handled
            }
        }
        on("making a request") {
            val request = handleRequest { uri = "?name=John" }
            it("should be handled") {
                assertEquals(ApplicationRequestStatus.Handled, request.requestResult)
            }
            it("should not contain response") {
                assertNull(request.response)
            }
        }
    }
}