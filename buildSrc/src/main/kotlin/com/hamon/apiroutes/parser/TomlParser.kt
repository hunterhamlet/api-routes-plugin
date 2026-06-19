package com.hamon.apiroutes.parser

import com.hamon.apiroutes.model.*
import java.io.File

object TomlParser {

    fun parse(file: File): ApiConfig = parseContent(file.readText())

    internal fun parseContent(content: String): ApiConfig {
        // ── Stage 1: collect raw sections ────────────────────────────────
        val rawParams   = mutableMapOf<String, String>()
        val rawPaths    = mutableMapOf<String, String>()
        val rawVersions = mutableMapOf<String, String>()
        // tenant value → inline table fields (path or path.ref)
        val rawTenants  = mutableMapOf<String, Map<String, String>>()
        // route value → String (shorthand) or Map<String, String> (inline table)
        val rawRoutes   = mutableMapOf<String, Any>()

        var section = ""

        for (rawLine in content.lines()) {
            val line = stripComment(rawLine).trim()
            if (line.isBlank()) continue

            val sectionMatch = SECTION_REGEX.matchEntire(line)
            if (sectionMatch != null) {
                section = sectionMatch.groupValues[1].trim()
                continue
            }

            val eqIdx = line.indexOf('=')
            if (eqIdx < 0) continue

            val key      = line.substring(0, eqIdx).trim()
            val valueStr = line.substring(eqIdx + 1).trim()

            when (section) {
                "params"   -> rawParams[key]   = requireQuotedString(valueStr, "params.$key")
                "paths"    -> rawPaths[key]    = requireQuotedString(valueStr, "paths.$key")
                "versions" -> rawVersions[key] = requireQuotedString(valueStr, "versions.$key")
                "tenants"  -> rawTenants[key]  = requireInlineTable(valueStr, "tenants.$key")
                "routes"   -> rawRoutes[key]   =
                    if (isQuotedString(valueStr)) requireQuotedString(valueStr, "routes.$key")
                    else requireInlineTable(valueStr, "routes.$key")
            }
        }

        // ── Stage 2: resolve refs and build model ────────────────────────
        val params   = rawParams.toMap()
        val paths    = rawPaths.toMap()
        val versions = rawVersions.toMap()

        val tenants = rawTenants.mapValues { (key, fields) ->
            TenantDef(resolvePathOrRef(fields, paths, "tenants.$key"))
        }

        val routes = rawRoutes.mapValues { (key, raw) ->
            buildRouteDef(key, raw)
        }

        validateRefs(params, paths, versions, tenants, routes)

        return ApiConfig(params, paths, versions, tenants, routes)
    }

    // ── parsing helpers ───────────────────────────────────────────────────

    private val SECTION_REGEX = Regex("""^\[([^\]]+)\]$""")

    private fun stripComment(line: String): String {
        var inQuote = false
        var quoteChar = ' '
        line.forEachIndexed { i, ch ->
            when {
                !inQuote && (ch == '"' || ch == '\'') -> { inQuote = true; quoteChar = ch }
                inQuote && ch == quoteChar             -> inQuote = false
                !inQuote && ch == '#'                  -> return line.substring(0, i)
            }
        }
        return line
    }

    private fun isQuotedString(value: String) =
        (value.startsWith('"') && value.endsWith('"')) ||
        (value.startsWith('\'') && value.endsWith('\''))

    private fun requireQuotedString(value: String, context: String): String {
        require(isQuotedString(value)) { "$context: expected a quoted string, got: $value" }
        return value.drop(1).dropLast(1)
    }

    private fun requireInlineTable(value: String, context: String): Map<String, String> {
        val trimmed = value.trim()
        require(trimmed.startsWith('{') && trimmed.endsWith('}')) {
            "$context: expected inline table '{ ... }', got: $trimmed"
        }
        val inner = trimmed.drop(1).dropLast(1).trim()
        if (inner.isEmpty()) return emptyMap()

        return splitByComma(inner).associate { entry ->
            val eqIdx = entry.indexOf('=')
            require(eqIdx > 0) { "$context: invalid entry in inline table: '$entry'" }
            val k = entry.substring(0, eqIdx).trim()
            val v = requireQuotedString(entry.substring(eqIdx + 1).trim(), "$context.$k")
            k to v
        }
    }

    private fun splitByComma(s: String): List<String> {
        val parts   = mutableListOf<String>()
        val current = StringBuilder()
        var inQuote = false
        var quoteChar = ' '

        for (ch in s) {
            when {
                !inQuote && (ch == '"' || ch == '\'') -> { inQuote = true; quoteChar = ch; current.append(ch) }
                inQuote && ch == quoteChar             -> { inQuote = false; current.append(ch) }
                !inQuote && ch == ','                  -> { parts += current.toString().trim(); current.clear() }
                else                                   -> current.append(ch)
            }
        }
        if (current.isNotBlank()) parts += current.toString().trim()
        return parts
    }

    private fun resolvePathOrRef(
        fields: Map<String, String>,
        paths: Map<String, String>,
        context: String,
    ): String {
        val path    = fields["path"]
        val pathRef = fields["path.ref"]
        require(path == null || pathRef == null) { "$context: cannot declare both 'path' and 'path.ref'" }
        require(path != null || pathRef != null) { "$context: must declare either 'path' or 'path.ref'" }

        if (path != null) return path
        val resolved = paths[pathRef!!]
        require(resolved != null) { "$context: path.ref '$pathRef' not found in [paths]" }
        return resolved
    }

    private fun buildRouteDef(key: String, raw: Any): RouteDef {
        if (raw is String) return RouteDef(pathInline = raw)

        @Suppress("UNCHECKED_CAST")
        val fields = raw as Map<String, String>

        val pathInline = fields["path"]
        val pathRef    = fields["path.ref"]
        require(pathInline == null || pathRef == null) {
            "Route '$key': cannot declare both 'path' and 'path.ref'"
        }

        return RouteDef(
            tenantRef  = fields["tenant.ref"],
            versionRef = fields["version.ref"],
            parentRef  = fields["parent.ref"],
            pathRef    = pathRef,
            pathInline = pathInline,
            paramRef   = fields["param.ref"],
        )
    }

    // ── validation ────────────────────────────────────────────────────────

    private fun validateRefs(
        params: Map<String, String>,
        paths: Map<String, String>,
        versions: Map<String, String>,
        tenants: Map<String, TenantDef>,
        routes: Map<String, RouteDef>,
    ) {
        for ((key, route) in routes) {
            route.pathRef?.let { ref ->
                require(ref in paths) { "Route '$key': path.ref '$ref' not found in [paths]" }
            }
            route.paramRef?.let { ref ->
                require(ref in params) { "Route '$key': param.ref '$ref' not found in [params]" }
            }
            route.tenantRef?.let { ref ->
                require(ref in tenants) { "Route '$key': tenant.ref '$ref' not found in [tenants]" }
            }
            route.versionRef?.let { ref ->
                require(ref in versions) { "Route '$key': version.ref '$ref' not found in [versions]" }
            }
            route.parentRef?.let { ref ->
                require(ref in routes) { "Route '$key': parent.ref '$ref' not found in [routes]" }
            }
        }

        for (key in routes.keys) detectCycle(key, routes, emptySet())
    }

    private fun detectCycle(key: String, routes: Map<String, RouteDef>, visited: Set<String>) {
        if (key in visited) error("Cyclic parent.ref detected: ${(visited + key).joinToString(" → ")}")
        routes[key]?.parentRef?.let { parent ->
            detectCycle(parent, routes, visited + key)
        }
    }
}
