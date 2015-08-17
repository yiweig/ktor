package org.jetbrains.ktor.routing

import org.jetbrains.ktor.application.*
import org.jetbrains.ktor.binding.*
import org.jetbrains.ktor.context.*
import org.jetbrains.ktor.locations.*
import java.util.*
import kotlin.reflect.jvm.*

class Routing() : RoutingEntry(parent = null) {
    val conversionService: ConversionService = DefaultConversionService()
    val bindingService = BindingService().apply {
        policy(context::class, BindingPolicy.Default)
        policy(location::class, BindingPolicy.Default)
    }

    inner class RoutingBindingContext(val request: RoutingApplicationRequest) : BindingContext {
        override fun getValue(parameter: BindingParameter): Any? {
            if (parameter.type.javaType == RoutingApplicationRequest::class.java )
                return request
            return conversionService.fromRequest(request, parameter.name, parameter.type.javaType, parameter.isOptional)
        }
    }

    data class Key<T : Any>(val name: String)

    val services = hashMapOf<Key<*>, Any>()
    fun addService<T : Any>(key: Key<T>, service: T) {
        services.put(key, service)
    }

    fun getService<T : Any>(key: Key<T>): T {
        val service = services[key] ?: throw UnsupportedOperationException("Cannot find service for key $key")
        return service as T
    }

    fun installInto(application: Application) {
        application.handler.intercept { request, next -> interceptor(request, next) }
    }

    private fun interceptor(request: ApplicationRequest, next: (ApplicationRequest) -> ApplicationRequestStatus): ApplicationRequestStatus {
        val resolveContext = RoutingResolveContext(request.requestLine, request.parameters, request.headers)
        val resolveResult = resolve(resolveContext)
        return when {
            resolveResult.succeeded -> {
                val chain = arrayListOf<RoutingInterceptor>()
                var current: RoutingEntry? = resolveResult.entry
                while (current != null) {
                    chain.addAll(0, current.interceptors)
                    current = current.parent
                }

                val handlers = resolveResult.entry.handlers
                val routingApplicationRequest = RoutingApplicationRequest(request, resolveResult)
                processChain(routingApplicationRequest, chain, handlers)
            }
            else -> next(request)
        }
    }

    private fun processChain(request: RoutingApplicationRequest, interceptors: List<RoutingInterceptor>, handlers: ArrayList<RoutingHandler<*, *>>): ApplicationRequestStatus {
        fun handle(index: Int, request: RoutingApplicationRequest): ApplicationRequestStatus {
            when {
                index < interceptors.size() -> {
                    val interceptor = interceptors[index].function
                    return interceptor(request) { request -> handle(index + 1, request) }
                }
                else -> {
                    for (handler in handlers) {
                        val handlerFunction = handler.function as Function2<Any, Any, ApplicationRequestStatus>

                        val contextType = handler.contextType
                        val context: Any = if (contextType == RoutingApplicationRequest::class)
                            request
                        else
                            bindingService.create(contextType, RoutingBindingContext(request))

                        val dataType = handler.dataType
                        val data: Any = if (dataType == Unit::class)
                            Unit
                        else
                            bindingService.create(dataType, RoutingBindingContext(request))

                        val handlerResult = handlerFunction(context, data)
                        if (handlerResult != ApplicationRequestStatus.Unhandled)
                            return handlerResult
                    }
                    return ApplicationRequestStatus.Unhandled
                }
            }
        }

        return handle(0, request)
    }
}

fun RoutingEntry.getService<T : Any>(key: Routing.Key<T>): T {
    return if (this is Routing)
        getService(key)
    else
        if (parent == null)
            throw UnsupportedOperationException("Services cannot be obtained from dangling route entries")
        else
            parent.getService(key)
}