package com.hamon.apiroutes.parser

import kotlin.test.Test

// T4 — tests se implementan junto con TomlParser
class TomlParserTest {

    @Test
    fun `parse full config with all sections`(): Unit = TODO("T4")

    @Test
    fun `parse route as plain string shorthand`(): Unit = TODO("T4")

    @Test
    fun `parse route with inline path`(): Unit = TODO("T4")

    @Test
    fun `parse route with path dot ref`(): Unit = TODO("T4")

    @Test
    fun `parse route with param dot ref`(): Unit = TODO("T4")

    @Test
    fun `parse route with parent dot ref chain`(): Unit = TODO("T4")

    @Test
    fun `error on missing path ref`(): Unit = TODO("T4")

    @Test
    fun `error on missing param ref`(): Unit = TODO("T4")

    @Test
    fun `error on missing tenant ref`(): Unit = TODO("T4")

    @Test
    fun `error on missing version ref`(): Unit = TODO("T4")

    @Test
    fun `error on missing parent ref`(): Unit = TODO("T4")

    @Test
    fun `error on cyclic parent ref`(): Unit = TODO("T4")

    @Test
    fun `error when path and path ref both declared`(): Unit = TODO("T4")
}
