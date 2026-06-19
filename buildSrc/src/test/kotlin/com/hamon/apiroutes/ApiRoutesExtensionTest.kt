package com.hamon.apiroutes

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApiRoutesExtensionTest {

    private fun buildProject(group: String = "") =
        ProjectBuilder.builder().build().also { p ->
            if (group.isNotEmpty()) p.group = group
            p.pluginManager.apply("com.hamon.apiroutes")
        }

    private fun Project.ext() = extensions.getByType(ApiRoutesExtension::class.java)

    @Test
    fun `default className is ApiRoutes`() {
        assertEquals("ApiRoutes", buildProject().ext().className.get())
    }

    @Test
    fun `default versionPosition is BEFORE_TENANT`() {
        assertEquals(VersionPosition.BEFORE_TENANT, buildProject().ext().versionPosition.get())
    }

    @Test
    fun `default tomlFile points to api toml in root project`() {
        val project = buildProject()
        val expected = project.rootProject.projectDir.resolve("api.toml").canonicalPath
        assertEquals(expected, project.ext().tomlFile.get().asFile.canonicalPath)
    }

    @Test
    fun `default outputDir is under build generated apiRoutes`() {
        val path = buildProject().ext().outputDir.get().asFile.path
        assertTrue(path.endsWith("generated/apiRoutes"), "Expected path ending in generated/apiRoutes but was: $path")
    }

    @Test
    fun `default outputPackage is apiroutes when project group is empty`() {
        assertEquals("apiroutes", buildProject().ext().outputPackage.get())
    }

    @Test
    fun `outputPackage appends routes to project group`() {
        assertEquals("com.hamon.kmp.routes", buildProject("com.hamon.kmp").ext().outputPackage.get())
    }

    @Test
    fun `user can override className`() {
        val project = buildProject()
        project.ext().className.set("MyApiRoutes")
        assertEquals("MyApiRoutes", project.ext().className.get())
    }

    @Test
    fun `user can override versionPosition to AFTER_TENANT`() {
        val project = buildProject()
        project.ext().versionPosition.set(VersionPosition.AFTER_TENANT)
        assertEquals(VersionPosition.AFTER_TENANT, project.ext().versionPosition.get())
    }

    @Test
    fun `user can override versionPosition to NONE`() {
        val project = buildProject()
        project.ext().versionPosition.set(VersionPosition.NONE)
        assertEquals(VersionPosition.NONE, project.ext().versionPosition.get())
    }
}

// Alias para hacer el código de test más conciso
private typealias Project = org.gradle.api.Project
