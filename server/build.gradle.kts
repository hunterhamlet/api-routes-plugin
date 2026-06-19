plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    id("com.hamon.apiroutes")
}

group = "com.hamon.kmp_save_api"
version = "1.0.0"
application {
    mainClass = "com.hamon.kmp_save_api.ApplicationKt"
}

apiRoutes {
    // Uses the local api.toml (gitignored). Copy api.example.toml → api.toml to get started.
    outputPackage.set("com.hamon.kmp_save_api.server.routes")
}

tasks.register<JavaExec>("rickAndMortyDemo") {
    group = "demo"
    description = "Demuestra ApiRoutes en acción: llama a rickandmortyapi.com con constantes generadas desde api.toml"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.hamon.kmp_save_api.RickAndMortyDemoKt")
    dependsOn("compileKotlin")
}

dependencies {
    api(projects.core)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serializationJson)
    implementation(libs.ktor.clientCio)
    implementation(libs.ktor.clientContentNegotiation)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}
