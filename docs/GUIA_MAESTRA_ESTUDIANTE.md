# 🅿️ PARKeo: guía maestra para estudiar Android, Kotlin, backend y base de datos

> [!info] Propósito
> Esta guía acompaña el código de PARKeo para una persona que recién empieza con desarrollo móvil Android, Kotlin, Gradle, Java/Spring y MySQL. Explica qué problema resuelve cada capa, cómo viajan los datos y qué conceptos conviene poder defender en una sustentación. Está escrita para Obsidian: los enlaces `[[#Sección]]` permiten volver rápidamente a los temas.

## 🧭 Mapa de navegación

- [[#🎯 1. Qué es PARKeo y cómo recorrerlo]]
- [[#🏗️ 2. Arquitectura y recorrido de los datos]]
- [[#📱 3. Android y Kotlin desde cero]]
- [[#🧩 4. Interfaz con Jetpack Compose]]
- [[#🧠 5. ViewModel, estado y corrutinas]]
- [[#🌐 6. Retrofit, DTO y repositorios]]
- [[#🧰 7. Gradle y dependencias]]
- [[#☕ 8. Backend Java y Spring Boot]]
- [[#🔐 9. Seguridad y autenticación]]
- [[#🗄️ 10. MySQL, modelo y relaciones]]
- [[#📆 11. Regla crítica: reservar sin conflictos]]
- [[#🧪 12. Pruebas y lectura de errores]]
- [[#🚀 13. Cómo levantar el proyecto]]
- [[#🗂️ 14. Catálogo comentado de todos los archivos versionados]]
- [[#🎓 15. Preguntas para sustentar y respuestas guía]]
- [[#📚 16. Ruta de estudio sugerida]]
- [[#⚠️ 17. Discrepancias y límites que debes explicar]]
- [[#✅ 18. Glosario de bolsillo]]

## 🎯 1. Qué es PARKeo y cómo recorrerlo

PARKeo es un sistema para descubrir estacionamientos, consultar información, registrar vehículos y administrar reservas. En lugar de que el teléfono se conecte directamente a MySQL, existe un servidor intermedio. La app Android presenta la experiencia y valida algunos datos para ayudar al usuario; el backend aplica las reglas que deben ser comunes y confiables; la base de datos conserva la información de manera duradera.

El repositorio se divide en `android/`, `backend/`, `database/`, `docs/` y `scripts/`. Al estudiar, conviene empezar con `README.md`, pasar a `docs/arquitectura.md`, seguir el flujo de autenticación y luego una operación vertical completa, por ejemplo crear una reserva. Una lectura vertical responde: qué toca el usuario, qué estado cambia en la pantalla, qué solicitud HTTP sale, qué controlador la recibe, qué servicio valida, qué repositorio consulta y qué tabla persiste.

El concepto “tiempo real” de la descripción del producto no debe confundirse automáticamente con WebSockets o una transmisión continua. En esta implementación las pantallas consultan una API HTTP; la actualidad de la disponibilidad depende de cuándo se vuelve a consultar. Poder describir con precisión lo que existe es mejor sustentación que repetir una frase comercial.

## 🏗️ 2. Arquitectura y recorrido de los datos

La aplicación sigue una separación por responsabilidades cercana a capas. En Android, `ui/` contiene pantallas y componentes Compose; `ui/viewmodel/` mantiene estado y coordina acciones; `domain/` contiene modelos e interfaces de repositorio; `data/` implementa esas interfaces y habla con almacenamiento local y API remota; `di/` construye las dependencias. “Domain” significa el vocabulario y las operaciones del problema, no una librería especial.

El backend aplica una separación análoga: `controller/` recibe HTTP y transforma parámetros; `service/` implementa reglas de negocio y transacciones; `repository/` consulta y guarda entidades; `entity/` representa persistencia relacional; `dto/request` y `dto/response` forman el contrato de entrada y salida; `security/` valida identidad y token. `config/` conecta componentes de Spring y `exception/` convierte fallos del dominio en respuestas HTTP coherentes.

Ejemplo de consulta de estacionamientos: una pantalla llama a su ViewModel; este ejecuta una función suspendida del repositorio de dominio; la implementación de datos llama a `ParkingApi`; Retrofit codifica la petición; OkHttp añade el bearer token si existe; Spring Security establece la identidad; el controlador delega al servicio; el repositorio JPA ejecuta SQL; el resultado vuelve como DTO y se representa en la pantalla. La respuesta no es una “llamada a una función remota” mágica: es una solicitud HTTP serializada y una respuesta HTTP deserializada.

El contrato entre app y servidor es un punto de acoplamiento: campos, nombres, tipos y reglas deben coincidir. Los DTO de red no deberían confundirse con entidades JPA ni con modelos de dominio. Un DTO puede cambiar para adaptarse a JSON; una entidad tiene anotaciones de persistencia; un modelo de dominio expresa lo que la app necesita sin obligarse a copiar exactamente el esquema SQL.

## 📱 3. Android y Kotlin desde cero

Kotlin es un lenguaje estático que corre en la máquina virtual de Java para Android. El compilador revisa tipos y transforma el código en bytecode/Dex. `val` declara una referencia que no se reasigna; `var` permite reasignarla. Ninguna garantiza que un objeto referenciado sea profundamente inmutable. `String?` es nullable y exige manejar `null`; `String` no permite nulo. Este sistema evita una familia común de errores que en Java terminan en `NullPointerException`.

Una función se declara con `fun`. `suspend fun` puede suspender una corrutina sin bloquear el hilo mientras espera I/O. No significa que se cree un hilo nuevo por cada llamada ni que mágicamente toda función sea asíncrona: debe ejecutarse desde una corrutina o desde otra función suspendida. `when` compara alternativas, `data class` genera utilidades como `copy`, igualdad estructural y `toString`, y `object` crea un singleton.

Las clases usan constructores primarios; por ejemplo `class DefaultAppContainer(private val context: Context)` recibe el contexto al construir el objeto. `private` restringe acceso a la clase o archivo; `override` implementa un miembro de una interfaz o clase base. Una interfaz define un contrato. El contenedor implementa `AppContainer` y expone repositorios sin que la interfaz de usuario tenga que construir OkHttp directamente.

Android ejecuta componentes con ciclos de vida. `MainActivity` es la entrada de UI declarada en el manifiesto. La actividad puede recrearse, por ejemplo al rotar; por ello el estado de pantalla no debería vivir únicamente en una variable local de Activity. `ViewModel` sobrevive a recreaciones de configuración y se limpia cuando deja de usarse su propietario. Permisos como ubicación y notificaciones se solicitan en ejecución; declararlos en el manifiesto no equivale a tener autorización del usuario.

## 🧩 4. Interfaz con Jetpack Compose

Compose es un toolkit declarativo: la pantalla se describe como una función del estado. Una función anotada `@Composable` produce UI; cuando el estado observado cambia, Compose vuelve a ejecutar las partes afectadas para reflejar el nuevo valor. No es una instrucción imperativa para “crear un botón una sola vez”. Un `Modifier` encadena presentación, tamaño, interacción o accesibilidad.

Las pantallas en `ui/screens/` reúnen formularios y contenido de cada flujo; los componentes en `ui/components/` comparten botones, tarjetas, barras, campos y estados vacíos/error. Separar componentes reduce duplicación y ayuda a mantener consistencia. `ui/theme/` define colores, tipografía, formas y dimensiones. Recursos XML bajo `res/` permiten valores alternativos por versión, modo noche y densidad.

Un estado de pantalla puede representar carga, error, éxito o lista vacía. Cuando el ViewModel publica estado mediante `StateFlow`, la pantalla lo observa y muestra la rama apropiada. Los callbacks (`onLogin`, `onReservationClick`) elevan eventos: el componente visual no necesita conocer Retrofit ni la base de datos. La navegación se define en `ParkeoNavGraph.kt` con rutas declaradas en `NavRoutes.kt`.

Los temas oscuro/claro dependen de valores y recursos alternativos. Android selecciona recursos por configuración, como `values-night`. Los iconos en varias densidades evitan que el sistema escale un único bitmap de forma borrosa. Los recursos `.webp` y `.png` son binarios, no admiten comentarios de texto internos; su uso y variantes se documentan aquí y en el manifiesto/configuración.

## 🧠 5. ViewModel, estado y corrutinas

Un ViewModel es un objeto de presentación que conserva datos observables y responde a acciones de UI. No es una Activity ni un repositorio. Por ejemplo `AuthViewModel` llama a `AuthRepository`, actualiza un `MutableStateFlow<AuthUiState>` y expone una vista de solo lectura `StateFlow`. El guion bajo en `_uiState` es una convención para indicar que solo el ViewModel muta el flujo y las pantallas lo observan.

`StateFlow` siempre tiene un valor actual y emite cambios a sus colectores. `Flow` representa una secuencia asíncrona; operadores como `map` transforman emisiones. `stateIn` convierte un flujo en estado compartido y `SharingStarted.WhileSubscribed(5000)` mantiene el upstream activo con una ventana breve después de que desaparezcan suscriptores. La UI debe recoger flujos con APIs compatibles con ciclo de vida cuando corresponda.

`viewModelScope.launch { ... }` inicia una corrutina ligada al ciclo de vida del ViewModel. Cuando este desaparece, el scope cancela el trabajo. Dentro se puede llamar a operaciones de red suspendidas sin bloquear el hilo de UI. `copy` en un data class crea un nuevo valor con campos sustituidos, patrón útil para actualizaciones inmutables. El `Result` del proyecto separa éxito y error para no propagar excepciones de red a cada pantalla.

Un detalle didáctico: los ViewModels reciben interfaces de repositorio, así se pueden sustituir implementaciones y probar la lógica sin conexión real. `ViewModelFactory` construye ViewModels que no tienen constructor vacío y `AppContainer` ensambla dependencias manualmente. Esto es inyección de dependencias básica; el proyecto no requiere asumir que usa Hilt/Dagger.

## 🌐 6. Retrofit, DTO y repositorios

Retrofit crea implementaciones de interfaces API a partir de anotaciones HTTP. `@GET`, `@POST`, `@Path`, `@Query` y `@Body` describen método, ruta y parámetros. Un método `suspend` entrega una respuesta de forma cooperativa. Los DTO anotados para Kotlin Serialization traducen JSON a objetos Kotlin. `ignoreUnknownKeys` tolera campos extra del backend, `isLenient` flexibiliza algunas entradas y `coerceInputValues` permite recuperar ciertos valores inválidos/opcionales según reglas del serializador; ninguna opción corrige nombres o tipos incorrectos de forma universal.

`AppContainer` crea `Json`, `OkHttpClient`, `Retrofit`, APIs y repositorios de manera perezosa (`by lazy`): se inicializan en el primer acceso. `AuthInterceptor` añade la autorización a peticiones. Un interceptor puede inspeccionar/modificar solicitudes y respuestas; el logging de cuerpo en modo debug facilita diagnóstico pero puede exponer datos sensibles en registros locales, por eso la configuración de producción lo desactiva.

Las implementaciones `*RepositoryImpl` traducen entre DTO de red y modelos que usa la app y coordinan almacenamiento local cuando es necesario. `SessionDataStore` usa Preferences DataStore para persistir datos de sesión de forma asíncrona. DataStore no es una base relacional ni un sustituto del servidor. El token local es una credencial: el código de cliente debe tratarlo como secreto y no escribirlo en logs.

La URL `BuildConfig.BASE_URL` cambia según build type/configuración. En emulador, `localhost` dentro del Android virtual apunta al propio emulador, no al equipo anfitrión; normalmente se usa `10.0.2.2`. En teléfono físico se utiliza una IP alcanzable del equipo en la red, además de permitir tráfico según la configuración Android. Una URL con `/` final es importante porque Retrofit requiere base URL con segmento final.

## 🧰 7. Gradle y dependencias

Gradle es el sistema de automatización de construcción: resuelve plugins y bibliotecas, compila código, procesa recursos y empaqueta la app. `settings.gradle.kts` nombra el proyecto e incluye `:app`; `build.gradle.kts` de raíz declara plugins compartidos; `app/build.gradle.kts` configura Android, variantes y dependencias. `.kts` significa que el script usa Kotlin DSL en lugar de Groovy.

`libs.versions.toml` es el catálogo versionado. Define alias para plugins, bibliotecas y versiones en un solo lugar. Una dependencia como `implementation(libs.retrofit)` agrega Retrofit al classpath de compilación/runtime de la app. `testImplementation` agrega dependencias a pruebas JVM; `androidTestImplementation` a pruebas instrumentadas; `debugImplementation` solo a variante debug. `platform(libs.androidx.compose.bom)` importa un BOM que coordina versiones de componentes Compose sin declarar cada una.

`compileSdk` indica APIs contra las que se compila; `minSdk` establece la versión mínima que puede instalar el paquete; `targetSdk` declara el comportamiento de plataforma para el que se preparó la app y afecta ciertas políticas de Android. `applicationId` identifica el paquete instalado y `namespace` organiza referencias generadas. `buildTypes` permite distinguir debug y release. `buildConfigField` genera constantes tipadas para el código.

El wrapper `gradlew`/`gradlew.bat` selecciona la versión de Gradle definida en `gradle/wrapper/gradle-wrapper.properties`, de modo que el equipo use una versión consistente. `gradle.properties` contiene opciones de construcción; `gradle-daemon-jvm.properties` fija características de la JVM del daemon. Los directorios `.gradle/` y `build/` son cachés/resultados generados, no documentación fuente que deba editarse ni incluirse manualmente.

## ☕ 8. Backend Java y Spring Boot

Java es el lenguaje del servidor y Spring Boot proporciona autoconfiguración y componentes web. `ParkeoApplication` es el punto de arranque. `@SpringBootApplication` combina configuración, búsqueda de componentes y autoconfiguración. Spring crea objetos gestionados (beans) y los inyecta por constructor; Lombok `@RequiredArgsConstructor` genera el constructor para campos `final`.

Un controlador REST usa anotaciones como `@RestController`, `@RequestMapping`, `@GetMapping` y `@PostMapping`. Lee partes HTTP: `@PathVariable` para el identificador en la ruta, `@RequestParam` para query string, `@RequestBody` para JSON y `@AuthenticationPrincipal` para identidad autenticada. Delega la regla de negocio al servicio y responde con `ResponseEntity`, que permite elegir estado HTTP.

El servicio es donde viven reglas importantes: una reserva no debe durar menos del mínimo ni exceder el límite; un usuario no debe administrar un vehículo ajeno; operador debe tener asignado el estacionamiento. `@Transactional` delimita una transacción: varias lecturas/escrituras conforman una unidad atómica. `readOnly=true` expresa que una operación consulta. Las reglas se vuelven a validar en el servidor aunque la app ya haya validado el formulario: un cliente puede ser modificado o no ser de confianza.

Spring Data JPA crea repositorios a partir de interfaces como `JpaRepository`. Consultas derivadas del nombre o anotadas con `@Query` se convierten en SQL. Hibernate mapea entidades y relaciones. Las entidades representan filas; `@Id`, `@GeneratedValue`, `@ManyToOne` y `@JoinColumn` describen identidad y asociaciones. DTO evitan publicar indiscriminadamente entidades internas.

Los errores personalizados describen not found, conflicto, validación o falta de autorización. `GlobalExceptionHandler` los convierte en respuestas comprensibles. No se deben exponer trazas, contraseñas o consultas crudas en respuestas productivas. `application.properties` lee configuración y variables de entorno; credenciales reales no deben quedar versionadas.

Maven, descrito por `backend/pom.xml`, descarga dependencias, compila, ejecuta pruebas y empaqueta. Los starters agrupan dependencias coherentes (web, security, data-jpa, validation, actuator). El plugin de Spring Boot facilita ejecutar y crear artefacto. El JDK configurado y la versión del proyecto deben coincidir; el `pom` declara Java 21.

## 🔐 9. Seguridad y autenticación

El flujo esperado empieza con `POST /api/auth/login`; el backend compara la contraseña ingresada con un hash BCrypt guardado, no con texto plano. Si autentica, devuelve access token y refresh token. El access token firmado identifica al usuario en llamadas posteriores mediante `Authorization: Bearer ...`. El servidor valida firma, vencimiento y usuario antes de permitir el endpoint.

JWT firmado no es necesariamente cifrado: sus claims pueden ser legibles por quien posea el token, por lo que no se colocan secretos en ellos. El refresh token permite obtener credenciales nuevas y se almacena/revoca según la política del backend. Rotación e invalidación deben manejarse cuidadosamente. Guardar tokens en Preferences DataStore no equivale a almacenamiento cifrado; el diseño debe evaluarse para el riesgo de la aplicación.

RBAC significa control de acceso basado en roles. `SecurityConfig` decide qué rutas son públicas, autenticadas o restringidas a un rol. “El usuario tiene rol” no basta: también hace falta autorización horizontal, verificar que el objeto solicitado pertenece al usuario o a un estacionamiento que su operador administra. La seguridad se verifica en backend; ocultar un botón no protege un endpoint.

BCrypt es un hash adaptativo con salt; no se descifra para recuperar la contraseña. TLS/HTTPS protege datos en tránsito en producción. CORS regula qué orígenes de navegador pueden hacer peticiones; no reemplaza autenticación y el tráfico Android nativo no está sujeto a la política de origen de navegador del mismo modo. `network_security_config.xml` define políticas de seguridad de red Android, no configura TLS del servidor.

Las claves de Maps, JWT y base de datos tienen contextos distintos. No deben confundirse una clave pública restringida de Maps con un secreto de firma JWT. Variables de entorno, `local.properties` fuera de Git y configuración de despliegue ayudan a separar secretos del código. Los valores de ejemplo no son credenciales válidas ni apropiadas para producción.

## 🗄️ 10. MySQL, modelo y relaciones

MySQL guarda información relacional. Una tabla organiza filas y columnas; una clave primaria identifica una fila; una clave foránea exige una relación válida; una restricción `UNIQUE` evita duplicados; un índice acelera búsquedas selectivas con coste adicional de almacenamiento y escrituras. `NOT NULL` expresa obligatoriedad. `DECIMAL` es apropiado para importes porque representa decimales exactos mejor que `FLOAT`.

Las tablas principales están en `database/schema.sql`: `users` y `roles` modelan cuentas y permisos; `user_roles` resuelve relación muchos-a-muchos; `refresh_tokens` conserva tokens revocables; `vehicle_types` y `vehicles` representan vehículos; `parking_lots` contiene sedes; `parking_services` describe servicios; `parking_spaces` representa cupos; `schedules` y `tariffs` definen horarios y precios; `reservations` enlaza usuario, vehículo, estacionamiento y espacio; tablas de historial/auditoría conservan cambios; `parking_ratings` guarda calificaciones; `operator_parking_assignments` asigna operadores.

Un `user` puede tener varios vehículos, pero cada vehículo referencia un usuario. Un estacionamiento tiene muchos espacios, horarios, tarifas y servicios. Una reserva apunta a un espacio, usuario, vehículo y estacionamiento. La redundancia de `parking_lot_id` en reserva puede facilitar filtros, pero exige mantener consistencia entre el lote y el espacio enlazados. El diseño usa nombres y constraints que el ORM y el SQL deben mapear de manera compatible.

`schema.sql` define estructura y `data.sql` carga datos semilla (roles, catálogos y ejemplos). `docker-compose.yml` levanta MySQL y backend como servicios conectados a una red; el backend usa el nombre DNS `mysql`, no `localhost`, para llegar al contenedor de base. Un volumen conserva datos al reiniciar contenedores. Los scripts SQL de inicialización de la imagen MySQL se ejecutan al inicializar un directorio de datos vacío; cambiar el SQL no garantiza que se vuelva a ejecutar sobre un volumen existente.

Hay documentación antigua con un esquema distinto (por ejemplo nombres de roles/campos). La fuente que debe comprobarse es el SQL actual más las entidades y consultas reales. No afirmar que el esquema descrito por un documento anterior sea idéntico al actual.

## 📆 11. Regla crítica: reservar sin conflictos

Dos usuarios podrían solicitar simultáneamente la misma plaza en el mismo intervalo. Una validación solo en Android no sirve: ambos teléfonos podrían decidir que el espacio está libre. El backend valida fechas, existencia y pertenencia del vehículo, tipo de vehículo, actividad del estacionamiento, precio y solapamiento. Luego persiste dentro de una transacción.

Dos intervalos `[inicioA, finA)` y `[inicioB, finB)` se solapan cuando `inicioA < finB` y `finA > inicioB`. Usar intervalos semiabiertos permite que una reserva que termina a las 10:00 y otra que inicia a las 10:00 no se consideren superpuestas. El repositorio tiene consulta de reservas existentes y consulta con bloqueo; el servicio puede bloquear la fila del espacio con `FOR UPDATE` antes de confirmar disponibilidad. El bloqueo serializa decisiones concurrentes sobre esa fila mientras vive la transacción.

En `ReservationService.createReservation`, el servidor obtiene el vehículo del usuario actual, verifica que no tenga otra reserva solapada, bloquea el espacio elegido o busca uno compatible, comprueba que el estacionamiento esté activo/abierto, calcula el precio desde tarifa del servidor y guarda con estado inicial. No se debe aceptar el total enviado por Android como fuente autoritativa: el cliente puede alterar JSON. Hay que entender que aislamiento, bloqueo y consulta de solapamiento trabajan juntos; cada uno por sí solo puede ser insuficiente según la base, índices y transacciones.

Los estados `PENDING`, `CONFIRMED`, `ACTIVE`, `COMPLETED`, `CANCELLED` y `NO_SHOW` describen el ciclo de una reserva; los estados de espacio son una dimensión distinta (`AVAILABLE`, `OCCUPIED`, `RESERVED`, `MAINTENANCE`, según el código/esquema vigente). Una reserva cancelada y un espacio en mantenimiento no son el mismo concepto. El operador puede hacer check-in/check-out con verificación de asignación y actualizar el estado correspondiente.

## 🧪 12. Pruebas y lectura de errores

El backend tiene pruebas de servicio para autenticación/reservas y una prueba de controlador de autenticación. JUnit define los casos; Mockito reemplaza colaboradoras y permite preparar respuestas; AssertJ expresa aserciones; Spring MVC Test puede verificar endpoints sin desplegar un servidor real. Un test de servicio suele comprobar una regla de dominio y sus errores esperados. Las pruebas no prueban todo el producto: complementan revisión manual, integración y pruebas de UI.

En Android existen dependencias para JUnit y pruebas instrumentadas, aunque eso no implica cobertura completa. Las pruebas instrumentadas corren en Android/emulador y pueden inspeccionar componentes de UI; las pruebas locales suelen correr en JVM. No se ejecutaron pruebas en este trabajo de documentación.

Al investigar un fallo, identifica primero la capa y el mensaje original. `UnknownHostException` sugiere DNS/red; timeout indica que el host no responde a tiempo; HTTP 401 sugiere autenticación inválida/expirada; 403 denota identidad sin permiso; 404 recurso inexistente; 409 conflicto de negocio; 400 validación/formato; 500 fallo inesperado del servidor. Un error JSON de parseo puede indicar que el contrato DTO difiere de la respuesta. Inspeccionar logs solo en desarrollo y sin volcar credenciales.

## 🚀 13. Cómo levantar el proyecto

Lee primero `docs/instalacion.md` y `scripts/`. Requisitos indicados por los archivos: Java 21, Maven, MySQL o Docker, Android Studio y SDK Android correspondiente. El proyecto Compose usa `compileSdk 35`, por lo que instalar ese SDK en Android Studio es lo más directo. Gradle wrapper está dentro de `android/`; Maven usa `backend/pom.xml`.

Con Docker Compose, revisa variables de entorno antes de iniciar, especialmente contraseñas y secreto JWT. Compose expone MySQL del contenedor por el puerto anfitrión 3307 y API en 8080; el servicio backend conecta al puerto interno 3306 del host `mysql`. En conexión local directa las propiedades pueden tener otros puertos/usuarios. No mezcles la URL de Android con la URL de JDBC.

En Android, configura `android/local.properties` para SDK y, si hace falta, `MAPS_API_KEY` y `BACKEND_URL`; no lo subas. La URL predeterminada de Gradle puede estar atada a una IP LAN del desarrollador. Para emulador usa el host accesible correcto; para dispositivo físico ambos deben tener conectividad de red y el backend debe escuchar en una interfaz accesible. HTTP claro puede requerir configuración permitida en desarrollo; en producción corresponde HTTPS.

Los scripts de base de datos pueden borrar o recrear datos. Antes de usar `reset-db.sh`, leer su contenido y verificar el nombre de la base, sobre todo si hay datos que conservar. El script de setup puede instalar/compilar dependiendo del sistema, por lo que no se debe ejecutar a ciegas. Esta guía no ejecuta compilaciones ni pruebas.

## 🗂️ 14. Catálogo comentado de todos los archivos versionados

Este índice comenta los 214 archivos actualmente versionados. Los archivos de caché y resultados bajo `.gradle/` o `build/` no están versionados y no son fuente. Las imágenes se describen porque por su naturaleza binaria no admiten comentarios legibles sin alterar el recurso.

### Android: entrada, configuración y recursos

- `android/settings.gradle.kts`: define repositorios para plugins/dependencias y declara el módulo `:app`.
- `android/build.gradle.kts`: configuración Gradle común de nivel raíz; centraliza plugins que usan los módulos.
- `android/gradle.properties`: propiedades globales del proceso de construcción, incluyendo ajustes de Gradle/Kotlin.
- `android/gradle/gradle-daemon-jvm.properties`: restricciones/versión de JVM del daemon para reproducibilidad de builds.
- `android/gradle/libs.versions.toml`: catálogo de versiones, alias de librerías y plugins Android, Compose, Retrofit, Maps, DataStore, coroutines y tests.
- `android/gradle/wrapper/gradle-wrapper.properties`: versión/distribución del wrapper que usa `gradlew`.
- `android/gradlew`, `android/gradlew.bat`: lanzadores Unix y Windows del wrapper; permiten usar Gradle sin instalar una versión global.
- `android/app/build.gradle.kts`: módulo Android; SDKs, identificadores, BuildConfig, tipos de build y dependencias.
- `android/app/proguard-rules.pro`: reglas para R8/ProGuard cuando se minifica u ofusca el APK.
- `android/app/src/main/AndroidManifest.xml`: declara aplicación, actividad, permisos, configuración de red y metadatos necesarios para Android.
- `android/app/src/main/java/pe/parkeo/MainActivity.kt`: Activity Compose de entrada; enlaza el contenedor de la aplicación con `setContent`.
- `android/app/src/main/java/pe/parkeo/ParkeoApplication.kt`: subclase `Application`; inicializa el contenedor accesible por toda la app.
- `android/app/src/main/ic_launcher-playstore.png`: recurso de marca para publicación; binario, se documenta aquí.
- `android/app/src/main/res/drawable/ic_launcher_background.xml`, `ic_launcher_foreground.xml`: capas vectoriales del icono adaptable.
- `android/app/src/main/res/drawable/splash_branding.xml`: recurso gráfico XML utilizado en la identidad de inicio.
- `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml`: definiciones de icono adaptable para Android 8/API 26 en adelante.
- `android/app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.webp` y `ic_launcher_round.webp`: iconos raster por densidad de pantalla; son binarios.
- `android/app/src/main/res/values/colors.xml`: colores de recursos nombrados.
- `android/app/src/main/res/values/strings.xml`: cadenas extraíbles/localizables de la app.
- `android/app/src/main/res/values/themes.xml`, `values-night/themes.xml`, `values-v31/themes.xml`: tema base, alternativa oscura y configuración específica de Android 12/API 31+.
- `android/app/src/main/res/xml/network_security_config.xml`: política Android para tráfico de red, certificados y excepciones por dominio según configuración.

### Android: ensamblado de dependencias y datos

- `android/app/src/main/java/pe/parkeo/di/AppContainer.kt`: contratos y construcción lazy de DataStore, JSON, OkHttp, Retrofit, APIs y repositorios; punto principal para entender la inyección manual.
- `android/app/src/main/java/pe/parkeo/data/local/SessionDataStore.kt`: persistencia asíncrona de tokens y datos de sesión con Preferences DataStore; observa cambios como Flow.
- `android/app/src/main/java/pe/parkeo/data/remote/interceptor/AuthInterceptor.kt`: interceptor HTTP para asociar credencial de acceso a solicitudes autenticadas.
- `android/app/src/main/java/pe/parkeo/data/remote/api/AuthApi.kt`, `ParkingApi.kt`, `ReservationApi.kt`, `VehicleApi.kt`, `AdminApi.kt`, `OperatorApi.kt`: interfaces declarativas Retrofit; agrupan endpoints por área funcional.
- `android/app/src/main/java/pe/parkeo/data/remote/dto/AuthDtos.kt`, `ParkingDtos.kt`, `ReservationDtos.kt`, `VehicleDtos.kt`, `AdminDtos.kt`, `OperatorDtos.kt`: estructuras de request/response serializables para contratos JSON de cada función.
- `android/app/src/main/java/pe/parkeo/data/remote/dto/PageDto.kt`: forma paginada del contrato remoto; permite expresar contenido, página y metadatos.
- `android/app/src/main/java/pe/parkeo/data/repository/AuthRepositoryImpl.kt`, `ParkingRepositoryImpl.kt`, `ReservationRepositoryImpl.kt`, `VehicleRepositoryImpl.kt`, `AdminRepositoryImpl.kt`, `OperatorRepositoryImpl.kt`: implementaciones concretas de repositorios; conectan interfaz de dominio con API/almacenamiento y transforman errores/resultados.
- `android/app/src/main/java/pe/parkeo/domain/repository/AuthRepository.kt`, `ParkingRepository.kt`, `ReservationRepository.kt`, `VehicleRepository.kt`, `AdminRepository.kt`, `OperatorRepository.kt`: contratos del dominio que declaran operaciones disponibles sin fijar Retrofit.
- `android/app/src/main/java/pe/parkeo/domain/model/ParkingLot.kt`, `Reservation.kt`, `Vehicle.kt`: modelos Kotlin de negocio que consumen ViewModels y pantallas.
- `android/app/src/main/java/pe/parkeo/util/Result.kt`: tipo sellado para representar éxito/error sin dispersar el manejo de excepción.
- `android/app/src/main/java/pe/parkeo/util/NetworkErrorParser.kt`: normaliza errores de conexión/HTTP para mensajes útiles a la UI.

### Android: interfaz, navegación y estado

- `android/app/src/main/java/pe/parkeo/ui/ParkeoApp.kt`: composición raíz de la app, tema y host de navegación.
- `android/app/src/main/java/pe/parkeo/ui/navigation/NavRoutes.kt`: nombres/constantes de rutas navegables.
- `android/app/src/main/java/pe/parkeo/ui/navigation/ParkeoNavGraph.kt`: asociación de rutas con pantallas, ViewModels y acciones de navegación.
- `android/app/src/main/java/pe/parkeo/ui/components/ParkeoBadge.kt`, `ParkeoBottomBar.kt`, `ParkeoButton.kt`, `ParkeoCard.kt`, `ParkeoStates.kt`, `ParkeoTextField.kt`, `ParkeoTopBar.kt`, `ParkingCard.kt`: componentes reutilizables; encapsulan presentación coherente y estados comunes.
- `android/app/src/main/java/pe/parkeo/ui/screens/auth/LoginScreen.kt`, `RegisterScreen.kt`: formularios y acciones de acceso/registro.
- `android/app/src/main/java/pe/parkeo/ui/screens/onboarding/OnboardingScreen.kt`: introducción inicial y avance/omisión de páginas.
- `android/app/src/main/java/pe/parkeo/ui/screens/splash/SplashScreen.kt`: pantalla de inicio mientras se determina navegación/sesión.
- `android/app/src/main/java/pe/parkeo/ui/screens/main/MainScreen.kt`: contenedor de navegación principal inferior.
- `android/app/src/main/java/pe/parkeo/ui/screens/home/HomeScreen.kt`: búsqueda/descubrimiento de estacionamientos y contenido inicial.
- `android/app/src/main/java/pe/parkeo/ui/screens/parking/ParkingDetailScreen.kt`: detalle del estacionamiento seleccionado.
- `android/app/src/main/java/pe/parkeo/ui/screens/reservation/ReservationCreateScreen.kt`, `ReservationsScreen.kt`: creación y lista de reservas.
- `android/app/src/main/java/pe/parkeo/ui/screens/vehicles/VehiclesScreen.kt`: consulta/gestión de vehículos del usuario.
- `android/app/src/main/java/pe/parkeo/ui/screens/profile/ProfileScreen.kt`: perfil, navegación a funciones personales y cierre de sesión.
- `android/app/src/main/java/pe/parkeo/ui/screens/operator/OperatorReservationsScreen.kt`, `OperatorSpacesScreen.kt`: vistas operativas de reservas y ocupación.
- `android/app/src/main/java/pe/parkeo/ui/screens/admin/AdminDashboardScreen.kt`, `AdminParkingLotsScreen.kt`, `AdminUsersScreen.kt`: estadísticas y administración de estacionamientos/usuarios.
- `android/app/src/main/java/pe/parkeo/ui/theme/Color.kt`, `Dimens.kt`, `Shape.kt`, `Theme.kt`, `Type.kt`: sistema visual de colores, tamaños, formas, composición de tema y tipografía.
- `android/app/src/main/java/pe/parkeo/ui/viewmodel/AuthViewModel.kt`, `HomeViewModel.kt`, `ParkingDetailViewModel.kt`, `ReservationViewModel.kt`, `VehicleViewModel.kt`, `AdminViewModel.kt`, `OperatorViewModel.kt`, `OnboardingViewModel.kt`, `SplashViewModel.kt`: estado observable y acciones para cada flujo de pantalla.
- `android/app/src/main/java/pe/parkeo/ui/viewmodel/ViewModelFactory.kt`: fábrica para ViewModels con dependencias no triviales.
- `android/app/src/main/java/pe/parkeo/util/NavigationHelper.kt`: pequeñas reglas/helper de navegación compartida.
- `android/app/src/main/java/pe/parkeo/util/RequestNotificationPermission.kt`: solicitud del permiso de notificaciones en versiones Android pertinentes.
- `android/app/src/main/java/pe/parkeo/util/ReminderScheduler.kt`, `ReminderReceiver.kt`, `ReservationReminderWorker.kt`: planificación, recepción y ejecución de recordatorios; muestran diferencias entre `BroadcastReceiver` y `WorkManager`.

### Backend: arranque y configuración

- `backend/pom.xml`: versión Java, parent Spring Boot, dependencias y plugins Maven.
- `backend/src/main/java/pe/parkeo/ParkeoApplication.java`: punto principal de arranque Spring Boot.
- `backend/src/main/resources/application.properties`: configuración de servidor, datasource, JPA, JWT y documentación desde propiedades/variables.
- `backend/src/main/java/pe/parkeo/config/CorsConfig.java`: política de orígenes y métodos HTTP.
- `backend/src/main/java/pe/parkeo/config/JwtConfig.java`: propiedades y beans que necesita JWT.
- `backend/src/main/java/pe/parkeo/config/OpenApiConfig.java`: configuración de documentación Swagger/OpenAPI.
- `backend/src/main/java/pe/parkeo/config/SecurityConfig.java`: filtros, autenticación, autorización, encoder y rutas permitidas.
- `backend/src/main/java/pe/parkeo/security/JwtAuthenticationFilter.java`, `JwtTokenProvider.java`, `UserDetailsServiceImpl.java`: lectura de bearer token, creación/validación JWT y carga de identidad Spring Security.

### Backend: HTTP, negocio y persistencia

- `backend/src/main/java/pe/parkeo/controller/AuthController.java`, `UserController.java`, `ParkingLotController.java`, `AvailabilityController.java`, `ReservationController.java`, `VehicleController.java`, `OperatorController.java`, `AdminController.java`: endpoints REST por área y conversión entre HTTP y servicios.
- `backend/src/main/java/pe/parkeo/service/AuthService.java`: registro/login, hash, emisión y administración del flujo de autenticación.
- `backend/src/main/java/pe/parkeo/service/UserService.java`, `VehicleService.java`: perfil y operaciones de vehículos con controles de propietario.
- `backend/src/main/java/pe/parkeo/service/ParkingLotService.java`, `ParkingSpaceService.java`: consulta/gestión de estacionamientos y espacios.
- `backend/src/main/java/pe/parkeo/service/AvailabilityService.java`: disponibilidad, consultas auxiliares y/o cálculo de precio.
- `backend/src/main/java/pe/parkeo/service/ReservationService.java`: núcleo de validación, creación, consulta, cancelación, check-in/check-out y transición de reservas.
- `backend/src/main/java/pe/parkeo/service/RefreshTokenService.java`: persistencia, vencimiento y revocación de refresh tokens.
- `backend/src/main/java/pe/parkeo/repository/ParkingLotRepository.java`, `ParkingSpaceRepository.java`, `ReservationRepository.java`: consultas de estacionamiento, bloqueos y ventanas de tiempo.
- `backend/src/main/java/pe/parkeo/repository/UserRepository.java`, `RoleRepository.java`, `RefreshTokenRepository.java`: acceso de usuarios, roles y tokens.
- `backend/src/main/java/pe/parkeo/repository/VehicleRepository.java`, `VehicleTypeRepository.java`: persistencia de vehículos y catálogo.
- `backend/src/main/java/pe/parkeo/repository/TariffRepository.java`, `ScheduleRepository.java`: búsqueda de tarifas y horarios.

### Backend: contratos y modelo

- `backend/src/main/java/pe/parkeo/dto/request/LoginRequest.java`, `RegisterRequest.java`, `RefreshTokenRequest.java`: entradas de identidad, registro y renovación; anotaciones validan forma de campos.
- `backend/src/main/java/pe/parkeo/dto/request/CreateVehicleRequest.java`, `UpdateVehicleRequest.java`: datos de alta y edición del vehículo.
- `backend/src/main/java/pe/parkeo/dto/request/CreateReservationRequest.java`, `CheckAvailabilityRequest.java`, `UpdateSpaceStatusRequest.java`: selección de fechas/espacio y operaciones sobre disponibilidad.
- `backend/src/main/java/pe/parkeo/dto/response/ApiResponse.java`: envoltura común de respuestas.
- `backend/src/main/java/pe/parkeo/dto/response/AuthResponse.java`, `UserResponse.java`, `VehicleResponse.java`: respuestas de cuenta, usuario y vehículo.
- `backend/src/main/java/pe/parkeo/dto/response/ParkingLotResponse.java`, `ParkingLotDetailResponse.java`, `ParkingSpaceResponse.java`, `AvailabilityResponse.java`: información resumida/detallada de sedes, cupos y disponibilidad.
- `backend/src/main/java/pe/parkeo/dto/response/ReservationResponse.java`, `ReservationDetailResponse.java`: resumen y detalle de reserva.
- `backend/src/main/java/pe/parkeo/entity/User.java`, `Role.java`, `RefreshToken.java`: identidad, roles y token persistido.
- `backend/src/main/java/pe/parkeo/entity/Vehicle.java`, `VehicleType.java`: medios de transporte registrados y catálogo.
- `backend/src/main/java/pe/parkeo/entity/ParkingLot.java`, `ParkingSpace.java`, `ParkingService.java`, `Schedule.java`, `Tariff.java`, `ParkingRating.java`, `Reservation.java`: agregado operacional del estacionamiento y vínculos JPA.
- `backend/src/main/java/pe/parkeo/enums/DayOfWeek.java`, `ReservationStatus.java`, `RoleName.java`, `SpaceStatus.java`, `TariffType.java`: conjuntos cerrados de valores válidos para reglas/columnas.
- `backend/src/main/java/pe/parkeo/exception/ConflictException.java`, `ResourceNotFoundException.java`, `UnauthorizedException.java`, `ValidationException.java`: errores de dominio con significado distinto.
- `backend/src/main/java/pe/parkeo/exception/GlobalExceptionHandler.java`: mapea excepciones a estados/cuerpos HTTP.

### Base de datos, despliegue, documentación y scripts

- `database/schema.sql`: DDL (crear tablas/índices/relaciones), charset, constraints y comentarios SQL del esquema.
- `database/data.sql`: datos semilla; revisar antes de reimportar para conocer IDs y credenciales de demostración.
- `database/README.md`: instrucciones y descripción del modelo; comparar con el esquema vigente porque puede haber información histórica.
- `docker-compose.yml`: servicios MySQL/backend, variables, puertos, volumen, red y healthchecks.
- `docs/arquitectura.md`: descripción de componentes y responsabilidades.
- `docs/api.md`: catálogo documentado de endpoints; comprobar rutas reales en controladores y OpenAPI.
- `docs/base-datos.md`: descripción del modelo SQL y tablas; cotejar con `schema.sql`.
- `docs/flujos.md`: recorridos esperados de UI, permisos, errores y sesión.
- `docs/instalacion.md`: requisitos, configuración y ejecución para entorno de desarrollo.
- `docs/seguridad.md`: modelo de amenazas/controles y recomendaciones; separar recomendación de configuración efectiva.
- `README.md`: presentación, stack, arquitectura, instalación y enlaces de entrada.
- `.gitignore`: evita versionar secretos locales, cachés, compilados y artefactos; es parte de la protección del repositorio.
- `logo.svg`: identidad vectorial escalable usada en documentación/presentación.
- `scripts/setup.sh`: automatiza preparación local; leer comandos antes de usar.
- `scripts/start-backend.sh`: prepara variables/ejecución del servicio backend.
- `scripts/reset-db.sh`: operación con efecto destructivo sobre datos; revisar objetivo y confirmar backup antes de usar.
- `scripts/demo_flow.sh`: automatiza llamadas para mostrar un flujo de API.
- `scripts/levantar-celular.sh`, `scripts/levantar_en_celular.txt`: notas y pasos para probar en un dispositivo físico y configurar conectividad.

### Pruebas backend

- `backend/src/test/java/pe/parkeo/ParkeoApplicationTests.java`: verifica que el contexto/arranque de la aplicación de prueba pueda inicializarse.
- `backend/src/test/java/pe/parkeo/controller/AuthControllerTest.java`: prueba contrato HTTP del controlador de autenticación.
- `backend/src/test/java/pe/parkeo/service/AuthServiceTest.java`: prueba éxito y errores principales del servicio de autenticación con dependencias simuladas.
- `backend/src/test/java/pe/parkeo/service/ReservationServiceTest.java`: prueba reglas, conflictos de intervalo y operaciones de reservas.

## 🎓 15. Preguntas para sustentar y respuestas guía

1. **¿Por qué la app no conecta directamente a MySQL?** Porque el servidor centraliza validación, autorización, reglas, transacciones y acceso a datos; no se exponen credenciales de BD en cada teléfono.
2. **¿Qué diferencia hay entre una Activity y un Composable?** Activity es un componente Android del ciclo de vida; un Composable es una función declarativa que describe UI dentro de un contexto Compose.
3. **¿Qué significa que Kotlin tenga null safety?** Los tipos distinguen valores anulables y no anulables, obligando al programa a tratar explícitamente el caso nulo.
4. **¿Qué aporta `suspend`?** Permite suspender una corrutina de forma no bloqueante mientras espera una operación; necesita un contexto de corrutina.
5. **¿Por qué usar ViewModel?** Mantiene estado de presentación fuera de la pantalla y sobrevive recreaciones de configuración; no debe contener lógica de persistencia específica.
6. **¿Qué diferencia hay entre `StateFlow` y `MutableStateFlow`?** El mutable permite emitir valores; la interfaz expone el contrato de lectura para evitar mutaciones externas.
7. **¿Qué es recomposición?** Reejecución selectiva de funciones Compose cuando cambia estado leído, para actualizar lo que se ve.
8. **¿Qué representa un DTO?** Un objeto de transferencia que define el contrato de datos entre límites, como el JSON del backend.
9. **¿Qué hace Retrofit?** Genera implementación HTTP de interfaces anotadas y convierte respuestas JSON mediante un converter.
10. **¿Qué hace un interceptor OkHttp?** Puede observar o modificar solicitudes/respuestas; aquí participa en agregar autenticación y registrar peticiones debug.
11. **¿Qué significa `by lazy`?** Inicializa un valor la primera vez que se solicita y conserva ese resultado, útil para componentes costosos.
12. **¿Por qué hay un repositorio de dominio y otro de datos?** El dominio consume una abstracción; datos brinda implementación concreta con Retrofit/DataStore, facilitando sustitución y pruebas.
13. **¿Qué diferencia hay entre `compileSdk`, `minSdk` y `targetSdk`?** APIs de compilación, mínimo instalable y nivel de comportamiento objetivo respectivamente.
14. **¿Para qué sirve el catálogo `libs.versions.toml`?** Centraliza alias/versiones de plugins y librerías y reduce duplicación en scripts Gradle.
15. **¿Qué es un plugin Gradle?** Extiende el sistema de build con tareas y configuración, como compilar un módulo Android.
16. **¿Qué diferencia hay entre `implementation` y `testImplementation`?** La primera dependencia sirve a la app; la segunda solo al classpath de pruebas unitarias.
17. **¿Qué rol cumple `@SpringBootApplication`?** Configura el arranque, habilita autoconfiguración y escanea componentes Spring.
18. **¿Qué debe hacer un controlador?** Traducir HTTP a una llamada de aplicación y construir respuesta; reglas complejas corresponden al servicio.
19. **¿Qué aporta `@Transactional`?** Define una unidad transaccional para ejecutar cambios consistentes y revertirlos ante fallo conforme a reglas de Spring.
20. **¿Qué es JPA frente a Hibernate?** JPA define API/estándar de persistencia; Hibernate es una implementación popular usada por Spring Data JPA.
21. **¿Por qué hay entidades y DTO distintos?** Las entidades reflejan persistencia/relaciones; DTO controlan el contrato externo y evitan filtrar campos internos.
22. **¿Qué diferencia hay entre 401 y 403?** 401 indica que falta/fracasa autenticación; 403 que la identidad autenticada no tiene autorización.
23. **¿JWT está cifrado?** Un JWT firmado común suele tener claims codificados y legibles; la firma comprueba integridad, no confidencialidad.
24. **¿Por qué BCrypt y no guardar la contraseña directamente?** Es un hash adaptativo con salt pensado para hacer costoso probar contraseñas; no permite recuperar el texto original.
25. **¿Qué es RBAC?** Autorización basada en roles para agrupar capacidades de usuarios.
26. **¿Qué es control horizontal?** Asegurar que un usuario solo acceda a recursos que le pertenecen o tiene asignados, incluso si conoce el ID.
27. **¿Para qué se usa clave foránea?** Evita referencias a filas inexistentes y mantiene integridad entre entidades relacionadas.
28. **¿Para qué sirve un índice?** Acelera ciertas búsquedas/ordenamientos con coste de espacio y mantenimiento al insertar/actualizar.
29. **¿Por qué el precio debe calcularse en backend?** El cliente no es confiable y puede manipular un importe; el servidor consulta la tarifa autoritativa.
30. **¿Cómo se detecta solapamiento temporal?** Comparando inicio solicitado con fin existente e inicio existente con fin solicitado; se bloquea/coordina para evitar carrera.
31. **¿Qué hace `SELECT ... FOR UPDATE` en el flujo?** Bloquea filas elegidas durante la transacción para coordinar escrituras concurrentes, según motor y aislamiento.
32. **¿Qué diferencia hay entre error de validación y conflicto?** Validación significa que el dato/operación no cumple reglas; conflicto que entra en colisión con estado existente, como horario ocupado.
33. **¿Qué es Docker Compose aquí?** Define servicios cooperantes, variables, puertos, volumen y red para levantar base y backend localmente.
34. **¿Por qué Compose backend apunta a `mysql:3306`?** Dentro de la red Compose, `mysql` es nombre DNS del servicio y 3306 es puerto interno.
35. **¿Un healthcheck implica que Compose espere siempre?** En este compose hay dependencia por `service_healthy`; verifica disponibilidad declarada antes de iniciar backend.
36. **¿Qué es una migración y el SQL actual es una?** Migración versiona cambios graduales de esquema; `schema.sql` crea estructura, pero por sí solo no constituye un historial migratorio automatizado.
37. **¿Qué hace CORS?** Permite/restringe solicitudes iniciadas desde orígenes web; no sustituye autenticación y autorización.
38. **¿Qué hace DataStore?** Persiste preferencias de forma asíncrona y observable; no es el almacenamiento MySQL del dominio.
39. **¿Para qué sirve WorkManager?** Programa trabajo persistente sujeto a restricciones del sistema; es apropiado para recordatorios/tareas diferibles.
40. **¿Por qué revisar logs sin tokens?** Los registros pueden copiarse o centralizarse; incluir credenciales las convierte en exposición de acceso.
41. **¿Qué prueba `ReservationServiceTest`?** Reglas del servicio de reserva en casos simulados; no demuestra por sí sola que UI, red y MySQL integren correctamente.
42. **¿Cómo tratarías una respuesta JSON nueva?** Revisaría el contrato del endpoint y DTO; tolerar claves desconocidas no resuelve incompatibilidad en tipos/campos requeridos.
43. **¿Qué significa separar debug y release?** Permite activar herramientas de diagnóstico en desarrollo y aplicar configuración segura/optimizada en distribución.
44. **¿Qué verificarías si el teléfono no conecta?** URL/base URL, IP y puerto, backend escuchando, red/firewall, permisos/política HTTP, logs y respuesta del endpoint.
45. **¿Cómo se diferencia disponibilidad de estado del espacio?** Disponibilidad se calcula para un intervalo considerando reservas; estado es condición operativa del espacio.

Al responder una pregunta, empieza con la definición, señala el archivo/clase donde se ve y da un ejemplo del flujo. Si una capacidad solo aparece en documentación y no en código ejecutable, dilo explícitamente.

## 📚 16. Ruta de estudio sugerida

1. Aprende variables, tipos, nulabilidad, funciones, clases, interfaces, listas y `when` con ejemplos pequeños de Kotlin.
2. Abre `AppContainer.kt` y traza la creación de Retrofit. Explica qué dependencia necesita cada objeto y por qué se crea lazy.
3. Sigue `AuthViewModel` hasta `AuthRepositoryImpl`, `AuthApi` y sus DTO. Dibuja petición y respuesta JSON.
4. Recorre `ParkeoNavGraph.kt` para entender cómo se selecciona pantalla y quién pasa callbacks.
5. Abre `ReservationService.java` y escribe las precondiciones en lenguaje natural antes de mirar cada `if`.
6. Compara entidades Java con tablas SQL: marca PK, FK, nullable, enum e índice.
7. Estudia transacciones, intervalos y bloqueo con dos solicitudes simultáneas como escenario.
8. Lee Gradle y Maven por capas; diferencia plugins, dependencias, SDK, compilador y empaquetado.
9. Ejecuta solo los pasos de instalación revisados para tu entorno y observa logs de forma segura.
10. Practica las preguntas de [[#🎓 15. Preguntas para sustentar y respuestas guía]] sin leer, luego usa esta guía para corregir precisión.

Para cada método que estudies, contesta: ¿quién lo llama?, ¿qué datos recibe?, ¿qué valida?, ¿qué puede fallar?, ¿qué efecto observable produce?, ¿cómo se prueba? Esa plantilla enseña a explicar métodos más allá de traducir sus nombres.

## ⚠️ 17. Discrepancias y límites que debes explicar

La guía es un mapa educativo de los archivos versionados, no una promesa de que cada pantalla esté conectada a un servicio real. Hay que seguir cada callback para confirmar una función; un icono o endpoint documentado no garantiza implementación completa. Los documentos existentes contienen referencias de configuración y partes del modelo que pueden estar desactualizadas respecto del código/SQL.

En `app/build.gradle.kts` la URL debug predeterminada contiene una IP local; para otro equipo se debe configurar `BACKEND_URL`. El README menciona algunos componentes del producto (por ejemplo flujo de mapas) que conviene comprobar en la pantalla efectiva. Los nombres de roles y tablas han evolucionado: cotejar `schema.sql`, enums Java y consultas en vez de inferir desde una captura antigua.

`docker-compose.yml` muestra credenciales de desarrollo de ejemplo con fallback. Sustituir secretos antes de exponer servicios. Las configuraciones CORS permisivas, tráfico HTTP de desarrollo, logging de cuerpo y valores locales no deben trasladarse a producción sin endurecimiento. Una API de mapas también puede generar coste y debe restringirse por plataforma y cuota.

No se editaron compilados o cachés generados, imágenes ni artefactos ignorados. Las pruebas y compilaciones no se ejecutaron: el cambio solicitado es comentario/documentación y no se debe presentar como validado en runtime. Revisa además cualquier documento existente que contenga valores sensibles antes de compartir el repositorio fuera del equipo.

## ✅ 18. Glosario de bolsillo

- **API:** contrato de operaciones que permite que dos programas colaboren.
- **Activity:** componente Android con ciclo de vida y punto de entrada a una interfaz.
- **Anotación:** metadato (`@Composable`, `@Entity`, `@GetMapping`) que un framework procesa.
- **Backend:** servicio central que valida y coordina reglas y datos.
- **Bean:** objeto administrado por el contenedor Spring.
- **Build:** proceso de resolver dependencias, compilar y empaquetar.
- **Composable:** función que describe UI declarativa Compose.
- **Corrutina:** tarea suspendible que permite concurrencia estructurada.
- **DTO:** objeto de transporte para cruzar un límite de sistema.
- **Entidad:** clase persistida/mapeada a una tabla.
- **Flow:** secuencia asíncrona de valores Kotlin.
- **Inyección de dependencias:** proporcionar colaboradores desde afuera en vez de construirlos dentro de cada clase.
- **JPA:** estándar Java de persistencia objeto-relacional.
- **JWT:** token con claims y firma, útil para autenticar solicitudes sin sesión de servidor tradicional.
- **ORM:** mapeo entre objetos y tablas relacionales.
- **Repositorio:** abstracción que consulta o persiste datos.
- **REST:** estilo de API HTTP basada en recursos y métodos.
- **Serialización:** conversión de objetos a JSON; deserialización hace el camino inverso.
- **Transacción:** unidad de cambios que debe observarse como consistente.
- **ViewModel:** estado y coordinación de presentación asociado al ciclo de vida.

---

> [!tip] Consejo final
> No memorices nombres de tecnologías aislados. Aprende a explicar una operación completa de extremo a extremo y qué responsabilidad pertenece a cada archivo. Esa explicación muestra comprensión del proyecto y fundamentos de Android.
