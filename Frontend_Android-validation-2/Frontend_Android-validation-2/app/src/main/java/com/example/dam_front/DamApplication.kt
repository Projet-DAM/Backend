package com.example.dam_front

import android.app.Application
import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.osmdroid.config.Configuration
import java.util.concurrent.TimeUnit

class DamApplication : Application(), ImageLoaderFactory {

    lateinit var notificationRepository: com.example.dam_front.repository.NotificationRepository
        private set

    override fun onCreate() {
        super.onCreate()

        notificationRepository = com.example.dam_front.repository.NotificationRepository(this)

        // ✅ Initialisation OSMDroid (nécessaire sinon carte bug)
        Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Log.d("DamApplication", "✅ OSMDroid initialisé")

        // ✅ Log Coil init
        Log.d("DamApplication", "✅ Application créée - Configuration Coil prête")
    }

    override fun newImageLoader(): ImageLoader {
        Log.d("DamApplication", "✅ Création d'un ImageLoader Coil personnalisé")

        val loggingInterceptor = HttpLoggingInterceptor { message ->
            if (message.startsWith("{")) {
                Log.d("CoilImageLoader", "📄 JSON: $message")
            } else {
                Log.d("CoilImageLoader", message)
            }
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .components {
                add(coil.decode.SvgDecoder.Factory())
            }
            .respectCacheHeaders(false)
            .build()
    }
}
