package com.hamon.apiroutes

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property

// T3 — implementation lives here
abstract class ApiRoutesExtension {

    abstract val tomlFile: RegularFileProperty

    abstract val outputDir: DirectoryProperty

    abstract val outputPackage: Property<String>

    abstract val className: Property<String>

    abstract val versionPosition: Property<VersionPosition>
}

enum class VersionPosition {
    BEFORE_TENANT,
    AFTER_TENANT,
    NONE,
}
