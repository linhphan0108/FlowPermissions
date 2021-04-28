package com.linhphan.flowpermission

import kotlinx.coroutines.flow.*

/**
 * ensure all of the items satisfy the condition.
 */
inline fun <T> Iterable<T>.ensureAll(predicate: (T) -> Boolean): Boolean {
    var result = true
    if (count() == 0){
        result = false
    }
    forEach {
        if (!predicate(it)){
            result = false
            return@forEach
        }
    }
    return result
}

fun <T> Flow<Flow<T>>.flattenConcat(): Flow<T> = flow {
    collect { value ->
        value.collect {
            emit(it)
        }
    }
}

inline fun <reified T> List<Flow<T>>.flattenFlow(): Flow<List<T>> = combine(this@flattenFlow) {
    it.toList()
}