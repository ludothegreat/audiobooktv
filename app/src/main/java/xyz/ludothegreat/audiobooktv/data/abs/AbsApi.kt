package xyz.ludothegreat.audiobooktv.data.abs

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import xyz.ludothegreat.audiobooktv.data.abs.dto.ItemsInProgressResponse
import xyz.ludothegreat.audiobooktv.data.abs.dto.LibrariesResponse
import xyz.ludothegreat.audiobooktv.data.abs.dto.LibraryItemsResponse
import xyz.ludothegreat.audiobooktv.data.abs.dto.LoginRequest
import xyz.ludothegreat.audiobooktv.data.abs.dto.LoginResponse
import xyz.ludothegreat.audiobooktv.data.abs.dto.MeResponse
import xyz.ludothegreat.audiobooktv.data.abs.dto.MediaProgress
import xyz.ludothegreat.audiobooktv.data.abs.dto.PingResponse
import xyz.ludothegreat.audiobooktv.data.abs.dto.StatusResponse

interface AbsApi {
    @GET("ping")
    suspend fun ping(): PingResponse

    @GET("status")
    suspend fun status(): StatusResponse

    @POST("login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("api/libraries")
    suspend fun libraries(): LibrariesResponse

    @GET("api/libraries/{libraryId}/items")
    suspend fun libraryItems(
        @Path("libraryId") libraryId: String,
        @Query("limit") limit: Int = 0,
        @Query("sort") sort: String = "media.metadata.title",
        @Query("desc") desc: Int = 0,
    ): LibraryItemsResponse

    @GET("api/me")
    suspend fun me(): MeResponse

    @GET("api/me/items-in-progress")
    suspend fun itemsInProgress(): ItemsInProgressResponse

    @GET("api/me/progress/{itemId}")
    suspend fun progress(@Path("itemId") itemId: String): MediaProgress

    @POST("api/items/{itemId}/play")
    suspend fun openPlayback(
        @Path("itemId") itemId: String,
        @Body body: xyz.ludothegreat.audiobooktv.data.abs.dto.PlayRequest,
    ): xyz.ludothegreat.audiobooktv.data.abs.dto.PlaybackSession

    @POST("api/session/{sessionId}/sync")
    suspend fun syncSession(
        @Path("sessionId") sessionId: String,
        @Body body: xyz.ludothegreat.audiobooktv.data.abs.dto.SessionSyncRequest,
    )

    @POST("api/session/{sessionId}/close")
    suspend fun closeSession(@Path("sessionId") sessionId: String)

    @POST("api/me/item/{itemId}/bookmark")
    suspend fun createBookmark(
        @Path("itemId") itemId: String,
        @Body body: xyz.ludothegreat.audiobooktv.data.abs.dto.CreateBookmarkRequest,
    ): xyz.ludothegreat.audiobooktv.data.abs.dto.AbsBookmark

    // ABS addresses a bookmark by its time, not by an id: PATCH looks the
    // bookmark up via body.time and updates the title (404 when no bookmark
    // exists at that time); DELETE takes the time as the path segment.
    // Verified against a live 2.35.1 server.
    @PATCH("api/me/item/{itemId}/bookmark")
    suspend fun updateBookmark(
        @Path("itemId") itemId: String,
        @Body body: xyz.ludothegreat.audiobooktv.data.abs.dto.CreateBookmarkRequest,
    ): xyz.ludothegreat.audiobooktv.data.abs.dto.AbsBookmark

    @DELETE("api/me/item/{itemId}/bookmark/{time}")
    suspend fun deleteBookmark(
        @Path("itemId") itemId: String,
        @Path("time") timeSec: Long,
    )
}
