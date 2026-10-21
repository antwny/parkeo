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
import pe.parkeo.data.remote.interceptor.TokenAuthenticator
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
    val adminRepository: AdminRepository
    val operatorRepository: OperatorRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val json: Json by lazy {
        Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    }

    override val sessionDataStore: SessionDataStore by lazy {
        SessionDataStore(context)
    }

    private val loggingInterceptor: HttpLoggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }
    }

    /** Cliente básico (sin Authenticator) para login / register / refresh. */
    private val bareOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val bareRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(bareOkHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    /** AuthApi sin Authenticator (refresh no debe disparar otro refresh). */
    private val bareAuthApi: AuthApi by lazy {
        bareRetrofit.create(AuthApi::class.java)
    }

    private val tokenAuthenticator: TokenAuthenticator by lazy {
        TokenAuthenticator(sessionDataStore, bareAuthApi)
    }

    private val authInterceptor: AuthInterceptor by lazy {
        AuthInterceptor(sessionDataStore)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(loggingInterceptor)
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

    private val parkingApi: ParkingApi by lazy {
        retrofit.create(ParkingApi::class.java)
    }

    private val vehicleApi: VehicleApi by lazy {
        retrofit.create(VehicleApi::class.java)
    }

    private val reservationApi: ReservationApi by lazy {
        retrofit.create(ReservationApi::class.java)
    }

    private val adminApi: AdminApi by lazy {
        retrofit.create(AdminApi::class.java)
    }

    private val operatorApi: OperatorApi by lazy {
        retrofit.create(OperatorApi::class.java)
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(bareAuthApi, sessionDataStore)
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

    override val adminRepository: AdminRepository by lazy {
        AdminRepositoryImpl(adminApi)
    }

    override val operatorRepository: OperatorRepository by lazy {
        OperatorRepositoryImpl(operatorApi)
    }
}
