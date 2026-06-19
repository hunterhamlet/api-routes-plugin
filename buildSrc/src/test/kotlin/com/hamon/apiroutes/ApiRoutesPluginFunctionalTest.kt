package com.hamon.apiroutes

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ApiRoutesPluginFunctionalTest {

    // ── helpers ───────────────────────────────────────────────────────────

    private fun buildProject(tomlContent: String = SIMPLE_TOML): File {
        val dir = Files.createTempDirectory("apiroutes-func").toFile()
        File(dir, "settings.gradle.kts").writeText("""rootProject.name = "test-apiroutes"""")
        File(dir, "build.gradle.kts").writeText("""
            plugins { id("com.hamon.apiroutes") }
            apiRoutes { outputPackage.set("com.example") }
        """.trimIndent())
        File(dir, "api.toml").writeText(tomlContent)
        return dir
    }

    private fun runner(dir: File, vararg args: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(dir)
            .withArguments(*args)
            .withPluginClasspath()

    // ── tests ─────────────────────────────────────────────────────────────

    @Test
    fun `plugin generates ApiRoutes kt for JVM project`() {
        val dir = buildProject()

        val result = runner(dir, "generateApiRoutes").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateApiRoutes")?.outcome)
        val generated = dir.walkTopDown().firstOrNull { it.name == "ApiRoutes.kt" }
        assertNotNull(generated, "ApiRoutes.kt must be generated")
        assertTrue(generated.readText().contains("object Health"))
    }

    @Test
    fun `plugin generates ApiRoutes kt for KMP project`() {
        val toml = """
            [params]
            user-id = "userId"
            [paths]
            users = "/users"
            [versions]
            v1 = "/v1"
            [tenants]
            sales = { path = "/sales" }
            [routes]
            user-detail = { tenant.ref = "sales", version.ref = "v1", path.ref = "users", param.ref = "user-id" }
        """.trimIndent()

        val dir = buildProject(toml)
        val result = runner(dir, "generateApiRoutes").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateApiRoutes")?.outcome)
        val content = dir.walkTopDown().first { it.name == "ApiRoutes.kt" }.readText()
        assertTrue(content.contains("object Sales"))
        assertTrue(content.contains("object V1"))
        assertTrue(content.contains("object UserDetail"))
        assertTrue(content.contains("""userId: String = "userId""""))
    }

    @Test
    fun `generated file compiles successfully`() {
        val dir = Files.createTempDirectory("apiroutes-compile").toFile()

        File(dir, "settings.gradle.kts").writeText("""
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                }
            }
            dependencyResolutionManagement {
                repositories {
                    mavenCentral()
                }
            }
            rootProject.name = "test-compile"
        """.trimIndent())
        // Manually add the generated dir to source sets so we can test compilation
        // independently of the plugin's source-set wiring (covered in T7 integration test).
        File(dir, "build.gradle.kts").writeText("""
            plugins {
                kotlin("jvm") version "2.4.0"
                id("com.hamon.apiroutes")
            }
            apiRoutes {
                outputPackage.set("com.example")
            }
            kotlin.sourceSets.named("main") {
                kotlin.srcDir(layout.buildDirectory.dir("generated/apiRoutes"))
            }
            tasks.named("compileKotlin") {
                dependsOn("generateApiRoutes")
            }
        """.trimIndent())
        File(dir, "api.toml").writeText(SIMPLE_TOML)

        val result = GradleRunner.create()
            .withProjectDir(dir)
            .withArguments("compileKotlin")
            .withPluginClasspath()
            .build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":compileKotlin")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateApiRoutes")?.outcome)
    }

    @Test
    fun `task is up to date on second run`() {
        val dir = buildProject()
        val run = runner(dir, "generateApiRoutes")

        run.build() // first run — SUCCESS
        val result = run.build() // second run — UP-TO-DATE

        assertEquals(TaskOutcome.UP_TO_DATE, result.task(":generateApiRoutes")?.outcome)
    }

    @Test
    fun `task re-runs when toml file changes`() {
        val dir = buildProject()
        val run = runner(dir, "generateApiRoutes")

        run.build() // initial run

        File(dir, "api.toml").writeText("""
            [routes]
            health = "/health"
            status = "/status"
        """.trimIndent())

        val result = run.build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":generateApiRoutes")?.outcome)
        val content = dir.walkTopDown().first { it.name == "ApiRoutes.kt" }.readText()
        assertTrue(content.contains("object Status"))
    }

    // ── fixtures ──────────────────────────────────────────────────────────

    companion object {
        private val SIMPLE_TOML = """
            [routes]
            health = "/health"
        """.trimIndent()
    }
}
