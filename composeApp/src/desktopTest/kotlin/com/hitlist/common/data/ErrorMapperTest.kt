package com.hitlist.common.data

import com.hitlist.common.domain.AppError
import com.hitlist.common.domain.AppResult
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ErrorMapperTest {

    @Test
    fun `maps request timeout to Timeout`() {
        // Arrange
        val exception = mockk<HttpRequestTimeoutException>()
        // Act
        val error = exception.toAppError()
        // Assert
        assertEquals(AppError.Network.Timeout, error)
    }

    @Test
    fun `maps connect timeout to NoConnection`() {
        // Arrange
        val exception = mockk<ConnectTimeoutException>()
        // Act
        val error = exception.toAppError()
        // Assert
        assertEquals(AppError.Network.NoConnection, error)
    }

    @Test
    fun `maps client request exception to Http with status code`() {
        // Arrange
        val ex = mockk<ClientRequestException>()
        every { ex.response } returns mockk { every { status } returns HttpStatusCode.NotFound }

        // Act
        val error = ex.toAppError()

        // Assert
        assertEquals(AppError.Network.Http(404), error)
    }

    @Test
    fun `maps server response exception to Http with status code`() {
        // Arrange
        val ex = mockk<ServerResponseException>()
        every { ex.response } returns mockk { every { status } returns HttpStatusCode.InternalServerError }

        // Act
        val error = ex.toAppError()

        // Assert
        assertEquals(AppError.Network.Http(500), error)
    }

    @Test
    fun `maps unknown throwable to Unexpected`() {
        // Arrange
        val cause = RuntimeException("boom")
        // Act
        val error = cause.toAppError()
        // Assert
        assertIs<AppError.Unexpected>(error)
        assertEquals(cause, error.cause)
    }

    @Test
    fun `toAppResult wraps success`() {
        // Arrange
        val source = Result.success(42)
        // Act
        val result = source.toAppResult()
        // Assert
        assertIs<AppResult.Success<Int>>(result)
        assertEquals(42, result.data)
    }

    @Test
    fun `toAppResult maps failure through error mapper`() {
        // Arrange
        val source = Result.failure<Int>(RuntimeException("boom"))
        // Act
        val result = source.toAppResult()
        // Assert
        assertIs<AppResult.Failure>(result)
        assertIs<AppError.Unexpected>(result.error)
    }
}
