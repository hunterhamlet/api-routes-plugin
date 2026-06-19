import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    id("com.hamon.apiroutes")
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    androidLibrary {
        namespace = "com.hamon.kmp_save_api.apiroutes"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
}

apiRoutes {
    // Use the committed example file; in production copy api.example.toml → api.toml
    // and point tomlFile to rootProject.layout.projectDirectory.file("api.toml").
    tomlFile.set(rootProject.layout.projectDirectory.file("api.example.toml"))
    outputPackage.set("com.hamon.kmp_save_api.routes")
}
