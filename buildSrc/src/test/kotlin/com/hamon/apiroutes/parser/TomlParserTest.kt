package com.hamon.apiroutes.parser

import kotlin.test.Test

// T4 — tests se implementan junto con TomlParser
class TomlParserTest {

    @Test
    fun `parse full config with all sections`() = TODO("T4")

    @Test
    fun `parse route as plain string shorthand`() = TODO("T4")

    @Test
    fun `parse route with inline path`() = TODO("T4")

    @Test
    fun `parse route with path dot ref`() = TODO("T4")

    @Test
    fun `parse route with param dot ref`() = TODO("T4")

    @Test
    fun `parse route with parent dot ref chain`() = TODO("T4")

    @Test
    fun `error on missing path ref`() = TODO("T4")

    @Test
    fun `error on missing param ref`() = TODO("T4")

    @Test
    fun `error on missing tenant ref`() = TODO("T4")

    @Test
    fun `error on missing version ref`() = TODO("T4")

    @Test
    fun `error on missing parent ref`() = TODO("T4")

    @Test
    fun `error on cyclic parent ref`() = TODO("T4")

    @Test
    fun `error when path and path ref both declared`() = TODO("T4")
}
