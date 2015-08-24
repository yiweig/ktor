package org.jetbrains.ktor.application

import java.util.*

public data class ValuesEntry(val key: String, val values: List<String>)

public class ValuesMap {
    companion object {
        val Empty = ValuesMap().freeze()
    }

    private val map = hashMapOf<String, ArrayList<String>>()
    private var frozen = false

    public fun freeze(): ValuesMap = apply { frozen = true }

    fun get(name: String): List<String>? = map[name]

    fun contains(name: String) = map.containsKey(name)
    fun contains(name: String, value: String) = map[name]?.contains(value) ?: false

    fun appendAll(valuesMap: ValuesMap) {
        if (frozen)
            throw UnsupportedOperationException("ValueMap is frozen and cannot be modified")
        for ((key, values) in valuesMap.map)
            map.getOrPut(key, { arrayListOf() }).addAll(values)
    }

    fun appendAll(key: String, values: Iterable<String>) {
        if (frozen)
            throw UnsupportedOperationException("ValueMap is frozen and cannot be modified")
        map.getOrPut(key, { arrayListOf() }).addAll(values)
    }

    fun append(key: String, value: String) {
        if (frozen)
            throw UnsupportedOperationException("ValueMap is frozen and cannot be modified")
        map.getOrPut(key, { arrayListOf() }).add(value)
    }
}

fun valuesOf(vararg pairs: Pair<String, Iterable<String>>): ValuesMap {
    return ValuesMap().apply {
        for ((key, values) in pairs)
            appendAll(key, values)
    }
}