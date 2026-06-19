package com.hamon.apiroutes.parser

import com.hamon.apiroutes.model.RouteDef
import com.hamon.apiroutes.model.TenantDef
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TomlParserTest {

    // ── happy path ────────────────────────────────────────────────────────

    @Test
    fun `parse full config with all sections`() {
        val config = TomlParser.parseContent("""
            [params]
            user-id = "userId"

            [paths]
            users = "/users"

            [versions]
            v1 = "/v1"

            [tenants]
            sales = { path = "/sales" }

            [routes]
            health = "/health"
            user-detail = { tenant.ref = "sales", version.ref = "v1", path.ref = "users", param.ref = "user-id" }
        """.trimIndent())

        assertEquals(mapOf("user-id" to "userId"), config.params)
        assertEquals(mapOf("users" to "/users"), config.paths)
        assertEquals(mapOf("v1" to "/v1"), config.versions)
        assertEquals(mapOf("sales" to TenantDef("/sales")), config.tenants)
        assertEquals(RouteDef(pathInline = "/health"), config.routes["health"])
        assertEquals(
            RouteDef(tenantRef = "sales", versionRef = "v1", pathRef = "users", paramRef = "user-id"),
            config.routes["user-detail"],
        )
    }

    @Test
    fun `parse route as plain string shorthand`() {
        val config = TomlParser.parseContent("""
            [routes]
            health = "/health"
        """.trimIndent())

        assertEquals(RouteDef(pathInline = "/health"), config.routes["health"])
    }

    @Test
    fun `parse route with inline path`() {
        val config = TomlParser.parseContent("""
            [routes]
            my-route = { path = "/foo" }
        """.trimIndent())

        assertEquals(RouteDef(pathInline = "/foo"), config.routes["my-route"])
    }

    @Test
    fun `parse route with path dot ref`() {
        val config = TomlParser.parseContent("""
            [paths]
            users = "/users"

            [routes]
            list-users = { path.ref = "users" }
        """.trimIndent())

        assertEquals(RouteDef(pathRef = "users"), config.routes["list-users"])
    }

    @Test
    fun `parse route with param dot ref`() {
        val config = TomlParser.parseContent("""
            [params]
            user-id = "userId"

            [paths]
            users = "/users"

            [routes]
            get-user = { path.ref = "users", param.ref = "user-id" }
        """.trimIndent())

        assertEquals(RouteDef(pathRef = "users", paramRef = "user-id"), config.routes["get-user"])
    }

    @Test
    fun `parse route with parent dot ref chain`() {
        val config = TomlParser.parseContent("""
            [params]
            user-id  = "userId"
            order-id = "orderId"

            [paths]
            users  = "/users"
            orders = "/orders"

            [routes]
            user-detail  = { path.ref = "users",  param.ref = "user-id" }
            user-orders  = { parent.ref = "user-detail", path.ref = "orders" }
            order-detail = { parent.ref = "user-orders", param.ref = "order-id" }
        """.trimIndent())

        assertEquals(
            RouteDef(parentRef = "user-detail", pathRef = "orders"),
            config.routes["user-orders"],
        )
        assertEquals(
            RouteDef(parentRef = "user-orders", paramRef = "order-id"),
            config.routes["order-detail"],
        )
    }

    @Test
    fun `tenant path resolves via path dot ref`() {
        val config = TomlParser.parseContent("""
            [paths]
            sales-path = "/sales"

            [tenants]
            sales = { path.ref = "sales-path" }

            [routes]
        """.trimIndent())

        assertEquals(TenantDef("/sales"), config.tenants["sales"])
    }

    @Test
    fun `inline comments are ignored`() {
        val config = TomlParser.parseContent("""
            [params]  # section comment
            user-id = "userId"  # inline comment

            [routes]
            health = "/health"  # route comment
        """.trimIndent())

        assertEquals("userId", config.params["user-id"])
        assertEquals(RouteDef(pathInline = "/health"), config.routes["health"])
    }

    @Test
    fun `empty sections produce empty maps`() {
        val config = TomlParser.parseContent("""
            [params]
            [paths]
            [versions]
            [tenants]
            [routes]
        """.trimIndent())

        assertEquals(emptyMap(), config.params)
        assertEquals(emptyMap(), config.routes)
    }

    // ── error cases ───────────────────────────────────────────────────────

    @Test
    fun `error on missing path ref`() {
        assertFailsWith<IllegalArgumentException> {
            TomlParser.parseContent("""
                [routes]
                my-route = { path.ref = "nonexistent" }
            """.trimIndent())
        }
    }

    @Test
    fun `error on missing param ref`() {
        assertFailsWith<IllegalArgumentException> {
            TomlParser.parseContent("""
                [routes]
                my-route = { path = "/foo", param.ref = "nonexistent" }
            """.trimIndent())
        }
    }

    @Test
    fun `error on missing tenant ref`() {
        assertFailsWith<IllegalArgumentException> {
            TomlParser.parseContent("""
                [routes]
                my-route = { tenant.ref = "nonexistent", path = "/foo" }
            """.trimIndent())
        }
    }

    @Test
    fun `error on missing version ref`() {
        assertFailsWith<IllegalArgumentException> {
            TomlParser.parseContent("""
                [routes]
                my-route = { version.ref = "nonexistent", path = "/foo" }
            """.trimIndent())
        }
    }

    @Test
    fun `error on missing parent ref`() {
        assertFailsWith<IllegalArgumentException> {
            TomlParser.parseContent("""
                [routes]
                my-route = { parent.ref = "nonexistent", path = "/foo" }
            """.trimIndent())
        }
    }

    @Test
    fun `error on cyclic parent ref`() {
        assertFailsWith<IllegalStateException> {
            TomlParser.parseContent("""
                [routes]
                route-a = { parent.ref = "route-b", path = "/a" }
                route-b = { parent.ref = "route-a", path = "/b" }
            """.trimIndent())
        }
    }

    @Test
    fun `error when path and path ref both declared`() {
        assertFailsWith<IllegalArgumentException> {
            TomlParser.parseContent("""
                [paths]
                users = "/users"

                [routes]
                bad-route = { path = "/foo", path.ref = "users" }
            """.trimIndent())
        }
    }
}
