package com.faturaapp.data.network

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Retrofit precisa da base URL no momento da construcao, mas o usuario configura o
 * endereco do backend em runtime (tela de Setup). Por isso reconstruimos o client
 * sempre que a URL salva muda, em vez de manter uma instancia singleton fixa.
 */
object ApiClientProvider {

    private val json = Json { ignoreUnknownKeys = true }

    private var cachedBaseUrl: String? = null
    private var cachedApi: FaturaApi? = null

    fun getApi(baseUrl: String): FaturaApi {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        cachedApi?.let {
            if (cachedBaseUrl == normalizedUrl) return it
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        val api = retrofit.create(FaturaApi::class.java)
        cachedBaseUrl = normalizedUrl
        cachedApi = api
        return api
    }
}
