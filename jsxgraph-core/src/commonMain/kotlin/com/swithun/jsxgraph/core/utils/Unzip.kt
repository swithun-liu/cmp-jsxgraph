/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/utils/zip.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult

data class UnzipLimits(
    val maxInputBytes: Int = 16 * 1024 * 1024,
    val maxOutputBytes: Int = 64 * 1024 * 1024,
    val maxFiles: Int = 256,
    val maxFileNameBytes: Int = 4096,
    val maxDeflateBlocks: Int = 1_000_000,
)

data class UnzippedFile(
    val content: String,
    val name: String,
)

sealed interface UnzipError {
    data class InputLimitExceeded(
        val limit: Int,
        val actual: Int,
    ) : UnzipError

    data class InvalidInputByte(
        val index: Int,
        val value: Int,
    ) : UnzipError

    data class UnexpectedEnd(
        val offset: Int,
        val context: String,
    ) : UnzipError

    data class UnsupportedContainer(
        val firstByte: Int?,
        val secondByte: Int?,
    ) : UnzipError

    data class UnsupportedZipCompressionMethod(
        val method: Int,
    ) : UnzipError

    data object EncryptedZipEntry : UnzipError

    data class FileNameLimitExceeded(
        val limit: Int,
        val actual: Int,
    ) : UnzipError

    data class FileCountLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : UnzipError

    data class OutputLimitExceeded(
        val limit: Int,
        val requested: Long,
    ) : UnzipError

    data class InvalidCompressedSize(
        val declared: Long,
        val consumed: Long,
    ) : UnzipError

    data class InvalidGzipCompressionMethod(
        val method: Int,
    ) : UnzipError

    data class InvalidDeflateBlockType(
        val type: Int,
    ) : UnzipError

    data class DeflateBlockLimitExceeded(
        val limit: Int,
    ) : UnzipError

    data class InvalidStoredBlockLength(
        val length: Int,
        val complement: Int,
    ) : UnzipError

    data class InvalidHuffmanTree(
        val tree: String,
        val reason: String,
    ) : UnzipError

    data class InvalidHuffmanSymbol(
        val tree: String,
        val symbol: Int,
    ) : UnzipError

    data class InvalidBackReference(
        val distance: Int,
        val outputSize: Int,
    ) : UnzipError
}

/**
 * Pure commonMain decoder for the containers accepted by
 * `JXG.Util.Unzip`.
 */
