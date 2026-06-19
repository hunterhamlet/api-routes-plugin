package com.hamon.kmp_save_api

import com.hamon.kmp_save_api.routes.ApiRoutes
import kotlin.test.Test
import kotlin.test.assertEquals

// Verifies that :server gets ApiRoutes transitively through api(projects.core) → projects.apiRoutes.
class ApiRoutesServerTest {

    @Test
    fun `server accesses health route via transitive core dependency`() {
        assertEquals("/health", ApiRoutes.Health.route)
    }

    @Test
    fun `server accesses nested route via transitive core dependency`() {
        assertEquals("/v1/sales/users/{userId}", ApiRoutes.Sales.V1.UserDetail.route)
        assertEquals("userId", ApiRoutes.Sales.V1.UserDetail.Params.userId)
    }
}
