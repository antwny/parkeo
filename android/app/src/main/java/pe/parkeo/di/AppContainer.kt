package pe.parkeo.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import pe.parkeo.BuildConfig
import pe.parkeo.data.local.SessionDataStore
import pe.parkeo.data.remote.api.*
import pe.parkeo.data.remote.interceptor.AuthInterceptor
import pe.parkeo.data.repository.*
import pe.parkeo.domain.repository.*
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

interface AppContainer {
    val sessionDataStore: SessionDataStore
    val authRepository: AuthRepository
    val parkingRepository: ParkingRepository
    val vehicleRepository: VehicleRepository
    val reservationRepository: ReservationRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val json: Json by lazy {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }

    override val sessionDataStore: SessionDataStore by lazy {
        SessionDataStore(context)
    }

    private val authInterceptor: AuthInterceptor by lazy {
        AuthInterceptor(sessionDataStore)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }

    private val parkingApi: ParkingApi by lazy {
        retrofit.create(ParkingApi::class.java)
    }

    private val vehicleApi: VehicleApi by lazy {
        retrofit.create(VehicleApi::class.java)
    }

    private val reservationApi: ReservationApi by lazy {
        retrofit.create(ReservationApi::class.java)
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(authApi, sessionDataStore)
    }

    override val parkingRepository: ParkingRepository by lazy {
        ParkingRepositoryImpl(parkingApi)
    }

    override val vehicleRepository: VehicleRepository by lazy {
        VehicleRepositoryImpl(vehicleApi)
    }

    override val reservationRepository: ReservationRepository by lazy {
        ReservationRepositoryImpl(reservationApi)
    }
}
