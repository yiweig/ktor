package org.jetbrains.ktor.routing

import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.http.*

public fun Application.routing(body: RoutingEntry.() -> Unit) {
    Routing().apply(body).installInto(this)
}

public fun RoutingEntry.respond(build: ApplicationResponse.() -> ApplicationRequestStatus) {
    handle { respond(build) }
}

fun RoutingEntry.methodAndPath(method: HttpMethod, path: String, build: RoutingEntry.() -> Unit) {
    method(method) { path(path, build) }
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

