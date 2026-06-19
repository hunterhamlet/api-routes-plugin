package com.hamon.apiroutes.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

// T6 — implementation lives here
abstract class GenerateApiRoutesTask : DefaultTask() {

    @get:InputFile
    abstract val tomlFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val outputPackage: Property<String>

    @get:Input
    abstract val className: Property<String>

    @get:Input
    abstract val versionPosition: Property<String>

    @TaskAction
    fun generate() = TODO("T6")
}
