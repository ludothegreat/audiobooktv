package xyz.ludothegreat.audiobooktv.data.abs

import okhttp3.OkHttpClient
import xyz.ludothegreat.audiobooktv.data.auth.SessionManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AbsApiProvider @Inject constructor(
    private val sessionManager: SessionManager,
) {
    @Volatile private var cachedTarget: AbsTarget? = null
    @Volatile private var cachedApi: AbsApi? = null
    @Volatile private var cachedOkHttp: OkHttpClient? = null

    fun get(): AbsApi {
        ensureFresh()
        return cachedApi ?: error("No active session; call connect() first.")
    }

    fun okHttp(): OkHttpClient {
        ensureFresh()
        return cachedOkHttp ?: error("No active session; call connect() first.")
    }

    private fun ensureFresh() {
        val target = sessionManager.currentTarget() ?: run {
            cachedTarget = null
            cachedApi = null
            cachedOkHttp = null
            return
        }
        if (cachedApi != null && cachedTarget == target) return
        val okhttp = AbsClientFactory.okHttp(target)
        cachedOkHttp = okhttp
        cachedApi = AbsClientFactory.retrofit(target).create(AbsApi::class.java)
        cachedTarget = target
    }

    fun invalidate() {
        cachedTarget = null
        cachedApi = null
        cachedOkHttp = null
    }
}
