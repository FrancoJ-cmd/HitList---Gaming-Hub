package com.hitlist.data.mapper

import com.hitlist.domain.error.AppError
import com.hitlist.domain.result.AppResult
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertEquals

class ErrorMapperTest {

    @Test
    fun `given HttpRequestTimeoutException, maps to Network Timeout`() {
        val error = HttpRequestTimeoutException("url", 10_000)
        val appError = error.toAppError()
        assertIs<AppError.Network.Timeout>(appError)
    }

    @Test
    fun `given ConnectTimeoutException, maps to Network NoConnection`() {
        val error = ConnectTimeoutException("Connection timed out")
        val appError = error.toAppError()
        assertIs<AppError.Network.NoConnection>(appError)
    }

    @Test
    fun `given unknown exception, maps to Unexpected with cause`() {
        val cause = IllegalStateException("boom")
        val appError = cause.toAppError()
        assertIs<AppError.Unexpected>(appError)
        assertEquals(cause, (appError as AppError.Unexpected).cause)
    }

    @Test
    fun `given successful Result, toAppResult returns Success`() {
        val result = Result.success("hello")
        val appResult = result.toAppResult()
        assertIs<AppResult.Success<String>>(appResult)
        assertEquals("hello", appResult.data)
    }

    @Test
    fun `given failed Result with unknown exception, toAppResult returns Failure with Unexpected`() {
        val result = Result.failure<String>(RuntimeException("fail"))
        val appResult = result.toAppResult()
        assertIs<AppResult.Failure>(appResult)
        assertIs<AppError.Unexpected>(appResult.error)
    }
}

