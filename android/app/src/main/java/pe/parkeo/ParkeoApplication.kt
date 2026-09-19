package pe.parkeo

import android.app.Application
import pe.parkeo.di.AppContainer
import pe.parkeo.di.DefaultAppContainer

class ParkeoApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(applicationContext)
    }
}