class Unzip(
    private val input: List<Int>,
    private val limits: UnzipLimits = UnzipLimits(),
) {
    // JSXGraph 1.13.3: src/utils/zip.js ->
    // JXG.Util.Unzip.prototype.unzip.
    fun unzip(): GMResult<List<UnzippedFile>, UnzipError> {
        if (input.size > limits.maxInputBytes) {
            return GMResult.Err(
                UnzipError.InputLimitExceeded(
                    limit = limits.maxInputBytes,
                    actual = input.size,
                ),
            )
        }
        val bytes = IntArray(input.size)
        for (index in input.indices) {
            val value = input[index]
            if (value !in 0..0xFF) {
                return GMResult.Err(
                    UnzipError.InvalidInputByte(index, value),
                )
            }
            bytes[index] = value
        }
        return Decoder(bytes, limits).unzip()
    }

    // JSXGraph 1.13.3: src/utils/zip.js ->
    // JXG.Util.Unzip.prototype.unzipFile.
    fun unzipFile(name: String): GMResult<String, UnzipError> =
        when (val result = unzip()) {
            is GMResult.Ok ->
                GMResult.Ok(
                    result.value
                        .firstOrNull { file -> file.name == name }
                        ?.content
                        .orEmpty(),
                )
            is GMResult.Err -> result
        }

    private class Decoder(
        bytes: IntArray,
        private val limits: UnzipLimits,
    ) {
        private val cursor = ByteCursor(bytes)
        private val files = mutableListOf<UnzippedFile>()
        private var outputBytes = 0L

        fun unzip(): GMResult<List<UnzippedFile>, UnzipError> {
            val first = when (
                val result = cursor.readByte("container signature")
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val second = when (
                val result = cursor.readByte("container signature")
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }

            return when {
                first == ZLIB_FIRST && second == ZLIB_SECOND -> {
                    when (
                        val result = inflate(
                            endExclusive = cursor.size,
                        )
                    ) {
                        is GMResult.Ok ->
                            addFile(result.value, ZLIB_FILE_NAME)
                        is GMResult.Err -> result
                    }
                }

                first == GZIP_FIRST && second == GZIP_SECOND ->
                    readGzip()

                first == ZIP_FIRST && second == ZIP_SECOND ->
                    readZip()

                else -> GMResult.Err(
                    UnzipError.UnsupportedContainer(first, second),
                )
            }.let { result ->
                when (result) {
                    is GMResult.Ok -> GMResult.Ok(files.toList())
                    is GMResult.Err -> result
                }
            }
        }

        /*
         * JSXGraph 1.13.3: src/utils/zip.js -> nextFile, ZIP branch.
         * The upstream name buffer keeps at most NAMEMAX - 1 bytes.
         */
        private fun readZip(): GMResult<Unit, UnzipError> {
            var signature = when (
                val result = cursor.readLittleEndian16(
                    "ZIP local header signature",
                )
            ) {
                is GMResult.Ok ->
                    ZIP_FIRST or
                        (ZIP_SECOND shl 8) or
                        (result.value shl 16)
                is GMResult.Err -> return result
            }

            while (signature == ZIP_LOCAL_FILE_SIGNATURE) {
                when (val result = cursor.skip(2, "ZIP version")) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
                val flags = when (
                    val result = cursor.readLittleEndian16("ZIP flags")
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                if (flags and ZIP_ENCRYPTED_FLAG != 0) {
                    return GMResult.Err(UnzipError.EncryptedZipEntry)
                }
                val method = when (
                    val result = cursor.readLittleEndian16(
                        "ZIP compression method",
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                when (val result = cursor.skip(4, "ZIP timestamp")) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
                when (val result = cursor.skip(4, "ZIP CRC32")) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
                val compressedSize = when (
                    val result = cursor.readLittleEndian32(
                        "ZIP compressed size",
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val uncompressedSize = when (
                    val result = cursor.readLittleEndian32(
                        "ZIP uncompressed size",
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val fileNameLength = when (
                    val result = cursor.readLittleEndian16(
                        "ZIP file name length",
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val extraLength = when (
                    val result = cursor.readLittleEndian16(
                        "ZIP extra length",
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                if (fileNameLength > limits.maxFileNameBytes) {
                    return GMResult.Err(
                        UnzipError.FileNameLimitExceeded(
                            limit = limits.maxFileNameBytes,
                            actual = fileNameLength,
                        ),
                    )
                }
                val nameBytes = when (
                    val result = cursor.readBytes(
                        fileNameLength,
                        "ZIP file name",
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val name = binaryString(
                    nameBytes.take(UPSTREAM_NAME_MAX - 1),
                )
                when (
                    val result = cursor.skip(extraLength, "ZIP extra field")
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }

                val compressedStart = cursor.position
                when (method) {
                    ZIP_DEFLATE_METHOD -> {
                        if (
                            flags and ZIP_DATA_DESCRIPTOR_FLAG == 0 &&
                            uncompressedSize >
                            limits.maxOutputBytes.toLong() - outputBytes
                        ) {
                            return GMResult.Err(
                                UnzipError.OutputLimitExceeded(
                                    limit = limits.maxOutputBytes,
                                    requested =
                                        outputBytes + uncompressedSize,
                                ),
                            )
                        }
                        val endExclusive =
                            if (
                                flags and ZIP_DATA_DESCRIPTOR_FLAG == 0
                            ) {
                                val end =
                                    compressedStart.toLong() +
                                        compressedSize
                                if (end > cursor.size) {
                                    return GMResult.Err(
                                        UnzipError.UnexpectedEnd(
                                            offset = cursor.position,
                                            context =
                                                "ZIP compressed payload",
                                        ),
                                    )
                                }
                                end.toInt()
                            } else {
                                cursor.size
                            }
                        val content = when (
                            val result = inflate(endExclusive)
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return result
                        }
                        if (
                            flags and ZIP_DATA_DESCRIPTOR_FLAG == 0
                        ) {
                            val consumed =
                                (cursor.position - compressedStart).toLong()
                            if (consumed != compressedSize) {
                                return GMResult.Err(
                                    UnzipError.InvalidCompressedSize(
                                        declared = compressedSize,
                                        consumed = consumed,
                                    ),
                                )
                            }
                        }
                        when (val result = addFile(content, name)) {
                            is GMResult.Ok -> Unit
                            is GMResult.Err -> return result
                        }
                    }

                    ZIP_STORED_METHOD -> {
                        /*
                         * Upstream only emits method 8 entries. Empty stored
                         * directory records are skipped so later local
                         * headers remain reachable.
                         */
                        if (compressedSize != 0L) {
                            return GMResult.Err(
                                UnzipError
                                    .UnsupportedZipCompressionMethod(method),
                            )
                        }
                    }

                    else -> return GMResult.Err(
                        UnzipError.UnsupportedZipCompressionMethod(method),
                    )
                }

                if (flags and ZIP_DATA_DESCRIPTOR_FLAG != 0) {
                    when (val result = readDataDescriptor()) {
                        is GMResult.Ok -> Unit
                        is GMResult.Err -> return result
                    }
                }

                signature = when (
                    val result = cursor.readLittleEndian32(
                        "ZIP next record signature",
                    )
                ) {
                    is GMResult.Ok -> result.value.toInt()
                    is GMResult.Err -> return result
                }
            }
            return GMResult.Ok(Unit)
        }

        /*
         * JSXGraph 1.13.3: src/utils/zip.js -> skipdir,
         * GZIP branch.
         */
        private fun readGzip(): GMResult<Unit, UnzipError> {
            val method = when (
                val result = cursor.readByte("GZIP compression method")
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (method != ZIP_DEFLATE_METHOD) {
                return GMResult.Err(
                    UnzipError.InvalidGzipCompressionMethod(method),
                )
            }
            val flags = when (
                val result = cursor.readByte("GZIP flags")
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            when (val result = cursor.skip(6, "GZIP fixed header")) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            if (flags and GZIP_EXTRA_FLAG != 0) {
                val length = when (
                    val result = cursor.readLittleEndian16(
                        "GZIP extra length",
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                when (val result = cursor.skip(length, "GZIP extra field")) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            if (flags and GZIP_NAME_FLAG != 0) {
                when (
                    val result = cursor.skipZeroTerminated(
                        limit = limits.maxFileNameBytes,
                        context = "GZIP file name",
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            if (flags and GZIP_COMMENT_FLAG != 0) {
                when (
                    val result = cursor.skipZeroTerminated(
                        limit = limits.maxFileNameBytes,
                        context = "GZIP comment",
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            if (flags and GZIP_HEADER_CRC_FLAG != 0) {
                when (val result = cursor.skip(2, "GZIP header CRC")) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            if (cursor.remaining < GZIP_TRAILER_SIZE) {
                return GMResult.Err(
                    UnzipError.UnexpectedEnd(
                        offset = cursor.position,
                        context = "GZIP payload and trailer",
                    ),
                )
            }
            val content = when (
                val result = inflate(cursor.size - GZIP_TRAILER_SIZE)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            when (val result = cursor.skip(GZIP_TRAILER_SIZE, "GZIP trailer")) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            return addFile(content, GZIP_FILE_NAME)
        }

        private fun readDataDescriptor(): GMResult<Unit, UnzipError> {
            val first = when (
                val result = cursor.readLittleEndian32(
                    "ZIP data descriptor",
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val remaining =
                if (first.toInt() == ZIP_DATA_DESCRIPTOR_SIGNATURE) 12
                else 8
            return cursor.skip(remaining, "ZIP data descriptor")
        }

        // JSXGraph 1.13.3: src/utils/zip.js -> deflateLoop.
        private fun inflate(
            endExclusive: Int,
        ): GMResult<List<Int>, UnzipError> {
            val remainingLimit =
                (limits.maxOutputBytes.toLong() - outputBytes)
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt()
            val inflater = DeflateDecoder(
                cursor = cursor,
                endExclusive = endExclusive,
                maxOutputBytes = remainingLimit,
                maxBlocks = limits.maxDeflateBlocks,
                absoluteOutputLimit = limits.maxOutputBytes,
                existingOutputBytes = outputBytes,
            )
            return inflater.inflate()
        }

        private fun addFile(
            content: List<Int>,
            name: String,
        ): GMResult<Unit, UnzipError> {
            if (files.size >= limits.maxFiles) {
                return GMResult.Err(
                    UnzipError.FileCountLimitExceeded(
                        limit = limits.maxFiles,
                        requested = files.size + 1,
                    ),
                )
            }
            val requested = outputBytes + content.size
            if (requested > limits.maxOutputBytes) {
                return GMResult.Err(
                    UnzipError.OutputLimitExceeded(
                        limit = limits.maxOutputBytes,
                        requested = requested,
                    ),
                )
            }
            files += UnzippedFile(
                content = binaryString(content),
                name = name,
            )
            outputBytes = requested
            return GMResult.Ok(Unit)
        }
    }

    private class DeflateDecoder(
        cursor: ByteCursor,
        endExclusive: Int,
        private val maxOutputBytes: Int,
        private val maxBlocks: Int,
        private val absoluteOutputLimit: Int,
        private val existingOutputBytes: Long,
    ) {
        private val bits = BitReader(cursor, endExclusive)
        private val output = mutableListOf<Int>()

        fun inflate(): GMResult<List<Int>, UnzipError> {
            var isLast = false
            var blockCount = 0
            while (!isLast) {
                blockCount += 1
                if (blockCount > maxBlocks) {
                    return GMResult.Err(
                        UnzipError.DeflateBlockLimitExceeded(maxBlocks),
                    )
                }
                isLast = when (val result = bits.readBits(1)) {
                    is GMResult.Ok -> result.value == 1
                    is GMResult.Err -> return result
                }
                val type = when (val result = bits.readBits(2)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                when (
                    val result = when (type) {
                        0 -> readStoredBlock()
                        1 -> readCompressedBlock(
                            literalTree = fixedLiteralTree(),
                            distanceTree = fixedDistanceTree(),
                        )
                        2 -> readDynamicBlock()
                        else -> GMResult.Err(
                            UnzipError.InvalidDeflateBlockType(type),
                        )
                    }
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            bits.alignToByte()
            return GMResult.Ok(output.toList())
        }

        private fun readStoredBlock(): GMResult<Unit, UnzipError> {
            bits.alignToByte()
            val length = when (val result = bits.readLittleEndian16()) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val complement = when (
                val result = bits.readLittleEndian16()
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if ((length xor complement) != 0xFFFF) {
                return GMResult.Err(
                    UnzipError.InvalidStoredBlockLength(
                        length = length,
                        complement = complement,
                    ),
                )
            }
            repeat(length) {
                val byte = when (val result = bits.readByte()) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                when (val result = append(byte)) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            return GMResult.Ok(Unit)
        }

        private fun readDynamicBlock(): GMResult<Unit, UnzipError> {
            val literalCodeCount =
                257 + when (val result = bits.readBits(5)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            val distanceCodeCount =
                1 + when (val result = bits.readBits(5)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            val codeLengthCount =
                4 + when (val result = bits.readBits(4)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
            val codeLengths = IntArray(19)
            for (index in 0 until codeLengthCount) {
                codeLengths[CODE_LENGTH_ORDER[index]] =
                    when (val result = bits.readBits(3)) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return result
                    }
            }
            val codeLengthTree = when (
                val result = HuffmanTree.create(
                    name = "code-length",
                    lengths = codeLengths,
                    maximumBits = 7,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val combined = IntArray(
                literalCodeCount + distanceCodeCount,
            )
            var index = 0
            while (index < combined.size) {
                val symbol = when (
                    val result = codeLengthTree.decode(bits)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                when (symbol) {
                    in 0..15 -> {
                        combined[index] = symbol
                        index += 1
                    }

                    16 -> {
                        val repeatCount =
                            3 + when (val result = bits.readBits(2)) {
                                is GMResult.Ok -> result.value
                                is GMResult.Err -> return result
                            }
                        if (index + repeatCount > combined.size) {
                            return GMResult.Err(
                                UnzipError.InvalidHuffmanTree(
                                    tree = "dynamic",
                                    reason =
                                        "Repeated code length exceeds " +
                                            "the declared symbol count.",
                                ),
                            )
                        }
                        val previous = if (index == 0) 0 else combined[index - 1]
                        repeat(repeatCount) {
                            combined[index] = previous
                            index += 1
                        }
                    }

                    17, 18 -> {
                        val extraBits = if (symbol == 17) 3 else 7
                        val base = if (symbol == 17) 3 else 11
                        val repeatCount =
                            base + when (
                                val result = bits.readBits(extraBits)
                            ) {
                                is GMResult.Ok -> result.value
                                is GMResult.Err -> return result
                            }
                        if (index + repeatCount > combined.size) {
                            return GMResult.Err(
                                UnzipError.InvalidHuffmanTree(
                                    tree = "dynamic",
                                    reason =
                                        "Repeated zero length exceeds " +
                                            "the declared symbol count.",
                                ),
                            )
                        }
                        index += repeatCount
                    }

                    else -> return GMResult.Err(
                        UnzipError.InvalidHuffmanSymbol(
                            tree = "code-length",
                            symbol = symbol,
                        ),
                    )
                }
            }
            val literalTree = when (
                val result = HuffmanTree.create(
                    name = "literal/length",
                    lengths = combined.copyOfRange(0, literalCodeCount),
                    maximumBits = 15,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val distanceTree = when (
                val result = HuffmanTree.create(
                    name = "distance",
                    lengths = combined.copyOfRange(
                        literalCodeCount,
                        combined.size,
                    ),
                    maximumBits = 15,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return readCompressedBlock(literalTree, distanceTree)
        }

        private fun readCompressedBlock(
            literalTree: HuffmanTree,
            distanceTree: HuffmanTree,
        ): GMResult<Unit, UnzipError> {
            while (true) {
                val symbol = when (
                    val result = literalTree.decode(bits)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                when (symbol) {
                    in 0..255 -> when (val result = append(symbol)) {
                        is GMResult.Ok -> Unit
                        is GMResult.Err -> return result
                    }

                    END_OF_BLOCK -> return GMResult.Ok(Unit)

                    in 257..285 -> {
                        val lengthIndex = symbol - 257
                        val length =
                            LENGTH_BASE[lengthIndex] +
                                when (
                                    val result = bits.readBits(
                                        LENGTH_EXTRA[lengthIndex],
                                    )
                                ) {
                                    is GMResult.Ok -> result.value
                                    is GMResult.Err -> return result
                                }
                        val distanceSymbol = when (
                            val result = distanceTree.decode(bits)
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return result
                        }
                        if (distanceSymbol !in DISTANCE_BASE.indices) {
                            return GMResult.Err(
                                UnzipError.InvalidHuffmanSymbol(
                                    tree = "distance",
                                    symbol = distanceSymbol,
                                ),
                            )
                        }
                        val distance =
                            DISTANCE_BASE[distanceSymbol] +
                                when (
                                    val result = bits.readBits(
                                        DISTANCE_EXTRA[distanceSymbol],
                                    )
                                ) {
                                    is GMResult.Ok -> result.value
                                    is GMResult.Err -> return result
                                }
                        if (distance <= 0 || distance > output.size) {
                            return GMResult.Err(
                                UnzipError.InvalidBackReference(
                                    distance = distance,
                                    outputSize = output.size,
                                ),
                            )
                        }
                        repeat(length) {
                            val value = output[output.size - distance]
                            when (val result = append(value)) {
                                is GMResult.Ok -> Unit
                                is GMResult.Err -> return result
                            }
                        }
                    }

                    else -> return GMResult.Err(
                        UnzipError.InvalidHuffmanSymbol(
                            tree = "literal/length",
                            symbol = symbol,
                        ),
                    )
                }
            }
        }

        private fun append(value: Int): GMResult<Unit, UnzipError> {
            if (output.size >= maxOutputBytes) {
                return GMResult.Err(
                    UnzipError.OutputLimitExceeded(
                        limit = absoluteOutputLimit,
                        requested = existingOutputBytes + output.size + 1L,
                    ),
                )
            }
            output += value
            return GMResult.Ok(Unit)
        }

        /*
         * The upstream fixed branch decodes its canonical table with
         * bitReverse. The precomputed lengths below describe the same
         * RFC 1951 table and share the dynamic-tree decoder.
         */
        private fun fixedLiteralTree(): HuffmanTree =
            FIXED_LITERAL_TREE

        private fun fixedDistanceTree(): HuffmanTree =
            FIXED_DISTANCE_TREE
    }

    private class HuffmanTree private constructor(
        private val name: String,
        private val maximumLength: Int,
        private val symbolsByCode: Map<Int, Int>,
    ) {
        fun decode(
            bits: BitReader,
        ): GMResult<Int, UnzipError> {
            var code = 0
            for (length in 1..maximumLength) {
                val bit = when (val result = bits.readBits(1)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                code = code or (bit shl (length - 1))
                val symbol = symbolsByCode[key(length, code)]
                if (symbol != null) {
                    return GMResult.Ok(symbol)
                }
            }
            return GMResult.Err(
                UnzipError.InvalidHuffmanTree(
                    tree = name,
                    reason = "No symbol matches the input prefix.",
                ),
            )
        }

        companion object {
            fun create(
                name: String,
                lengths: IntArray,
                maximumBits: Int,
            ): GMResult<HuffmanTree, UnzipError> {
                val counts = IntArray(maximumBits + 1)
                var maximumLength = 0
                for (length in lengths) {
                    if (length !in 0..maximumBits) {
                        return GMResult.Err(
                            UnzipError.InvalidHuffmanTree(
                                tree = name,
                                reason = "Code length $length is invalid.",
                            ),
                        )
                    }
                    if (length > 0) {
                        counts[length] += 1
                        maximumLength = maxOf(maximumLength, length)
                    }
                }
                if (maximumLength == 0) {
                    return GMResult.Err(
                        UnzipError.InvalidHuffmanTree(
                            tree = name,
                            reason = "The tree contains no symbols.",
                        ),
                    )
                }
                var available = 1
                for (length in 1..maximumBits) {
                    available = (available shl 1) - counts[length]
                    if (available < 0) {
                        return GMResult.Err(
                            UnzipError.InvalidHuffmanTree(
                                tree = name,
                                reason = "The tree is oversubscribed.",
                            ),
                        )
                    }
                }

                val nextCode = IntArray(maximumBits + 1)
                var code = 0
                for (length in 1..maximumBits) {
                    code = (code + counts[length - 1]) shl 1
                    nextCode[length] = code
                }
                val symbols = mutableMapOf<Int, Int>()
                for (symbol in lengths.indices) {
                    val length = lengths[symbol]
                    if (length == 0) {
                        continue
                    }
                    val canonicalCode = nextCode[length]
                    nextCode[length] += 1
                    symbols[key(length, reverseBits(canonicalCode, length))] =
                        symbol
                }
                return GMResult.Ok(
                    HuffmanTree(name, maximumLength, symbols),
                )
            }

            private fun reverseBits(
                value: Int,
                length: Int,
            ): Int {
                var source = value
                var reversed = 0
                repeat(length) {
                    reversed = (reversed shl 1) or (source and 1)
                    source = source ushr 1
                }
                return reversed
            }

            private fun key(
                length: Int,
                code: Int,
            ): Int = (length shl 16) or code
        }
    }

    private class BitReader(
        private val cursor: ByteCursor,
        private val endExclusive: Int,
    ) {
        private var current = 0
        private var remainingBits = 0

        // JSXGraph 1.13.3: src/utils/zip.js -> readBit, readBits.
        fun readBits(count: Int): GMResult<Int, UnzipError> {
            var result = 0
            repeat(count) { index ->
                if (remainingBits == 0) {
                    if (cursor.position >= endExclusive) {
                        return GMResult.Err(
                            UnzipError.UnexpectedEnd(
                                offset = cursor.position,
                                context = "DEFLATE bit stream",
                            ),
                        )
                    }
                    current = when (
                        val byte = cursor.readByte("DEFLATE bit stream")
                    ) {
                        is GMResult.Ok -> byte.value
                        is GMResult.Err -> return byte
                    }
                    remainingBits = 8
                }
                result = result or ((current and 1) shl index)
                current = current ushr 1
                remainingBits -= 1
            }
            return GMResult.Ok(result)
        }

        fun alignToByte() {
            current = 0
            remainingBits = 0
        }

        fun readByte(): GMResult<Int, UnzipError> {
            if (remainingBits != 0) {
                return GMResult.Err(
                    UnzipError.InvalidHuffmanTree(
                        tree = "stored",
                        reason = "Stored byte read is not byte-aligned.",
                    ),
                )
            }
            if (cursor.position >= endExclusive) {
                return GMResult.Err(
                    UnzipError.UnexpectedEnd(
                        offset = cursor.position,
                        context = "DEFLATE stored block",
                    ),
                )
            }
            return cursor.readByte("DEFLATE stored block")
        }

        fun readLittleEndian16(): GMResult<Int, UnzipError> {
            val low = when (val result = readByte()) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val high = when (val result = readByte()) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return GMResult.Ok(low or (high shl 8))
        }
    }

    private class ByteCursor(
        private val bytes: IntArray,
    ) {
        var position: Int = 0
            private set

        val size: Int
            get() = bytes.size

        val remaining: Int
            get() = size - position

        fun readByte(
            context: String,
        ): GMResult<Int, UnzipError> {
            if (position >= bytes.size) {
                return GMResult.Err(
                    UnzipError.UnexpectedEnd(position, context),
                )
            }
            return GMResult.Ok(bytes[position++])
        }

        fun readLittleEndian16(
            context: String,
        ): GMResult<Int, UnzipError> {
            val low = when (val result = readByte(context)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val high = when (val result = readByte(context)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            return GMResult.Ok(low or (high shl 8))
        }

        fun readLittleEndian32(
            context: String,
        ): GMResult<Long, UnzipError> {
            var result = 0L
            repeat(4) { index ->
                val byte = when (val value = readByte(context)) {
                    is GMResult.Ok -> value.value
                    is GMResult.Err -> return value
                }
                result = result or (byte.toLong() shl (index * 8))
            }
            return GMResult.Ok(result and 0xFFFF_FFFFL)
        }

        fun readBytes(
            count: Int,
            context: String,
        ): GMResult<List<Int>, UnzipError> {
            if (count < 0 || count > remaining) {
                return GMResult.Err(
                    UnzipError.UnexpectedEnd(position, context),
                )
            }
            val result = bytes.copyOfRange(position, position + count)
            position += count
            return GMResult.Ok(result.toList())
        }

        fun skip(
            count: Int,
            context: String,
        ): GMResult<Unit, UnzipError> {
            if (count < 0 || count > remaining) {
                return GMResult.Err(
                    UnzipError.UnexpectedEnd(position, context),
                )
            }
            position += count
            return GMResult.Ok(Unit)
        }

        fun skipZeroTerminated(
            limit: Int,
            context: String,
        ): GMResult<Unit, UnzipError> {
            var count = 0
            while (true) {
                val byte = when (val result = readByte(context)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                if (byte == 0) {
                    return GMResult.Ok(Unit)
                }
                count += 1
                if (count > limit) {
                    return GMResult.Err(
                        UnzipError.FileNameLimitExceeded(
                            limit = limit,
                            actual = count,
                        ),
                    )
                }
            }
        }
    }

    companion object {
        private const val ZLIB_FIRST = 0x78
        private const val ZLIB_SECOND = 0xDA
        private const val GZIP_FIRST = 0x1F
        private const val GZIP_SECOND = 0x8B
        private const val ZIP_FIRST = 0x50
        private const val ZIP_SECOND = 0x4B
        private const val ZIP_LOCAL_FILE_SIGNATURE = 0x04034B50
        private const val ZIP_DATA_DESCRIPTOR_SIGNATURE = 0x08074B50
        private const val ZIP_STORED_METHOD = 0
        private const val ZIP_DEFLATE_METHOD = 8
        private const val ZIP_ENCRYPTED_FLAG = 1
        private const val ZIP_DATA_DESCRIPTOR_FLAG = 8
        private const val GZIP_HEADER_CRC_FLAG = 2
        private const val GZIP_EXTRA_FLAG = 4
        private const val GZIP_NAME_FLAG = 8
        private const val GZIP_COMMENT_FLAG = 16
        private const val GZIP_TRAILER_SIZE = 8
        private const val UPSTREAM_NAME_MAX = 256
        private const val ZLIB_FILE_NAME = "geonext.gxt"
        private const val GZIP_FILE_NAME = "file"
        private const val END_OF_BLOCK = 256

        private val LENGTH_BASE = intArrayOf(
            3, 4, 5, 6, 7, 8, 9, 10,
            11, 13, 15, 17,
            19, 23, 27, 31,
            35, 43, 51, 59,
            67, 83, 99, 115,
            131, 163, 195, 227,
            258,
        )
        private val LENGTH_EXTRA = intArrayOf(
            0, 0, 0, 0, 0, 0, 0, 0,
            1, 1, 1, 1,
            2, 2, 2, 2,
            3, 3, 3, 3,
            4, 4, 4, 4,
            5, 5, 5, 5,
            0,
        )
        private val DISTANCE_BASE = intArrayOf(
            1, 2, 3, 4,
            5, 7, 9, 13,
            17, 25, 33, 49,
            65, 97, 129, 193,
            257, 385, 513, 769,
            1025, 1537, 2049, 3073,
            4097, 6145, 8193, 12289,
            16385, 24577,
        )
        private val DISTANCE_EXTRA = intArrayOf(
            0, 0, 0, 0,
            1, 1, 2, 2,
            3, 3, 4, 4,
            5, 5, 6, 6,
            7, 7, 8, 8,
            9, 9, 10, 10,
            11, 11, 12, 12,
            13, 13,
        )
        private val CODE_LENGTH_ORDER = intArrayOf(
            16, 17, 18, 0, 8, 7, 9, 6, 10, 5,
            11, 4, 12, 3, 13, 2, 14, 1, 15,
        )

        private val FIXED_LITERAL_TREE: HuffmanTree = run {
            val lengths = IntArray(288)
            for (symbol in 0..143) lengths[symbol] = 8
            for (symbol in 144..255) lengths[symbol] = 9
            for (symbol in 256..279) lengths[symbol] = 7
            for (symbol in 280..287) lengths[symbol] = 8
            when (
                val result = HuffmanTree.create(
                    name = "fixed literal/length",
                    lengths = lengths,
                    maximumBits = 15,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err ->
                    error("Fixed literal tree construction failed.")
            }
        }

        private val FIXED_DISTANCE_TREE: HuffmanTree = run {
            when (
                val result = HuffmanTree.create(
                    name = "fixed distance",
                    lengths = IntArray(32) { 5 },
                    maximumBits = 15,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err ->
                    error("Fixed distance tree construction failed.")
            }
        }

        private fun binaryString(bytes: List<Int>): String =
            buildString(bytes.size) {
                for (byte in bytes) {
                    append(byte.toChar())
                }
            }
    }
}
