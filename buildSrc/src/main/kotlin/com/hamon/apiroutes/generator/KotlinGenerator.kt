package com.hamon.apiroutes.generator

import com.hamon.apiroutes.VersionPosition
import com.hamon.apiroutes.model.ApiConfig
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File

// Fully resolved route — computed from ApiConfig by the generator.
internal data class ResolvedRoute(
    val key: String,
    val tenantRef: String?,
    val tenantPath: String?,
    val versionRef: String?,
    val versionPath: String?,
    val parentKey: String?,
    val ownPath: String,
    val ownParamName: String?,
    val accumulatedParams: List<String>,
)

object KotlinGenerator {

    fun generate(
        config: ApiConfig,
        outputDir: File,
        packageName: String,
        className: String,
        versionPosition: VersionPosition,
    ) {
        val resolved = resolveAllRoutes(config)
        buildFileSpec(resolved, config, packageName, className, versionPosition)
            .writeTo(outputDir.also { it.mkdirs() })
    }

    // ── resolution ────────────────────────────────────────────────────────

    internal fun resolveAllRoutes(config: ApiConfig): Map<String, ResolvedRoute> {
        val cache = mutableMapOf<String, ResolvedRoute>()

        fun resolve(key: String): ResolvedRoute = cache.getOrPut(key) {
            val def = config.routes[key]!!
            val parent = def.parentRef?.let { resolve(it) }

            val tenantRef = def.tenantRef ?: parent?.tenantRef
            val versionRef = def.versionRef ?: parent?.versionRef
            val paramName = def.paramRef?.let { config.params[it] }
            val pathPart = def.pathRef?.let { config.paths[it] } ?: def.pathInline ?: ""
            val ownPath = pathPart + (paramName?.let { "/{$it}" } ?: "")

            ResolvedRoute(
                key = key,
                tenantRef = tenantRef,
                tenantPath = tenantRef?.let { config.tenants[it]?.path },
                versionRef = versionRef,
                versionPath = versionRef?.let { config.versions[it] },
                parentKey = def.parentRef,
                ownPath = ownPath,
                ownParamName = paramName,
                accumulatedParams = (parent?.accumulatedParams ?: emptyList()) + listOfNotNull(paramName),
            )
        }

        return config.routes.keys.associateWith { resolve(it) }
    }

    internal fun assembleRoute(
        route: ResolvedRoute,
        versionPosition: VersionPosition,
        all: Map<String, ResolvedRoute>,
    ): String {
        val ancestor = buildAncestorPath(route, all).ifEmpty { null }
        val own = route.ownPath.ifEmpty { null }
        return when (versionPosition) {
            VersionPosition.BEFORE_TENANT -> listOfNotNull(route.versionPath, route.tenantPath, ancestor, own)
            VersionPosition.AFTER_TENANT  -> listOfNotNull(route.tenantPath, route.versionPath, ancestor, own)
            VersionPosition.NONE          -> listOfNotNull(route.tenantPath, ancestor, own)
        }.joinToString("")
    }

    private fun buildAncestorPath(route: ResolvedRoute, all: Map<String, ResolvedRoute>): String {
        val parent = route.parentKey?.let { all[it] } ?: return ""
        return buildAncestorPath(parent, all) + parent.ownPath
    }

    // ── KotlinPoet code generation ────────────────────────────────────────

    private fun buildFileSpec(
        resolved: Map<String, ResolvedRoute>,
        config: ApiConfig,
        packageName: String,
        className: String,
        versionPosition: VersionPosition,
    ): FileSpec {
        val apiObject = TypeSpec.objectBuilder(className).apply {
            buildApiRoutesContent(resolved, config, versionPosition).forEach { addType(it) }
        }.build()

        return FileSpec.builder(packageName, className)
            .addType(apiObject)
            .build()
    }

    private fun buildApiRoutesContent(
        resolved: Map<String, ResolvedRoute>,
        config: ApiConfig,
        versionPosition: VersionPosition,
    ): List<TypeSpec> {
        val rootRoutes = resolved.values.filter { it.parentKey == null }
        val result = mutableListOf<TypeSpec>()

        // No tenant, no version → directly in ApiRoutes
        rootRoutes.filter { it.tenantRef == null && it.versionRef == null }
            .forEach { result += buildRouteObject(it, resolved, versionPosition) }

        // Version only (no tenant) → ApiRoutes.V1 { ... }
        rootRoutes.filter { it.tenantRef == null && it.versionRef != null }
            .groupBy { it.versionRef!! }
            .forEach { (vKey, routes) ->
                result += buildVersionGroup(vKey, routes, resolved, versionPosition)
            }

        // Has tenant → ApiRoutes.Sales { path; [V1 { ... }]; [direct routes] }
        rootRoutes.filter { it.tenantRef != null }
            .groupBy { it.tenantRef!! }
            .forEach { (tKey, routes) ->
                result += buildTenantGroup(tKey, config, routes, resolved, versionPosition)
            }

        return result
    }

    private fun buildTenantGroup(
        tenantKey: String,
        config: ApiConfig,
        routes: List<ResolvedRoute>,
        all: Map<String, ResolvedRoute>,
        versionPosition: VersionPosition,
    ): TypeSpec = TypeSpec.objectBuilder(tenantKey.toPascalCase()).apply {
        addProperty(constProp("path", config.tenants[tenantKey]!!.path))

        routes.filter { it.versionRef == null }
            .forEach { addType(buildRouteObject(it, all, versionPosition)) }

        routes.filter { it.versionRef != null }
            .groupBy { it.versionRef!! }
            .forEach { (vKey, vRoutes) ->
                addType(buildVersionGroup(vKey, vRoutes, all, versionPosition))
            }
    }.build()

    private fun buildVersionGroup(
        versionKey: String,
        routes: List<ResolvedRoute>,
        all: Map<String, ResolvedRoute>,
        versionPosition: VersionPosition,
    ): TypeSpec = TypeSpec.objectBuilder(versionKey.toPascalCase()).apply {
        routes.forEach { addType(buildRouteObject(it, all, versionPosition)) }
    }.build()

    private fun buildRouteObject(
        route: ResolvedRoute,
        all: Map<String, ResolvedRoute>,
        versionPosition: VersionPosition,
    ): TypeSpec = TypeSpec.objectBuilder(route.key.toPascalCase()).apply {
        addProperty(constProp("path", route.ownPath))
        addProperty(constProp("route", assembleRoute(route, versionPosition, all)))

        if (route.accumulatedParams.isNotEmpty()) {
            addType(buildParamsObject(route.accumulatedParams))
        }

        all.values.filter { it.parentKey == route.key }
            .forEach { addType(buildRouteObject(it, all, versionPosition)) }
    }.build()

    private fun buildParamsObject(params: List<String>): TypeSpec =
        TypeSpec.objectBuilder("Params").apply {
            params.forEach { paramName ->
                addProperty(constProp(paramName, paramName))
            }
        }.build()

    private fun constProp(name: String, value: String): PropertySpec =
        PropertySpec.builder(name, String::class)
            .addModifiers(KModifier.CONST)
            .initializer("%S", value)
            .build()
}

internal fun String.toPascalCase(): String = split(Regex("[-_]"))
    .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
