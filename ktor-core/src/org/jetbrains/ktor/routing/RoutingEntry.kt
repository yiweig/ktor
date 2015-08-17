package org.jetbrains.ktor.routing

import org.jetbrains.ktor.application.*
import java.util.*
import kotlin.reflect.*

data class RoutingNode(val selector: RoutingSelector, val entry: RoutingEntry)
data class RoutingInterceptor(val function: (RoutingApplicationRequest, (RoutingApplicationRequest) -> ApplicationRequestStatus) -> ApplicationRequestStatus)
data class RoutingHandler<TContext>(val contextType: KClass<TContext>, val function: (TContext) -> ApplicationRequestStatus)

open class RoutingEntry(val parent: RoutingEntry?) {
    val children = ArrayList<RoutingNode> ()
    val interceptors = ArrayList<RoutingInterceptor>()
    val handlers = ArrayList<RoutingHandler<*>>()

    public fun select(selector: RoutingSelector): RoutingEntry {
        val existingEntry = children.firstOrNull { it.selector.equals(selector) }?.entry
        if (existingEntry == null) {
            val entry = createChild()
            children.add(RoutingNode(selector, entry))
            return entry
        }
        return existingEntry
    }

    protected fun resolve(request: RoutingResolveContext, segmentIndex: Int, current: RoutingResolveResult): RoutingResolveResult {
        var failEntry: RoutingEntry? = null
        for ((selector, entry) in children) {
            val result = selector.evaluate(request, segmentIndex)
            if (result.succeeded) {
                for ((key, values) in result.values) {
                    current.values.getOrPut(key, { arrayListOf() }).addAll(values)
                }
                val subtreeResult = entry.resolve(request, segmentIndex + result.segmentIncrement, current)
                if (subtreeResult.succeeded) {
                    return subtreeResult
                } else {
                    failEntry = subtreeResult.entry
                }
            }
        }

        when (segmentIndex) {
            request.path.parts.size() -> return RoutingResolveResult(true, this, current.values)
            else -> return RoutingResolveResult(false, failEntry ?: this)
        }
    }

    public fun resolve(request: RoutingResolveContext): RoutingResolveResult {
        return resolve(request, 0, RoutingResolveResult(false, this, HashMap<String, MutableList<String>>()))
    }

    public fun addInterceptor(interceptor: (RoutingApplicationRequest, (RoutingApplicationRequest) -> ApplicationRequestStatus) -> ApplicationRequestStatus) {
        interceptors.add(RoutingInterceptor(interceptor))
    }

    public inline fun addHandler<reified TContext : Any>(noinline handler: TContext.() -> ApplicationRequestStatus) {
        handlers.add(RoutingHandler<TContext>(TContext::class, handler))
    }

    open fun createChild(): RoutingEntry = RoutingEntry(this)
}