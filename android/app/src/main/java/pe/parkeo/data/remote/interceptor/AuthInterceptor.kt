// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Implementación del módulo Parkeo identificado por el paquete y nombre de esta clase.
package pe.parkeo.data.remote.interceptor

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import pe.parkeo.data.local.SessionDataStore

class AuthInterceptor(
    private val sessionDataStore: SessionDataStore
) : Interceptor {
    /** Ejecuta la operación indicada por el nombre: intercept. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { sessionDataStore.getAccessToken() }
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
