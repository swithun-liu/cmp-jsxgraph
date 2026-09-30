/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.utils.UnzipError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ReaderPreparationTest {
    @Test
    fun geonextRawAndArchivePreparationMatchOfficialFixture() {
        val expected =
            "<GEONEXT><content>A &amp; B &lt;arc&gt;literal</content>" +
                "<point><id>P</id></point></GEONEXT>"

        assertEquals(
            expected,
            prepare(
                ReaderPreparation.prepareGeonext(RAW_GEONEXT),
            ),
        )
        assertEquals(
            expected,
            prepare(
                ReaderPreparation.prepareGeonext(GEONEXT_ARCHIVE),
            ),
        )
    }

    @Test
    fun intergeoRawBase64AndArchivePreparationMatchOfficialFixture() {
        val expected = "<construction><elements/></construction>"

        assertEquals(
            expected,
            prepare(ReaderPreparation.prepareIntergeo(expected)),
        )
        assertEquals(
            expected,
            prepare(
                ReaderPreparation.prepareIntergeo(
                    "PGNvbnN0cnVjdGlvbj48ZWxlbWVudHMvPjwvY29uc3RydWN0aW9uPg==",
                ),
            ),
        )
        assertEquals(
            expected,
            prepare(
                ReaderPreparation.prepareIntergeo(INTERGEO_ARCHIVE),
            ),
        )
    }

    @Test
    fun geogebraArchiveAndSymbolReplacementMatchOfficialFixture() {
        assertEquals(
            "PI^2^3==!=<=>=&&//",
            ReaderPreparation.geogebraUtf8Replace(
                "\u03C0\u00B2\u00B3\u225F\u2260" +
                    "\u2264\u2265\u2227\u2228",
            ),
        )
        assertEquals(
            "<geogebra>PI^2//<=</geogebra>",
            prepare(
                ReaderPreparation.prepareGeogebra(GEOGEBRA_ARCHIVE),
            ),
        )
        assertEquals(
            "<geogebra>PI^2//<=</geogebra>",
            prepare(
                ReaderPreparation.prepareGeogebra(
                    "<geogebra>\u03C0\u00B2\u2228\u2264</geogebra>",
                ),
            ),
        )
    }

    @Test
    fun cinderellaRawAndArchivePreparationMatchOfficialFixture() {
        assertEquals(
            "<cindyscript/>",
            prepare(
                ReaderPreparation.prepareCinderella("<cindyscript/>"),
            ),
        )
        assertEquals(
            "Cindy fixture",
            prepare(
                ReaderPreparation.prepareCinderella(
                    source = CINDERELLA_ARCHIVE,
                    isString = true,
                ),
            ),
        )
    }

    @Test
    fun preparationFailuresAndLimitsAreStructured() {
        assertIs<
            GMResult.Err<ReaderPreparationError.Base64DecodingFailed>
        >(
            ReaderPreparation.prepareGeonext("abc"),
        )
        val archiveError = assertIs<
            GMResult.Err<ReaderPreparationError.ArchiveDecodingFailed>
        >(
            ReaderPreparation.prepareIntergeo("AQIDBA=="),
        ).error
        assertIs<UnzipError.UnsupportedContainer>(archiveError.cause)

        assertIs<
            GMResult.Err<ReaderPreparationError.SourceLimitExceeded>
        >(
            ReaderPreparation.prepareCinderella(
                source = "<cindyscript/>",
                limits = ReaderPreparationLimits(
                    maxSourceCharacters = 1,
                ),
            ),
        )
        assertIs<
            GMResult.Err<ReaderPreparationError.PreparedLimitExceeded>
        >(
            ReaderPreparation.prepareGeonext(
                source = RAW_GEONEXT,
                limits = ReaderPreparationLimits(
                    maxPreparedCharacters = 1,
                ),
            ),
        )
        assertIs<GMResult.Err<ReaderPreparationError.InvalidLimits>>(
            ReaderPreparation.prepareGeogebra(
                source = "<geogebra/>",
                limits = ReaderPreparationLimits(
                    maxPreparedCharacters = -1,
                ),
            ),
        )
    }

    private fun prepare(
        result: GMResult<String, ReaderPreparationError>,
    ): String = assertIs<GMResult.Ok<String>>(result).value

    private companion object {
        const val RAW_GEONEXT =
            "<GEONEXT><content>A & B <arc>literal</content>" +
                "<point><id>P</id></point></GEONEXT>"
        const val GEONEXT_ARCHIVE =
            "UEsDBBQAAAAIAAAAAAAAAAAAPgAAAFEAAAAKAAAAc291cmNlLmd4dLNx" +
                "d/X3c40IsbNJzs8rSc0rsXNUUFNwUrBJLEq2y8ksSS1KzLHRh8nZ" +
                "FORngqjMFLsAG30gaaMPFdGHmQMAUEsFBg=="
        const val INTERGEO_ARCHIVE =
            "UEsDBBQAAAAIAAAAAAAAAAAAHwAAACgAAAAZAAAAY29uc3RydWN0aW9u" +
                "L2ludGVyZ2VvLnhtbLNJzs8rLikqTS7JzM+zs0nNSc1NzSsp1rez0U" +
                "eRAQBQSwUG"
        const val GEOGEBRA_ARCHIVE =
            "UEsDBBQAAAAIAAAAAAAAAAAAGgAAAB8AAAAMAAAAZ2VvZ2VicmEueG1s" +
                "s0lPzU9PTSpKtDvfcGjTo44VjzqX2OjDBQFQSwUG"
        const val CINDERELLA_ARCHIVE =
            "UEsDBBQAAAAIAAAAAAAAAAAADwAAAA0AAAAQAAAAY29uc3RydWN0aW9u" +
                "LmNkeXPOzEupVEjLrCgpLUoFAFBLBQY="
    }
}
