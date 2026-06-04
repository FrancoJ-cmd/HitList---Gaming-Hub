package com.hitlist.data.mapper

import com.hitlist.domain.error.AppError
import com.hitlist.domain.result.AppResult
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException

fun Throwable.toAppError(): AppError = when (this) {
    is HttpRequestTimeoutException -> AppError.Network.Timeout
    is ConnectTimeoutException -> AppError.Network.NoConnection
    is ClientRequestException -> AppError.Network.Http(response.status.value)
    is ServerResponseException -> AppError.Network.Http(response.status.value)
    else -> AppError.Unexpected(this)
}

fun <T> Result<T>.toAppResult(): AppResult<T> = fold(
    onSuccess = { AppResult.Success(it) },
    onFailure = { AppResult.Failure(it.toAppError()) }
)
