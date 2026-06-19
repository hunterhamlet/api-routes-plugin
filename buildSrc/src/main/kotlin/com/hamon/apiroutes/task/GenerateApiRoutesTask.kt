package com.hamon.apiroutes.task

import com.hamon.apiroutes.VersionPosition
import com.hamon.apiroutes.generator.KotlinGenerator
import com.hamon.apiroutes.parser.TomlParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

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
    fun generate() {
        val config = TomlParser.parse(tomlFile.get().asFile)
        KotlinGenerator.generate(
            config = config,
            outputDir = outputDir.get().asFile,
            packageName = outputPackage.get(),
            className = className.get(),
            versionPosition = VersionPosition.valueOf(versionPosition.get()),
        )
    }
}
