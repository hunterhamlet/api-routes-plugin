package com.hamon.kmp_save_api

import com.hamon.kmp_save_api.server.routes.ApiRoutes
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private const val RICK_AND_MORTY_BASE = "https://rickandmortyapi.com"

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
    }

    routing {
        get("/") {
            call.respondText(sayHello("Ktor"))
        }

        // ── Rick and Morty proxy ──────────────────────────────────────────
        // Routes come from api.toml → generated ApiRoutes (server.routes package).
        // Changing api.toml renames/moves these paths at compile time.

        get(ApiRoutes.Characters.route) {
            val upstream = httpClient.get("$RICK_AND_MORTY_BASE${ApiRoutes.Characters.route}")
            call.respondText(upstream.bodyAsText(), ContentType.Application.Json)
        }

        get(ApiRoutes.Characters.Character.route) {
            val id = call.parameters[ApiRoutes.Characters.Character.Params.id]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing id")
            val upstream = httpClient.get(
                "$RICK_AND_MORTY_BASE${ApiRoutes.Characters.Character.route}"
                    .replace("{${ApiRoutes.Characters.Character.Params.id}}", id)
            )
            call.respondText(upstream.bodyAsText(), ContentType.Application.Json)
        }

        get(ApiRoutes.Episodes.route) {
            val upstream = httpClient.get("$RICK_AND_MORTY_BASE${ApiRoutes.Episodes.route}")
            call.respondText(upstream.bodyAsText(), ContentType.Application.Json)
        }
    }
}
