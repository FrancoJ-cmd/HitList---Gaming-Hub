package com.hitlist.common.domain

data class Stale<out T>(val value: T, val isStale: Boolean)
