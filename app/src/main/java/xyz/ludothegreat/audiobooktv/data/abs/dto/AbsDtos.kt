package xyz.ludothegreat.audiobooktv.data.abs.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PingResponse(val success: Boolean? = null)

@Serializable
data class StatusResponse(
    @SerialName("serverVersion") val serverVersion: String? = null,
    @SerialName("isInit") val isInit: Boolean? = null,
)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(
    val user: AbsUser? = null,
    val error: String? = null,
)

@Serializable
data class AbsUser(
    val id: String,
    val username: String,
    val token: String? = null,
    val type: String? = null,
)
