package com.hamon.kmp_save_api

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.hamon.kmp_save_api.app.routes.ApiRoutes
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

private const val BASE_URL = "https://rickandmortyapi.com"

private data class CharacterInfo(val name: String, val status: String, val species: String)

@Composable
fun RickAndMortyScreen() {
    val scope = rememberCoroutineScope()
    val client = remember { HttpClient() }
    var characters by remember { mutableStateOf<List<CharacterInfo>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { client.close() } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item {
            Text(
                "Rick & Morty — api-routes demo",
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "Rutas generadas desde api.toml",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    HorizontalDivider()
                    RouteRow("ApiRoutes.Characters.route", ApiRoutes.Characters.route)
                    RouteRow("ApiRoutes.Characters.Character.route", ApiRoutes.Characters.Character.route)
                    RouteRow("ApiRoutes.Characters.Character.Params.id", ApiRoutes.Characters.Character.Params.id)
                    RouteRow("ApiRoutes.Episodes.route", ApiRoutes.Episodes.route)
                    RouteRow("ApiRoutes.Episodes.Episode.route", ApiRoutes.Episodes.Episode.route)
                }
            }
        }

        item {
            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        error = null
                        characters = emptyList()
                        try {
                            val url = "$BASE_URL${ApiRoutes.Characters.route}"
                            val body = client.get(url).bodyAsText()
                            characters = Json.parseToJsonElement(body)
                                .jsonObject["results"]
                                ?.jsonArray
                                ?.take(10)
                                ?.map { el ->
                                    val o = el.jsonObject
                                    CharacterInfo(
                                        name = o["name"]?.jsonPrimitive?.content ?: "?",
                                        status = o["status"]?.jsonPrimitive?.content ?: "?",
                                        species = o["species"]?.jsonPrimitive?.content ?: "?",
                                    )
                                } ?: emptyList()
                        } catch (e: Exception) {
                            error = e.message ?: "Error desconocido"
                        }
                        loading = false
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (loading) "Cargando…" else "GET $BASE_URL${ApiRoutes.Characters.route}")
            }
        }

        error?.let { msg ->
            item {
                Text(msg, color = MaterialTheme.colorScheme.error)
            }
        }

        if (characters.isNotEmpty()) {
            item {
                Text("Personajes (primeros 10)", style = MaterialTheme.typography.titleSmall)
            }
            items(characters) { char ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(char.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${char.status} · ${char.species}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            "\"$value\"",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}