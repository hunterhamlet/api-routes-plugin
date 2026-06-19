package com.hamon.kmp_save_api

import com.hamon.kmp_save_api.server.routes.ApiRoutes
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

private const val BASE = "https://rickandmortyapi.com"

fun main() = runBlocking {
    val client = HttpClient(CIO)

    println("""
        ┌──────────────────────────────────────────────────────────┐
        │        Rick & Morty Demo — powered by api-routes         │
        └──────────────────────────────────────────────────────────┘
    """.trimIndent())

    println("📄 Rutas generadas desde api.toml en tiempo de compilación:\n")
    println("   ApiRoutes.Characters.route               = \"${ApiRoutes.Characters.route}\"")
    println("   ApiRoutes.Characters.Character.route     = \"${ApiRoutes.Characters.Character.route}\"")
    println("   ApiRoutes.Characters.Character.Params.id = \"${ApiRoutes.Characters.Character.Params.id}\"")
    println("   ApiRoutes.Episodes.route                 = \"${ApiRoutes.Episodes.route}\"")
    println("   ApiRoutes.Episodes.Episode.route         = \"${ApiRoutes.Episodes.Episode.route}\"")
    println()

    // ── Personaje #1 ──────────────────────────────────────────────────────────
    val rickUrl = buildUrl(ApiRoutes.Characters.Character.route, ApiRoutes.Characters.Character.Params.id, "1")
    println("📡 GET $rickUrl")
    val rick = client.get(rickUrl).bodyAsText().toJson()
    println("   ✓ ${rick.str("name")} | ${rick.str("status")} | ${rick.str("species")} | ${rick.str("gender")}\n")

    // ── Personaje #2 ──────────────────────────────────────────────────────────
    val mortyUrl = buildUrl(ApiRoutes.Characters.Character.route, ApiRoutes.Characters.Character.Params.id, "2")
    println("📡 GET $mortyUrl")
    val morty = client.get(mortyUrl).bodyAsText().toJson()
    println("   ✓ ${morty.str("name")} | ${morty.str("status")} | ${morty.str("species")} | ${morty.str("gender")}\n")

    // ── Lista de personajes (page 1) ──────────────────────────────────────────
    println("📡 GET $BASE${ApiRoutes.Characters.route}")
    val chars = client.get("$BASE${ApiRoutes.Characters.route}").bodyAsText().toJson()
    val total = chars["info"]?.jsonObject?.get("count")
    val names = chars["results"]?.jsonArray?.take(5)?.map { it.jsonObject.str("name") }
    println("   ✓ Total personajes: $total")
    println("   ✓ Primeros 5: ${names?.joinToString(", ")}\n")

    // ── Episodio #1 ───────────────────────────────────────────────────────────
    val ep1Url = buildUrl(ApiRoutes.Episodes.Episode.route, ApiRoutes.Episodes.Episode.Params.id, "1")
    println("📡 GET $ep1Url")
    val ep = client.get(ep1Url).bodyAsText().toJson()
    println("   ✓ \"${ep.str("name")}\" | ${ep.str("air_date")} | ${ep.str("episode")}\n")

    println("✅ Todas las rutas provienen de api.toml → no hay strings hardcodeados en el código.")
    client.close()
}

private fun buildUrl(route: String, paramKey: String, value: String): String =
    "$BASE${route.replace("{$paramKey}", value)}"

private fun String.toJson(): JsonObject = Json.parseToJsonElement(this).jsonObject
private fun JsonObject.str(key: String): String = this[key]?.jsonPrimitive?.content ?: "?"
