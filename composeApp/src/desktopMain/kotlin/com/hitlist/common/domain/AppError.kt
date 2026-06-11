package com.hitlist.common.domain

sealed class AppError {
    abstract fun toUserMessage(): String

    sealed class Network : AppError() {
        data object NoConnection : Network() {
            override fun toUserMessage() = "Sin conexión a internet"
        }
        data object Timeout : Network() {
            override fun toUserMessage() = "Tiempo de espera agotado. Intentá de nuevo"
        }
        data class Http(val code: Int) : Network() {
            override fun toUserMessage() = "Error del servidor ($code). Intentá más tarde"
        }
    }

    data class Unexpected(val cause: Throwable? = null) : AppError() {
        override fun toUserMessage() = "Ocurrió un error inesperado"
    }
}
