package com.hamon.apiroutes

import com.hamon.apiroutes.task.GenerateApiRoutesTask
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.TaskProvider

class ApiRoutesPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create("apiRoutes", ApiRoutesExtension::class.java)

        extension.tomlFile.convention(project.rootProject.layout.projectDirectory.file("api.toml"))
        extension.outputDir.convention(project.layout.buildDirectory.dir("generated/apiRoutes"))
        extension.outputPackage.convention(project.provider {
            val group = project.group.toString()
            if (group.isNotEmpty()) "$group.routes" else "apiroutes"
        })
        extension.className.convention("ApiRoutes")
        extension.versionPosition.convention(VersionPosition.BEFORE_TENANT)

        // Two-step register + configure: avoids the KotlinDSL vararg-extension ambiguity on register().
        val taskProvider = project.tasks.register("generateApiRoutes", GenerateApiRoutesTask::class.java)
        taskProvider.configure {
            // `this` = GenerateApiRoutesTask (Gradle Kotlin DSL uses receiver lambdas for Action<T>)
            group = "apiroutes"
            description = "Generates ApiRoutes Kotlin object from api.toml"
            tomlFile.set(extension.tomlFile)
            outputDir.set(extension.outputDir)
            outputPackage.set(extension.outputPackage)
            className.set(extension.className)
            versionPosition.set(extension.versionPosition.map { it.name })
        }

        project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
            wireSourceSet(project, "commonMain", extension.outputDir)
            wireCompileTasks(project, taskProvider)
        }
        project.plugins.withId("org.jetbrains.kotlin.jvm") {
            wireSourceSet(project, "main", extension.outputDir)
            project.tasks.configureEach {
                if (name == "compileKotlin") dependsOn(taskProvider)
            }
        }
        project.plugins.withId("org.jetbrains.kotlin.android") {
            wireSourceSet(project, "main", extension.outputDir)
            wireCompileTasks(project, taskProvider)
        }
    }

    // Adds the generated output dir to the named Kotlin source set via reflection so that
    // the plugin does not need a direct compile-time dep on a specific KGP version.
    private fun wireSourceSet(project: Project, sourceSetName: String, outputDir: DirectoryProperty) {
        val ext = project.extensions.findByName("kotlin") ?: return
        try {
            @Suppress("UNCHECKED_CAST")
            val sourceSets = ext.javaClass.getMethod("getSourceSets").invoke(ext)
                    as NamedDomainObjectCollection<*>
            val sourceSet = sourceSets.findByName(sourceSetName) ?: return
            val kotlinDirSet = sourceSet.javaClass.getMethod("getKotlin").invoke(sourceSet)
                    as SourceDirectorySet
            kotlinDirSet.srcDir(outputDir)
        } catch (e: Exception) {
            project.logger.warn("[api-routes] Source set '$sourceSetName' wiring failed: ${e.message}")
        }
    }

    private fun wireCompileTasks(project: Project, taskProvider: TaskProvider<GenerateApiRoutesTask>) {
        project.tasks.configureEach {
            if (name.startsWith("compile") && "Kotlin" in name && "Test" !in name) {
                dependsOn(taskProvider)
            }
        }
    }
}
