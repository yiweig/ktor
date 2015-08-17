package org.jetbrains.ktor.binding

import java.lang.reflect.*
import kotlin.reflect.*
import kotlin.reflect.jvm.*

private data class BindingInfo(val type: KClass<*>,
                               val policy: BindingPolicy,
                               val constructor: KFunction<Any>,
                               val parameters: Map<String, BindingParameter>)

data class BindingParameter(val name: String, val type: KType, val isOptional: Boolean)

interface BindingPolicy {
    object Default : BindingPolicy {
        override fun getParameterName(parameter: KParameter): String = TODO()
    }

    fun getParameterName(parameter: KParameter): String
}

interface BindingContext {
    fun getValue(parameter: BindingParameter): Any?
}

public class BindingService {
    private val binders = hashMapOf<KClass<*>, BindingInfo>()
    private val policies = hashMapOf<KClass<*>, BindingPolicy>()

    fun policy(annotationType: KClass<*>, policy: BindingPolicy) {
        policies.put(annotationType, policy)
    }

    private fun getOrCreateBindingInfo(type: KClass<*>): BindingInfo {
        return binders.getOrPut(type) {
            val policy = type.annotations.map { policies[it.annotationType().kotlin] }.filterNotNull().single()
            val constructor: KFunction<Any> = type.primaryConstructor ?: type.constructors.single()
            val parameters = constructor.parameters.map { parameter ->
                val parameterName = parameter.name ?: policy.getParameterName(parameter)
                BindingParameter(parameterName, parameter.type, parameter.type.isMarkedNullable)
            }.toMap { it.name }

            BindingInfo(type, policy, constructor, parameters)
        }
    }

    fun create(type: KClass<*>, context: BindingContext): Any {
        val info = getOrCreateBindingInfo(type)
        return create(info, context)
    }

    fun create(info: BindingInfo, context: BindingContext): Any {
        val arguments = info.parameters.map {
            resolveParameter(context, it.value)
        }.toTypedArray()
        return info.constructor.call(*arguments)
    }

    private fun resolveParameter(context: BindingContext, parameter: BindingParameter): Any? {
        val type = parameter.type.rawType()
        if (type != null) {
            val policy = type.annotations.asSequence().map { policies[it.annotationType().kotlin] }.filterNotNull().firstOrNull()
            if (policy != null) {
                return create(type, context)
            }
        }
        return context.getValue(parameter)
    }

    fun KType.rawType(): KClass<*>? {
        val javaType: Type = javaType
        return when (javaType) {
            is Class<*> -> javaType.kotlin
            else -> null
        }
    }
}

