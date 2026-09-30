/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UnzipCompressorTest {
    @Test
    fun storedFixedAndDynamicDeflateBlocksMatchOfficialFixture() {
        val stored = unzip(STORED_ZIP)
        assertEquals(
            listOf(
                UnzippedFile(
                    content = "stored-block:0123456789",
                    name = "stored.txt",
                ),
            ),
            stored,
        )

        val fixed = unzip(FIXED_ZIP)
        assertEquals(1, fixed.size)
        assertEquals("nested/fixed.txt", fixed.single().name)
        assertEquals(
            "fixed-block:" + "ABRACADABRA-".repeat(32),
            fixed.single().content,
        )

        val dynamic = unzip(DYNAMIC_ZIP)
        assertEquals(1, dynamic.size)
        assertEquals("dynamic.txt", dynamic.single().name)
        assertEquals(dynamicContent(), dynamic.single().content)
    }

    @Test
    fun gzipZlibAndMultiFileZipMatchOfficialFixture() {
        val gzip = unzip(GZIP)
        assertEquals(
            listOf(
                UnzippedFile(
                    content = "gzip:" + "XYZ-".repeat(48),
                    name = "file",
                ),
            ),
            gzip,
        )
        assertEquals(
            listOf(
                UnzippedFile(
                    content = "gzip-header:" + "Q-".repeat(32),
                    name = "file",
                ),
            ),
            unzip(GZIP_WITH_HEADERS),
        )

        val zlib = unzip(ZLIB)
        assertEquals(1, zlib.size)
        assertEquals("geonext.gxt", zlib.single().name)
        assertEquals(
            "JXG.create('point'%2C%20%5B1%2C%202%5D)%3B%20" +
                "Gr%C3%BC%C3%9Fe%20%F0%9F%98%80",
            zlib.single().content,
        )

        val multi = unzip(MULTI_ZIP)
        assertEquals(
            listOf("nested/fixed.txt", "dynamic.txt"),
            multi.map(UnzippedFile::name),
        )
        assertEquals(
            "fixed-block:" + "ABRACADABRA-".repeat(32),
            multi[0].content,
        )
        assertEquals(dynamicContent(), multi[1].content)
        assertEquals(
            listOf(
                UnzippedFile(
                    content =
                        "fixed-block:" +
                            "ABRACADABRA-".repeat(32),
                    name = "descriptor.txt",
                ),
            ),
            unzip(DESCRIPTOR_ZIP),
        )
    }

    @Test
    fun unzipFileAndCompressorMatchOfficialFixture() {
        val archive = binaryInput(MULTI_ZIP)
        assertEquals(
            dynamicContent(),
            assertIs<GMResult.Ok<String>>(
                Unzip(archive).unzipFile("dynamic.txt"),
            ).value,
        )
        assertEquals(
            "",
            assertIs<GMResult.Ok<String>>(
                Unzip(archive).unzipFile("missing.txt"),
            ).value,
        )
        assertEquals(
            "JXG.create('point', [1, 2]); Grüße 😀",
            assertIs<GMResult.Ok<String>>(
                Compressor.decompress(ZLIB),
            ).value,
        )
    }

    @Test
    fun archiveResourceFailuresAreStructured() {
        assertIs<GMResult.Err<UnzipError.InputLimitExceeded>>(
            Unzip(
                input = listOf(1, 2, 3, 4),
                limits = UnzipLimits(maxInputBytes = 3),
            ).unzip(),
        )
        assertIs<GMResult.Err<UnzipError.InvalidInputByte>>(
            Unzip(listOf(0x78, 0xDA, 256)).unzip(),
        )
        assertIs<GMResult.Err<UnzipError.UnsupportedContainer>>(
            Unzip(listOf(1, 2, 3, 4)).unzip(),
        )
        assertIs<GMResult.Err<UnzipError.OutputLimitExceeded>>(
            Unzip(
                input = binaryInput(STORED_ZIP),
                limits = UnzipLimits(maxOutputBytes = 22),
            ).unzip(),
        )
        assertIs<GMResult.Err<UnzipError.FileCountLimitExceeded>>(
            Unzip(
                input = binaryInput(MULTI_ZIP),
                limits = UnzipLimits(maxFiles = 1),
            ).unzip(),
        )
        assertIs<GMResult.Err<UnzipError.FileNameLimitExceeded>>(
            Unzip(
                input = binaryInput(FIXED_ZIP),
                limits = UnzipLimits(maxFileNameBytes = 5),
            ).unzip(),
        )
        assertIs<GMResult.Err<UnzipError.DeflateBlockLimitExceeded>>(
            Unzip(
                input = binaryInput(STORED_ZIP),
                limits = UnzipLimits(maxDeflateBlocks = 0),
            ).unzip(),
        )
    }

    @Test
    fun malformedArchiveFailuresStayAtTheDecoderBoundary() {
        val unsupportedMethod = binaryInput(STORED_ZIP).toMutableList()
        unsupportedMethod[8] = 12
        assertEquals(
            UnzipError.UnsupportedZipCompressionMethod(12),
            assertIs<GMResult.Err<UnzipError>>(
                Unzip(unsupportedMethod).unzip(),
            ).error,
        )

        val invalidStoredLength = binaryInput(STORED_ZIP).toMutableList()
        invalidStoredLength[43] = 0
        assertIs<GMResult.Err<UnzipError.InvalidStoredBlockLength>>(
            Unzip(invalidStoredLength).unzip(),
        )

        assertIs<GMResult.Err<UnzipError.UnexpectedEnd>>(
            Unzip(binaryInput(ZLIB).dropLast(12)).unzip(),
        )
    }

    @Test
    fun compressorFailuresAreStructured() {
        assertIs<GMResult.Err<DecompressError.Base64Decoding>>(
            Compressor.decompress("abc"),
        )
        assertIs<GMResult.Err<DecompressError.MissingArchiveEntry>>(
            Compressor.decompress("UEsFBg=="),
        )
        assertIs<GMResult.Err<DecompressError.InvalidUriEncoding>>(
            Compressor.decompress("eNpTdTVSNbIAAAQQASw="),
        )
        assertIs<GMResult.Err<DecompressError.InvalidUriEncoding>>(
            Compressor.decompress("eNpTjYoCAAGAANo="),
        )
    }

    private fun unzip(base64: String): List<UnzippedFile> =
        assertIs<GMResult.Ok<List<UnzippedFile>>>(
            Unzip(binaryInput(base64)).unzip(),
        ).value

    private fun binaryInput(base64: String): List<Int> =
        assertIs<GMResult.Ok<List<Int>>>(
            Base64.decodeAsArray(base64),
        ).value

    private fun dynamicContent(): String =
        List(7) { index ->
            "n${index % 17}:" +
                "abcde".repeat((index % 5) + 1) +
                ":${(index * 7919) % 104729};"
        }.joinToString("|")

    companion object {
        private const val STORED_ZIP =
            "UEsDBBQAAAAIAAAAAAAAAAAAHAAAABcAAAAKAAAAc3RvcmVkLnR4dAEXAOj/" +
                "c3RvcmVkLWJsb2NrOjAxMjM0NTY3ODlQSwUG"
        private const val FIXED_ZIP =
            "UEsDBBQAAAAIAAAAAAAAAAAAGwAAAIwBAAAQAAAAbmVzdGVkL2ZpeGVk" +
                "LnR4dEvLrEhN0U3KyU/OtnJ0CnJ0dnQBUbqjbPqzAVBLBQY="
        private const val DYNAMIC_ZIP =
            "UEsDBBQAAAAIAAAAAAAAAAAATQAAAKEAAAALAAAAZHluYW1pYy50eHRt" +
                "zrsNwDAIBNCVjOHA2NPk13qCDB8kUlhROrh3EszSt/04r17GPSnnDMzJ" +
                "I6tLlkBo3EL4K8mVDRYsv5wdJjWNDt7j7HDErusDYiAZD1BLBQY="
        private const val MULTI_ZIP =
            "UEsDBBQAAAAIAAAAAAAAAAAAGwAAAIwBAAAQAAAAbmVzdGVkL2ZpeGVk" +
                "LnR4dEvLrEhN0U3KyU/OtnJ0CnJ0dnQBUbqjbPqzAVBLAwQUAAAACAAA" +
                "AAAAAAAAAE0AAAChAAAACwAAAGR5bmFtaWMudHh0bc67DcAwCATQlYzhw" +
                "NjT5Nd6ggwfJFJYUTq4dxLM0rf9OK9exj0p5wzMySOrS5ZAaNxC+CvJl" +
                "Q0WLL+cHSY1jQ7e4+xwxK7rA2IgGQ9QSwUG"
        private const val GZIP =
            "H4sIAAAAAAACE0uvyiywioiM0h3KGACzend1xQAAAA=="
        private const val GZIP_WITH_HEADERS =
            "H4sIGgAAAAAAA3NvdXJjZS5neHQAZml4dHVyZS1jb21tZW50AAAA" +
                "S6/KLNDNSE1MSS2yCtSlDAIAAAAAAEwAAAA="
        private const val DESCRIPTOR_ZIP =
            "UEsDBBQACAAIAAAAAAAAAAAAAAAAAAAAAAAOAAAAZGVzY3JpcHRv" +
                "ci50eHRLy6xITdFNyslPzrZydApydHZ0AVG6o2z6swFQSwcIAAAA" +
                "ABsAAACMAQAAUEsFBg=="
        private const val ZLIB =
            "eNrzinDXSy5KTSxJ1VAvyM/MK1FXNXJWNTJQNXUyhLCMVE1dNFWNnYBM" +
                "9yJVZ2NVJ2cQaemWClLmZgBkqVpaqFoYAADm5xHg"
    }
}
