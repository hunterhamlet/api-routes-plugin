package com.hamon.apiroutes.generator

import com.hamon.apiroutes.model.ApiConfig
import com.hamon.apiroutes.model.ResolvedRoute
import com.squareup.kotlinpoet.FileSpec
import java.io.File

// T5 — implementation lives here
object KotlinGenerator {
    fun generate(config: ApiConfig, outputDir: File, packageName: String, className: String): Unit =
        TODO("T5")
}
