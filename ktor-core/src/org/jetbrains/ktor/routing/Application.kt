package org.jetbrains.ktor.routing

import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.http.*
import java.util.*

open class RoutingApplicationRequest(applicationRequest: ApplicationRequest,
                                     val resolveResult: RoutingResolveResult) : ApplicationRequest by applicationRequest {

    override val parameters: Map<String, List<String>>

    init {
        val result = HashMap<String, MutableList<String>>()
        for ((key, values) in applicationRequest.parameters) {
            result.getOrPut(key, { arrayListOf() }).addAll(values)
        }
        for ((key, values) in resolveResult.values) {
            if (!result.containsKey(key)) {
                // HACK: should think about strategy of merging params and resolution values
                result.getOrPut(key, { arrayListOf() }).addAll(values)
            }
        }
        parameters = result
    }
}

public fun Application.routing(body: RoutingEntry.() -> Unit) {
    val table = Routing()
    table.body()
    interceptRoute(table)
}

fun Application.interceptRoute(routing: RoutingEntry) {
    handler.intercept { request, next ->
        val resolveContext = RoutingResolveContext(request.requestLine, request.parameters, request.headers)
        val resolveResult = routing.resolve(resolveContext)
        when {
            resolveResult.succeeded -> {
                val chain = arrayListOf<RoutingInterceptor>()
                var current: RoutingEntry? = resolveResult.entry
                while (current != null) {
                    chain.addAll(0, current.interceptors)
                    current = current.parent
                }

                val handlers = resolveResult.entry.handlers
                val routingApplicationRequest = RoutingApplicationRequest(request, resolveResult)
                processChain(chain, routingApplicationRequest, handlers)
            }
            else -> next(request)
        }
    }
}

private fun processChain(interceptors: List<RoutingInterceptor>, request: RoutingApplicationRequest, handlers: ArrayList<(RoutingApplicationRequest) -> ApplicationRequestStatus>): ApplicationRequestStatus {
    fun handle(index: Int, request: RoutingApplicationRequest): ApplicationRequestStatus {
        when (index) {
            in interceptors.indices -> {
                return interceptors[index].function(request) { request -> handle(index + 1, request) }
            }
            else -> {
                for (handler in handlers) {
                    val handlerResult = handler(request)
                    if (handlerResult != ApplicationRequestStatus.Unhandled)
                        return handlerResult
                }
                return ApplicationRequestStatus.Unhandled
            }
        }
    }

    return handle(0, request)
}

public fun RoutingEntry.respond(body: ApplicationResponse.() -> ApplicationRequestStatus) {
    handle {
        respond {
            body()
        }
    }
}

fun RoutingEntry.methodAndPath(method: HttpMethod, path: String, body: RoutingEntry.() -> Unit) {
    method(method) {
        path(path) {
            body()
        }
    }
}

fun RoutingEntry.contentType(contentType: ContentType, build: RoutingEntry.() -> Unit) {
    header("Accept", "${contentType.contentType}/${contentType.contentSubtype}", build)
}


public fun RoutingEntry.get(path: String, body: RoutingEntry.() -> Unit): Unit = methodAndPath(HttpMethod.Get, path, body)
public fun RoutingEntry.put(path: String, body: RoutingEntry.() -> Unit): Unit = methodAndPath(HttpMethod.Put, path, body)
public fun RoutingEntry.delete(path: String, body: RoutingEntry.() -> Unit): Unit = methodAndPath(HttpMethod.Delete, path, body)
public fun RoutingEntry.post(path: String, body: RoutingEntry.() -> Unit): Unit = methodAndPath(HttpMethod.Post, path, body)
public fun RoutingEntry.options(path: String, body: RoutingEntry.() -> Unit): Unit = methodAndPath(HttpMethod.Options, path, body)

public fun RoutingEntry.get(body: RoutingEntry.() -> Unit): Unit = method(HttpMethod.Get, body)
public fun RoutingEntry.put(body: RoutingEntry.() -> Unit): Unit = method(HttpMethod.Put, body)
public fun RoutingEntry.delete(body: RoutingEntry.() -> Unit): Unit = method(HttpMethod.Delete, body)
public fun RoutingEntry.post(body: RoutingEntry.() -> Unit): Unit = method(HttpMethod.Post, body)
public fun RoutingEntry.options(body: RoutingEntry.() -> Unit): Unit = method(HttpMethod.Options, body)

