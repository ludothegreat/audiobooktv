package xyz.ludothegreat.audiobooktv.data.abs

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import xyz.ludothegreat.audiobooktv.data.abs.dto.LoginRequest
import xyz.ludothegreat.audiobooktv.data.abs.dto.LoginResponse
import xyz.ludothegreat.audiobooktv.data.abs.dto.PingResponse
import xyz.ludothegreat.audiobooktv.data.abs.dto.StatusResponse

interface AbsApi {
    @GET("ping")
    suspend fun ping(): PingResponse

    @GET("status")
    suspend fun status(): StatusResponse

    @POST("login")
    suspend fun login(@Body body: LoginRequest): LoginResponse
}
