package org.jetbrains.ktor.tests.application

import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.context.*
import org.jetbrains.ktor.http.*
import org.jetbrains.ktor.routing.*
import org.jetbrains.ktor.tests.*
import org.junit.*
import kotlin.test.*

class ContextHandlerTest {

    context class EmptyContext()

    Test fun `handler with empty context`() = withTestApplication {
        application.routing {
            handle<EmptyContext, Unit> {
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
            handle<ParameterContext, Unit> {
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

    context class ContextWithRequest(val request: RoutingApplicationRequest, val name: String)

    Test fun `handler with context having request`() = withTestApplication {
        application.routing {
            handle<ContextWithRequest, Unit> {
                assertTrue(this is ContextWithRequest, "it should be ParameterContext")
                assertEquals(name, "John")
                request.respondText("Test")
            }
        }
        on("making a request") {
            val request = handleRequest { uri = "?name=John" }
            it("should be handled") {
                assertEquals(ApplicationRequestStatus.Handled, request.requestResult)
            }
            it("should contain response") {
                assertNotNull(request.response)
                assertEquals(request.response!!.content, "Test")
            }
        }
    }
}