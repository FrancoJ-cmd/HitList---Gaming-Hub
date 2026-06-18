package com.hitlist.common.domain

/**
 * Envuelve un dato que puede estar desactualizado: `isStale` es true cuando se devuelve cache
 * vieja como fallback (p. ej. al fallar el fetch fresco). Reemplaza el `Pair<T, Boolean>` que
 * se arrastraba por las capas.
 */
data class Stale<out T>(val value: T, val isStale: Boolean)
