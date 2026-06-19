This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop (JVM), Server.

---

## api-routes — Gradle Plugin

Gradle plugin incluido en `buildSrc` que lee un archivo `api.toml` local y genera en tiempo de compilación un objeto Kotlin tipado con las rutas de tu API.

Sin el plugin:
```kotlin
client.get("/api/character/$id")   // string hardcodeado — typo silencioso
```
Con el plugin:
```kotlin
client.get(ApiRoutes.Characters.Character.route.replace("{id}", id))
// el compilador avisa si el path cambia
```

### Plataformas soportadas

| Plataforma | Modo | Fuente generada |
|------------|------|-----------------|
| **KMP commonMain** | Módulo KMP con el plugin | `commonMain` → todos los targets |
| Android / iOS / Desktop / Web JS / Wasm | vía commonMain | heredado de KMP |
| **JVM puro** (Ktor server, CLI…) | Módulo JVM con el plugin | `main` source set |

### Configuración

**1. Crear `api.toml` en la raíz del proyecto** (gitignoreado; copia `api.example.toml` como base):

```toml
[params]
character-id = "id"

[paths]
character = "/api/character"

[routes]
characters = { path.ref = "character" }
character  = { parent.ref = "characters", param.ref = "character-id" }
```

**2. Aplicar el plugin** — opción A: módulo KMP dedicado:

```kotlin
// api-routes/build.gradle.kts
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("com.hamon.apiroutes")
}
apiRoutes { outputPackage.set("com.example.routes") }
```

```kotlin
// core/build.gradle.kts — expone ApiRoutes a todos los consumers
commonMain.dependencies { api(projects.apiRoutes) }
```

Opción B: módulo JVM (server, script…):

```kotlin
plugins { kotlin("jvm"); id("com.hamon.apiroutes") }
apiRoutes { outputPackage.set("com.example.server.routes") }
```

**3. Usar las constantes generadas:**

```kotlin
ApiRoutes.Characters.route                    // "/api/character"
ApiRoutes.Characters.Character.route          // "/api/character/{id}"
ApiRoutes.Characters.Character.Params.id      // "id"
```

### Opciones del bloque `apiRoutes {}`

| Propiedad | Default | Descripción |
|-----------|---------|-------------|
| `tomlFile` | `<root>/api.toml` | Archivo TOML de entrada |
| `outputDir` | `build/generated/apiRoutes` | Directorio de salida |
| `outputPackage` | `<group>.routes` | Paquete del objeto generado |
| `className` | `"ApiRoutes"` | Nombre del objeto raíz |
| `versionPosition` | `BEFORE_TENANT` | `BEFORE_TENANT` / `AFTER_TENANT` / `NONE` |

### Estructura de `api.toml`

```toml
[params]    # nombre del param de path: user-id = "userId"
[paths]     # segmentos reutilizables: users = "/users"
[versions]  # versiones: v1 = "/v1"
[tenants]   # agrupadores: sales = { path = "/sales" }
[routes]    # definición flat de rutas (ver api.example.toml para referencia completa)
```

Jerarquía generada según combinación:

| Combinación | Acceso |
|-------------|--------|
| sin tenant, sin version | `ApiRoutes.Health` |
| version sola | `ApiRoutes.V1.ApiStatus` |
| tenant solo | `ApiRoutes.Sales.LastSale` |
| tenant + version | `ApiRoutes.Sales.V1.UserDetail` |
| sub-ruta via `parent.ref` | `ApiRoutes.Sales.V1.UserDetail.UserOrders` |

Cada ruta expone `.path` (segmento propio), `.route` (ruta completa) y `.Params.*` (nombres de parámetros acumulados).

### Demo — Rick and Morty API

Este proyecto usa el plugin en `:server` (Ktor/JVM) con el `api.toml` de la raíz apuntando a la [Rick and Morty API](https://rickandmortyapi.com):

```bash
./gradlew :server:run
```

```bash
curl http://localhost:8080/api/character      # lista de personajes
curl http://localhost:8080/api/character/1    # Rick Sanchez
curl http://localhost:8080/api/episode        # lista de episodios
```

Comandos adicionales:

```bash
./gradlew :server:generateApiRoutes           # genera ApiRoutes.kt sin compilar
./gradlew :buildSrc:test                      # 45 tests del plugin
./gradlew :core:jvmTest :server:test          # tests de integración
```

---

* [/app/iosApp](./app/iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose
  Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/app/shared](./app/shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - [commonMain](./app/shared/src/commonMain/kotlin) is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      the [iosMain](./app/shared/src/iosMain/kotlin) folder would be the right place for such calls.
      Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./app/shared/src/jvmMain/kotlin)
      folder is the appropriate location.

* [/core](./core/src) is for the code that will be shared between all targets in the project.
  The most important subfolder is [commonMain](./core/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

* [/server](./server/src/main/kotlin) is for the Ktor server application.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and
options:

- Android app: `./gradlew :app:androidApp:assembleDebug`
- Desktop app:
    - Hot reload: `./gradlew :app:desktopApp:hotRun --auto`
    - Standard run: `./gradlew :app:desktopApp:run`
- Server: `./gradlew :server:run`
- Web app:
    - Wasm target (faster, modern browsers): `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun`
    - JS target (slower, supports older browsers): `./gradlew :app:webApp:jsBrowserDevelopmentRun`
- iOS app: open the [/app/iosApp](./app/iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :app:shared:testAndroidHostTest`
- Desktop tests: `./gradlew :app:shared:jvmTest`
- Server tests: `./gradlew :server:test`
- Web tests:
    - Wasm target: `./gradlew :app:shared:wasmJsTest`
    - JS target: `./gradlew :app:shared:jsTest`
- iOS tests: `./gradlew :app:shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack
channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).