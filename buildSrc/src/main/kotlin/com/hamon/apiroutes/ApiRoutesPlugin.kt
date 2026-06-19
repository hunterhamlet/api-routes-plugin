package com.hamon.apiroutes

import org.gradle.api.Plugin
import org.gradle.api.Project

class ApiRoutesPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("apiRoutes", ApiRoutesExtension::class.java)

        extension.tomlFile.convention(
            project.rootProject.layout.projectDirectory.file("api.toml")
        )
        extension.outputDir.convention(
            project.layout.buildDirectory.dir("generated/apiRoutes")
        )
        extension.outputPackage.convention(
            project.provider {
                val group = project.group.toString()
                if (group.isNotEmpty()) "$group.routes" else "apiroutes"
            }
        )
        extension.className.convention("ApiRoutes")
        extension.versionPosition.convention(VersionPosition.BEFORE_TENANT)

        // Task registration y wiring de source sets → T6
    }
}
