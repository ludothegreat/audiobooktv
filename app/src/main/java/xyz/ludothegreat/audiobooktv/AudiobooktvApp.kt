package xyz.ludothegreat.audiobooktv

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.hilt.android.HiltAndroidApp
import xyz.ludothegreat.audiobooktv.data.abs.AbsApiProvider
import javax.inject.Inject

@HiltAndroidApp
class AudiobooktvApp : Application(), SingletonImageLoader.Factory {

    @Inject lateinit var apiProvider: AbsApiProvider

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(callFactory = { apiProvider.okHttp() }),
                )
            }
            .build()
    }
}
