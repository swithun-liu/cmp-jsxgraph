/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/utils/uuid.js
 * Copyright 2010 Robert Kieffer.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.math.RandomSource
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.random.Random

private val defaultUuidRandomSource = RandomSource { Random.nextDouble() }

// JSXGraph 1.13.3: src/utils/uuid.js -> JXG.Util.genUUID.
fun genUUID(
    prefix: String = "",
    randomSource: RandomSource = defaultUuidRandomSource,
): String {
    val normalizedPrefix = when {
        prefix.isEmpty() || prefix.endsWith("-") -> prefix
        else -> "$prefix-"
    }
    var randomPool = 0

    return normalizedPrefix + buildString(UUID_LENGTH) {
        for (index in 0 until UUID_LENGTH) {
            when (index) {
                8, 13, 18, 23 -> append('-')
                14 -> append('4')
                else -> {
                    if (randomPool <= 0x02) {
                        randomPool = jsToInt32(
                            0x2000000.toDouble() +
                                randomSource.nextDouble() * 0x1000000,
                        )
                    }

                    val randomNibble = randomPool and 0xF
                    randomPool = randomPool shr 4
                    val characterIndex = if (index == 19) {
                        (randomNibble and 0x3) or 0x8
                    } else {
                        randomNibble
                    }
                    append(UUID_CHARACTERS[characterIndex])
                }
            }
        }
    }
}

private fun jsToInt32(value: Double): Int {
    if (!value.isFinite() || value == 0.0) {
        return 0
    }
    val integer = if (value > 0.0) floor(value) else ceil(value)
    val modulo = integer % UINT32_MODULUS
    val unsigned = if (modulo < 0.0) modulo + UINT32_MODULUS else modulo
    return if (unsigned >= INT32_LIMIT) {
        (unsigned - UINT32_MODULUS).toInt()
    } else {
        unsigned.toInt()
    }
}

private const val UUID_CHARACTERS =
    "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
private const val UUID_LENGTH = 36
private const val UINT32_MODULUS = 4294967296.0
private const val INT32_LIMIT = 2147483648.0
