package com.hamon.apiroutes

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

abstract class ApiRoutesExtension {

    /** Ruta al api.toml. Default: api.toml en el root del proyecto. */
    abstract val tomlFile: RegularFileProperty

    /** Directorio donde se escribe el .kt generado. Default: build/generated/apiRoutes. */
    abstract val outputDir: DirectoryProperty

    /** Paquete del archivo generado. Default: <project.group>.routes */
    abstract val outputPackage: Property<String>

    /** Nombre del objeto generado. Default: ApiRoutes. */
    abstract val className: Property<String>

    /**
     * Posición de la versión en el .route ensamblado.
     *   BEFORE_TENANT → /v1/sales/users  (default, más común en APIs públicas)
     *   AFTER_TENANT  → /sales/v1/users
     *   NONE          → versión manejada externamente (header, BuildConfig, etc.)
     */
    abstract val versionPosition: Property<VersionPosition>
}

enum class VersionPosition {
    BEFORE_TENANT,
    AFTER_TENANT,
    NONE,
}
