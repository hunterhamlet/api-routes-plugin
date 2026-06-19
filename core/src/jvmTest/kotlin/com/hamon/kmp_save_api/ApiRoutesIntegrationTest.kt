package com.hamon.kmp_save_api

import com.hamon.kmp_save_api.routes.ApiRoutes
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiRoutesIntegrationTest {

    // ── routes without tenant or version ─────────────────────────────────

    @Test
    fun `health route is accessible and correct`() {
        assertEquals("/health", ApiRoutes.Health.path)
        assertEquals("/health", ApiRoutes.Health.route)
    }

    // ── routes with version only ──────────────────────────────────────────

    @Test
    fun `versioned route without tenant is accessible`() {
        assertEquals("/status", ApiRoutes.V1.ApiStatus.path)
        assertEquals("/v1/status", ApiRoutes.V1.ApiStatus.route)
    }

    // ── routes with tenant only ───────────────────────────────────────────

    @Test
    fun `tenant route without version is accessible`() {
        assertEquals("/sales", ApiRoutes.Sales.path)
        assertEquals("/last-sale", ApiRoutes.Sales.LastSale.path)
        assertEquals("/sales/last-sale", ApiRoutes.Sales.LastSale.route)
    }

    // ── routes with tenant + version ──────────────────────────────────────

    @Test
    fun `user-detail route with tenant and version is accessible`() {
        assertEquals("/users/{userId}", ApiRoutes.Sales.V1.UserDetail.path)
        assertEquals("/v1/sales/users/{userId}", ApiRoutes.Sales.V1.UserDetail.route)
    }

    @Test
    fun `user-detail Params object exposes userId`() {
        assertEquals("userId", ApiRoutes.Sales.V1.UserDetail.Params.userId)
    }

    // ── nested child routes ───────────────────────────────────────────────

    @Test
    fun `user-orders is nested under user-detail`() {
        assertEquals("/orders", ApiRoutes.Sales.V1.UserDetail.UserOrders.path)
        assertEquals("/v1/sales/users/{userId}/orders", ApiRoutes.Sales.V1.UserDetail.UserOrders.route)
        assertEquals("userId", ApiRoutes.Sales.V1.UserDetail.UserOrders.Params.userId)
    }

    @Test
    fun `order-detail accumulates params from entire parent chain`() {
        assertEquals("/{orderId}", ApiRoutes.Sales.V1.UserDetail.UserOrders.OrderDetail.path)
        assertEquals(
            "/v1/sales/users/{userId}/orders/{orderId}",
            ApiRoutes.Sales.V1.UserDetail.UserOrders.OrderDetail.route,
        )
        assertEquals("userId", ApiRoutes.Sales.V1.UserDetail.UserOrders.OrderDetail.Params.userId)
        assertEquals("orderId", ApiRoutes.Sales.V1.UserDetail.UserOrders.OrderDetail.Params.orderId)
    }

    // ── second tenant (payments) ──────────────────────────────────────────

    @Test
    fun `payments routes are accessible`() {
        assertEquals("/payments", ApiRoutes.Payments.path)
        assertEquals("/v2/payments/sales-offer", ApiRoutes.Payments.V2.SalesOffer.route)
        assertEquals("/v2/payments/purchase-cart", ApiRoutes.Payments.V2.PurchaseCart.route)
    }
}
