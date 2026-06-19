package com.hamon.apiroutes.generator

import com.hamon.apiroutes.VersionPosition
import com.hamon.apiroutes.model.ApiConfig
import com.hamon.apiroutes.model.RouteDef
import com.hamon.apiroutes.model.TenantDef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KotlinGeneratorTest {

    // ── PascalCase ────────────────────────────────────────────────────────

    @Test
    fun `route name converts kebab-case to PascalCase`() {
        assertEquals("GetLastSale", "get-last-sale".toPascalCase())
        assertEquals("PurchaseCart", "purchase-cart".toPascalCase())
        assertEquals("UserDetail", "user-detail".toPascalCase())
    }

    @Test
    fun `route name converts snake_case to PascalCase`() {
        assertEquals("GetLastSale", "get_last_sale".toPascalCase())
        assertEquals("PurchaseCart", "purchase_cart".toPascalCase())
    }

    // ── resolveAllRoutes ──────────────────────────────────────────────────

    @Test
    fun `resolve route with no tenant and no version`() {
        val config = simpleConfig(routes = mapOf(
            "health" to RouteDef(pathInline = "/health"),
        ))
        val resolved = KotlinGenerator.resolveAllRoutes(config)

        with(resolved["health"]!!) {
            assertEquals("/health", ownPath)
            assertEquals(null, tenantRef)
            assertEquals(null, versionRef)
            assertEquals(null, parentKey)
            assertEquals(emptyList(), accumulatedParams)
        }
    }

    @Test
    fun `resolve route with path ref and param ref`() {
        val config = simpleConfig(
            params = mapOf("user-id" to "userId"),
            paths = mapOf("users" to "/users"),
            routes = mapOf(
                "user-detail" to RouteDef(pathRef = "users", paramRef = "user-id"),
            ),
        )
        val resolved = KotlinGenerator.resolveAllRoutes(config)

        with(resolved["user-detail"]!!) {
            assertEquals("/users/{userId}", ownPath)
            assertEquals("userId", ownParamName)
            assertEquals(listOf("userId"), accumulatedParams)
        }
    }

    @Test
    fun `resolve child route inherits tenant and version from parent`() {
        val config = simpleConfig(
            params = mapOf("user-id" to "userId", "order-id" to "orderId"),
            paths = mapOf("users" to "/users", "orders" to "/orders"),
            versions = mapOf("v1" to "/v1"),
            tenants = mapOf("sales" to TenantDef("/sales")),
            routes = mapOf(
                "user-detail"  to RouteDef(tenantRef = "sales", versionRef = "v1", pathRef = "users", paramRef = "user-id"),
                "user-orders"  to RouteDef(parentRef = "user-detail", pathRef = "orders"),
                "order-detail" to RouteDef(parentRef = "user-orders", paramRef = "order-id"),
            ),
        )
        val resolved = KotlinGenerator.resolveAllRoutes(config)

        with(resolved["user-orders"]!!) {
            assertEquals("sales", tenantRef)
            assertEquals("/sales", tenantPath)
            assertEquals("v1", versionRef)
            assertEquals("/v1", versionPath)
            assertEquals("/orders", ownPath)
            assertEquals(listOf("userId"), accumulatedParams)
        }

        with(resolved["order-detail"]!!) {
            assertEquals("sales", tenantRef)
            assertEquals("v1", versionRef)
            assertEquals("/{orderId}", ownPath)
            assertEquals(listOf("userId", "orderId"), accumulatedParams)
        }
    }

    // ── assembleRoute ─────────────────────────────────────────────────────

    @Test
    fun `generate route with no tenant and no version`() {
        val config = simpleConfig(routes = mapOf("health" to RouteDef(pathInline = "/health")))
        val resolved = KotlinGenerator.resolveAllRoutes(config)
        val route = assembleRoute("health", resolved, VersionPosition.BEFORE_TENANT)
        assertEquals("/health", route)
    }

    @Test
    fun `generate route with version only`() {
        val config = simpleConfig(
            versions = mapOf("v1" to "/v1"),
            routes = mapOf("status" to RouteDef(versionRef = "v1", pathInline = "/status")),
        )
        val resolved = KotlinGenerator.resolveAllRoutes(config)
        assertEquals("/v1/status", assembleRoute("status", resolved, VersionPosition.BEFORE_TENANT))
        assertEquals("/status", assembleRoute("status", resolved, VersionPosition.NONE))
    }

    @Test
    fun `generate route with tenant only`() {
        val config = simpleConfig(
            tenants = mapOf("sales" to TenantDef("/sales")),
            routes = mapOf("last-sale" to RouteDef(tenantRef = "sales", pathInline = "/last-sale")),
        )
        val resolved = KotlinGenerator.resolveAllRoutes(config)
        assertEquals("/sales/last-sale", assembleRoute("last-sale", resolved, VersionPosition.BEFORE_TENANT))
    }

    @Test
    fun `generate route with tenant and version BEFORE_TENANT`() {
        val resolved = fullExampleResolved()
        assertEquals(
            "/v1/sales/users/{userId}",
            assembleRoute("user-detail", resolved, VersionPosition.BEFORE_TENANT),
        )
    }

    @Test
    fun `generate route with tenant and version AFTER_TENANT`() {
        val resolved = fullExampleResolved()
        assertEquals(
            "/sales/v1/users/{userId}",
            assembleRoute("user-detail", resolved, VersionPosition.AFTER_TENANT),
        )
    }

    @Test
    fun `generate nested route via parent ref`() {
        val resolved = fullExampleResolved()
        assertEquals(
            "/v1/sales/users/{userId}/orders/{orderId}",
            assembleRoute("order-detail", resolved, VersionPosition.BEFORE_TENANT),
        )
    }

    // ── generated file content ────────────────────────────────────────────

    @Test
    fun `generate Params object with own param`() {
        val content = generate(
            params = mapOf("user-id" to "userId"),
            paths = mapOf("users" to "/users"),
            routes = mapOf("user-detail" to RouteDef(pathRef = "users", paramRef = "user-id")),
        )
        assertTrue(content.contains("object Params"), "Expected Params object")
        assertTrue(content.contains("""userId: String = "userId""""))
    }

    @Test
    fun `generate Params object accumulates params from parent chain`() {
        val content = generate(
            params = mapOf("user-id" to "userId", "order-id" to "orderId"),
            paths = mapOf("users" to "/users", "orders" to "/orders"),
            versions = mapOf("v1" to "/v1"),
            tenants = mapOf("sales" to TenantDef("/sales")),
            routes = mapOf(
                "user-detail"  to RouteDef(tenantRef = "sales", versionRef = "v1", pathRef = "users", paramRef = "user-id"),
                "user-orders"  to RouteDef(parentRef = "user-detail", pathRef = "orders"),
                "order-detail" to RouteDef(parentRef = "user-orders", paramRef = "order-id"),
            ),
        )
        // OrderDetail should accumulate both params
        val orderDetailIdx = content.indexOf("object OrderDetail")
        assertTrue(orderDetailIdx >= 0)
        val afterOrderDetail = content.substring(orderDetailIdx)
        assertTrue(afterOrderDetail.contains("""userId: String = "userId""""))
        assertTrue(afterOrderDetail.contains("""orderId: String = "orderId""""))
    }

    @Test
    fun `tenant object exposes path const`() {
        val content = generate(
            tenants = mapOf("sales" to TenantDef("/sales")),
            routes = mapOf("last-sale" to RouteDef(tenantRef = "sales", pathInline = "/last-sale")),
        )
        val salesIdx = content.indexOf("object Sales")
        assertTrue(salesIdx >= 0)
        val afterSales = content.substring(salesIdx)
        assertTrue(afterSales.contains("""path: String = "/sales""""))
    }

    @Test
    fun `route without tenant or version appears directly in ApiRoutes`() {
        val content = generate(routes = mapOf("health" to RouteDef(pathInline = "/health")))
        // Health should be nested directly under ApiRoutes (near the top)
        assertTrue(content.contains("object Health"))
        assertFalse(content.contains("object V1"))
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private fun simpleConfig(
        params: Map<String, String> = emptyMap(),
        paths: Map<String, String> = emptyMap(),
        versions: Map<String, String> = emptyMap(),
        tenants: Map<String, TenantDef> = emptyMap(),
        routes: Map<String, RouteDef> = emptyMap(),
    ) = ApiConfig(params, paths, versions, tenants, routes)

    private fun fullExampleResolved(): Map<String, ResolvedRoute> {
        val config = simpleConfig(
            params = mapOf("user-id" to "userId", "order-id" to "orderId"),
            paths = mapOf("users" to "/users", "orders" to "/orders"),
            versions = mapOf("v1" to "/v1"),
            tenants = mapOf("sales" to TenantDef("/sales")),
            routes = mapOf(
                "user-detail"  to RouteDef(tenantRef = "sales", versionRef = "v1", pathRef = "users", paramRef = "user-id"),
                "user-orders"  to RouteDef(parentRef = "user-detail", pathRef = "orders"),
                "order-detail" to RouteDef(parentRef = "user-orders", paramRef = "order-id"),
            ),
        )
        return KotlinGenerator.resolveAllRoutes(config)
    }

    private fun assembleRoute(
        key: String,
        resolved: Map<String, ResolvedRoute>,
        versionPosition: VersionPosition,
    ) = KotlinGenerator.assembleRoute(resolved[key]!!, versionPosition, resolved)

    private fun generate(
        params: Map<String, String> = emptyMap(),
        paths: Map<String, String> = emptyMap(),
        versions: Map<String, String> = emptyMap(),
        tenants: Map<String, TenantDef> = emptyMap(),
        routes: Map<String, RouteDef> = emptyMap(),
        versionPosition: VersionPosition = VersionPosition.BEFORE_TENANT,
    ): String {
        val config = ApiConfig(params, paths, versions, tenants, routes)
        val outputDir = createTempDir("apiroutes-test")
        KotlinGenerator.generate(config, outputDir, "com.test", "ApiRoutes", versionPosition)
        return outputDir.walkTopDown().first { it.extension == "kt" }.readText()
    }
}
