// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Punto de entrada o inicialización del proceso; integra la configuración principal de la aplicación.
package pe.parkeo

import android.app.Application
import pe.parkeo.di.AppContainer
import pe.parkeo.di.DefaultAppContainer

class ParkeoApplication : Application() {
    lateinit var container: AppContainer

    /** Gestiona el evento de ciclo de vida o interacción indicado: on create. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(applicationContext)
    }
}
