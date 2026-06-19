package com.hamon.apiroutes.model

data class ApiConfig(
    val params: Map<String, String>,
    val paths: Map<String, String>,
    val versions: Map<String, String>,
    val tenants: Map<String, TenantDef>,
    val routes: Map<String, RouteDef>,
)

data class TenantDef(val path: String)

data class RouteDef(
    val tenantRef: String? = null,
    val versionRef: String? = null,
    val parentRef: String? = null,
    val pathRef: String? = null,
    val pathInline: String? = null,
    val paramRef: String? = null,
)

data class ResolvedRoute(
    val key: String,
    val tenantPath: String?,
    val versionPath: String?,
    val parentKey: String?,
    val ownPath: String,
    val accumulatedParams: List<String>,
)
