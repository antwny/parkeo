/*
 * PARKeo · Android
 * Archivo: ParkeoApplication.kt
 * Capa: aplicación (inicialización del proceso y de las dependencias globales).
 * Propósito: crear una única instancia del contenedor de dependencias que viva mientras
 * viva el proceso de la aplicación.
 * Conceptos clave: Application, ciclo de vida del proceso, onCreate, singleton, contexto
 * de aplicación, inyección de dependencias manual, lateinit y contenedor AppContainer.
 * Flujo: Android crea esta clase antes que cualquier Activity o servicio; onCreate construye
 * DefaultAppContainer con el contexto de la aplicación. Después MainActivity y ParkeoApp
 * obtienen ese contenedor y lo entregan a ParkeoViewModelFactory para crear ViewModels.
 * Relación con otros archivos: AndroidManifest.xml la registra con android:name;
 * di/AppContainer.kt define el contrato y la implementación; ParkeoApp.kt la recupera
 * mediante el contexto; ui/viewmodel/ViewModelFactory.kt consume sus repositorios.
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones de arquitectura y DI manual).
 */
package pe.parkeo

import android.app.Application
import pe.parkeo.di.AppContainer
import pe.parkeo.di.DefaultAppContainer

/**
 * Subclase de `Application` que representa el proceso completo de PARKeo.
 *
 * Android crea una sola instancia de esta clase por proceso, antes de lanzar cualquier
 * Activity, y la mantiene viva mientras el proceso exista. Por eso es el lugar adecuado
 * para construir dependencias costosas o compartidas, como DataStore, Retrofit y los
 * repositorios. El manifiesto la enlaza con `android:name=".ParkeoApplication"`; sin esa
 * declaración, Android usaría la clase `Application` genérica y este código no se ejecutaría.
 *
 * `container` es un singleton de facto dentro del proceso: se inicializa una vez en
 * `onCreate` y las pantallas lo consultan a través de `ParkeoViewModelFactory`. No es un
 * singleton global de JVM ni sobrevive a la muerte del proceso; si Android lo recrea, se
 * vuelve a ejecutar `onCreate` y se construye un contenedor nuevo.
 */
class ParkeoApplication : Application() {
    /**
     * Contenedor de dependencias de la aplicación.
     *
     * Se declara `lateinit` porque Android instancia la clase con un constructor sin
     * argumentos y asigna el valor en `onCreate`. Después de esa inicialización es de solo
     * lectura en la práctica: las pantallas no lo reemplazan, solo leen repositorios y
     * servicios. Mantener aquí el contenedor evita crear clientes HTTP o DataStore por
     * cada pantalla.
     */
    lateinit var container: AppContainer

    /**
     * Inicializa el contenedor manual de dependencias.
     *
     * `super.onCreate()` permite que `Application` complete su propia inicialización.
     * Luego se construye `DefaultAppContainer` con `applicationContext`, un contexto ligado
     * al proceso que no retiene Activities ni vistas. Los repositorios exponen sus
     * dependencias de forma perezosa (`by lazy`), por lo que la red y el almacenamiento se
     * crean cuando se usan por primera vez.
     *
     * Este método se ejecuta una vez por proceso, no una vez por Activity, y no debe
     * realizar trabajo bloqueante prolongado porque retrasa el arranque de la app.
     */
    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(applicationContext)
    }
}
