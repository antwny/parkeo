/*
 * PARKeo · Android
 * Archivo: NavRoutes.kt
 * Capa: navegación (catálogo central de rutas).
 * Propósito: evitar cadenas mágicas dispersas y servir como contrato entre el grafo y las
 * pantallas que solicitan una transición.
 * Conceptos clave: rutas de Navigation Compose, segmentos, parámetros de ruta, comodines
 * `{parkingId}`, tipos de argumento, convención de nombres y funciones constructoras.
 * Flujo: cada destino del NavHost usa una constante o una función de este objeto; las rutas
 * con datos reciben el identificador y lo codifican en el segmento correspondiente, que el
 * grafo vuelve a leer como Long.
 * Relación con otros archivos: ParkeoNavGraph.kt consume estas rutas en `composable` y
 * `navArgument`; MainScreen y ProfileScreen no navegan directamente, sino que invocan
 * callbacks que el grafo traduce a llamadas del NavController.
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones de Compose y navegación).
 */
package pe.parkeo.ui.navigation

/**
 * Catálogo de rutas de PARKeo.
 *
 * Las rutas sin parámetros son cadenas literales en minúsculas y sin barras iniciales. Las
 * rutas con parámetros declaran el nombre del argumento entre llaves, por ejemplo
 * `parking/{parkingId}`. El nombre entre llaves debe coincidir exactamente con el que se
 * registra en `navArgument` dentro de `ParkeoNavGraph`; de lo contrario, el argumento llega
 * nulo o se produce un error de destino.
 *
 * Convención de nombres:
 * - `splash`, `onboarding`, `login`, `register`: flujo de entrada.
 * - `home`: contenedor principal con pestañas según el rol.
 * - `parking/{parkingId}`: detalle de una cochera concreta.
 * - `reservation/create/{parkingId}`: formulario de reserva para una cochera.
 * - `reservations`, `vehicles`, `profile`: secciones del usuario.
 *
 * Los roles no se modelan como parámetro de ruta: `MainScreen` lee el rol del perfil
 * autenticado y decide qué pestañas mostrar. Esto evita que un rol viaje en una URL y que
 * la navegación dependa de un dato que puede cambiar. La autorización real, de todos modos,
 * corresponde al backend; ocultar una pestaña no protege un endpoint.
 */
object NavRoutes {
    /** Ruta inicial: pantalla de marca que decide entre onboarding y home. */
    const val SPLASH = "splash"

    /** Presentación de las ventajas de PARKeo antes del registro o del login. */
    const val ONBOARDING = "onboarding"

    /** Formulario de inicio de sesión con correo y contraseña. */
    const val LOGIN = "login"

    /** Formulario de creación de cuenta; se abre desde el login. */
    const val REGISTER = "register"

    /** Contenedor principal autenticado: pestañas y contenido según el rol del usuario. */
    const val HOME = "home"

    /**
     * Plantilla del detalle de una cochera.
     *
     * `{parkingId}` es un marcador, no un valor real: el grafo lo declara con
     * `NavType.LongType` y la función `parkingDetail` lo sustituye al navegar.
     */
    const val PARKING_DETAIL = "parking/{parkingId}"

    /**
     * Plantilla del formulario para crear una reserva en una cochera.
     *
     * Comparte el nombre de argumento `parkingId` con el detalle, pero se registra en un
     * destino independiente para que la pantalla de reserva reciba el identificador.
     */
    const val RESERVATION_CREATE = "reservation/create/{parkingId}"

    /** Listado de reservas del usuario o del operador, según el rol. */
    const val RESERVATIONS = "reservations"

    /** Listado y gestión de vehículos registrados por el cliente. */
    const val VEHICLES = "vehicles"

    /** Perfil del usuario con datos, accesos por rol y cierre de sesión. */
    const val PROFILE = "profile"

    /**
     * Construye la ruta concreta del detalle de una cochera.
     *
     * Sustituye el marcador `{parkingId}` por el identificador recibido. El resultado, por
     * ejemplo `parking/12`, es el que se entrega a `navController.navigate`.
     *
     * @param id identificador numérico de la cochera que se quiere mostrar.
     * @return ruta concreta lista para navegar.
     */
    fun parkingDetail(id: Long) = "parking/$id"

    /**
     * Construye la ruta concreta del formulario de reserva.
     *
     * Sustituye `{parkingId}` por el identificador de la cochera. El grafo vuelve a leerlo
     * como `Long` y lo pasa a `ReservationCreateScreen`.
     *
     * @param parkingId identificador numérico de la cochera donde se reservará.
     * @return ruta concreta lista para navegar.
     */
    fun reservationCreate(parkingId: Long) = "reservation/create/$parkingId"
}
