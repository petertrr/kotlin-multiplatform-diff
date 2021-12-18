/*
 * Copyright 2021 Peter Trifanov.
 * Copyright 2019 java-diff-utils.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file has been modified by Peter Trifanov when porting from Java to Kotlin.
 */
package io.github.petertrr.diffutils.unifieddiff

import io.github.petertrr.diffutils.patch.ChangeDelta
import io.github.petertrr.diffutils.patch.Chunk
import io.github.petertrr.diffutils.unifieddiff.internal.UnifiedDiffLine
import okio.IOException
import okio.Source
import okio.buffer

public class UnifiedDiffReader internal constructor(source: Source) {
    private val READER: InternalUnifiedDiffReader = InternalUnifiedDiffReader(source)
    private val data: UnifiedDiff = UnifiedDiff()
    private val DIFF_COMMAND: UnifiedDiffLine = UnifiedDiffLine(true, "^diff\\s") { match, line ->
            processDiff(
                match,
                line
            )
        }
    private val SIMILARITY_INDEX: UnifiedDiffLine = UnifiedDiffLine(true, "^similarity index (\\d+)%$") { match, line ->
            processSimilarityIndex(
                match,
                line
            )
        }
    private val INDEX: UnifiedDiffLine = UnifiedDiffLine(true, "^index\\s[\\da-zA-Z]+\\.\\.[\\da-zA-Z]+(\\s(\\d+))?$") { match, line ->
            processIndex(
                match,
                line
            )
        }
    private val FROM_FILE: UnifiedDiffLine = UnifiedDiffLine(true, "^---\\s") { match, line ->
            processFromFile(
                match,
                line
            )
        }
    private val TO_FILE: UnifiedDiffLine = UnifiedDiffLine(true, "^\\+\\+\\+\\s") { match, line ->
            processToFile(
                match,
                line
            )
        }
    private val RENAME_FROM: UnifiedDiffLine = UnifiedDiffLine(true, "^rename\\sfrom\\s(.+)$") { match, line ->
            processRenameFrom(
                match,
                line
            )
        }
    private val RENAME_TO: UnifiedDiffLine = UnifiedDiffLine(true, "^rename\\sto\\s(.+)$") { match, line ->
            processRenameTo(
                match,
                line
            )
        }
    private val NEW_FILE_MODE: UnifiedDiffLine = UnifiedDiffLine(true, "^new\\sfile\\smode\\s(\\d+)") { match, line ->
            processNewFileMode(
                match,
                line
            )
        }
    private val DELETED_FILE_MODE: UnifiedDiffLine = UnifiedDiffLine(true, "^deleted\\sfile\\smode\\s(\\d+)") { match, line ->
            processDeletedFileMode(
                match,
                line
            )
        }
    private val CHUNK: UnifiedDiffLine = UnifiedDiffLine(false, UNIFIED_DIFF_CHUNK_REGEXP) { match, chunkStart ->
            processChunk(
                match,
                chunkStart
            )
        }
    private val LINE_NORMAL = UnifiedDiffLine(false, "^\\s") { match, line ->
            processNormalLine(
                match,
                line
            )
        }
    private val LINE_DEL = UnifiedDiffLine(false, "^-") { match: MatchResult, line: String ->
            processDelLine(
                match,
                line
            )
        }
    private val LINE_ADD = UnifiedDiffLine(false, "^\\+") { match: MatchResult, line: String ->
            processAddLine(
                match,
                line
            )
        }
    private lateinit var actualFile: UnifiedDiffFile

    // schema = [[/^\s+/, normal], [/^diff\s/, start], [/^new file mode \d+$/, new_file],
    // [/^deleted file mode \d+$/, deleted_file], [/^index\s[\da-zA-Z]+\.\.[\da-zA-Z]+(\s(\d+))?$/, index],
    // [/^---\s/, from_file], [/^\+\+\+\s/, to_file], [/^@@\s+\-(\d+),?(\d+)?\s+\+(\d+),?(\d+)?\s@@/, chunk],
    // [/^-/, del], [/^\+/, add], [/^\\ No newline at end of file$/, eof]];
    @Throws(IOException::class, UnifiedDiffParserException::class)
    private fun parse(): UnifiedDiff {
//        String headerTxt = "";
//        LOG.log(Level.FINE, "header parsing");
//        String line = null;
//        while (READER.ready()) {
//            line = READER.readLine();
//            LOG.log(Level.FINE, "parsing line {0}", line);
//            if (DIFF_COMMAND.validLine(line) || INDEX.validLine(line)
//                    || FROM_FILE.validLine(line) || TO_FILE.validLine(line)
//                    || NEW_FILE_MODE.validLine(line)) {
//                break;
//            } else {
//                headerTxt += line + "\n";
//            }
//        }
//        if (!"".equals(headerTxt)) {
//            data.setHeader(headerTxt);
//        }
        var line: String? = READER.readLine()
        while (line != null) {
            var headerTxt = ""
//            LOG.log(java.util.logging.Level.FINE, "header parsing")
            while (line != null) {
//                LOG.log(java.util.logging.Level.FINE, "parsing line {0}", line)
                headerTxt += if (validLine(
                        line, DIFF_COMMAND, SIMILARITY_INDEX, INDEX,
                        FROM_FILE, TO_FILE,
                        RENAME_FROM, RENAME_TO,
                        NEW_FILE_MODE, DELETED_FILE_MODE,
                        CHUNK
                    )
                ) {
                    break
                } else {
                    """
     $line
     
     """.trimIndent()
                }
                line = READER.readLine()
            }
            if ("" != headerTxt) {
                data.header = headerTxt
            }
            if (line != null && !CHUNK.validLine(line)) {
                initFileIfNecessary()
                while (line != null && !CHUNK.validLine(line)) {
                    if (!processLine(
                            line, DIFF_COMMAND, SIMILARITY_INDEX, INDEX,
                            FROM_FILE, TO_FILE,
                            RENAME_FROM, RENAME_TO,
                            NEW_FILE_MODE, DELETED_FILE_MODE
                        )
                    ) {
                        throw UnifiedDiffParserException("expected file start line not found")
                    }
                    line = READER.readLine()
                }
            }
            if (line != null) {
                processLine(line, CHUNK)
                while (READER.readLine().also { line = it } != null) {
                    line = checkForNoNewLineAtTheEndOfTheFile(line)
                    if (!processLine(line, LINE_NORMAL, LINE_ADD, LINE_DEL)) {
                        throw UnifiedDiffParserException("expected data line not found")
                    }
                    if (originalTxt.size == old_size && revisedTxt.size == new_size
                        || old_size == 0 && new_size == 0 && originalTxt.size == old_ln && revisedTxt.size == new_ln
                    ) {
                        finalizeChunk()
                        break
                    }
                }
                line = READER.readLine()
                line = checkForNoNewLineAtTheEndOfTheFile(line)
            }
            if (line == null || line!!.startsWith("--") && !line!!.startsWith("---")) {
                break
            }
        }

        var tailTxt = ""
        generateSequence { READER.readLine() }.forEach {
            if (tailTxt.isNotEmpty()) {
                tailTxt += "\n"
            }
            tailTxt += it
        }
        data.tail = tailTxt
        return data
    }

    @Throws(IOException::class)
    private fun checkForNoNewLineAtTheEndOfTheFile(line: String?): String? {
        if ("\\ No newline at end of file" == line) {
            actualFile.isNoNewLineAtTheEndOfTheFile = true
            return READER.readLine()
        }
        return line
    }

    @Throws(UnifiedDiffParserException::class)
    private fun processLine(line: String?, vararg rules: UnifiedDiffLine): Boolean {
        if (line == null) {
            return false
        }
        for (rule in rules) {
            if (rule.processLine(line)) {
//                LOG.fine("  >>> processed rule $rule")
                return true
            }
        }
//        LOG.warning("  >>> no rule matched $line")
        return false
        //throw new UnifiedDiffParserException("parsing error at line " + line);
    }

    private fun validLine(line: String?, vararg rules: UnifiedDiffLine): Boolean {
        if (line == null) {
            return false
        }
        for (rule in rules) {
            if (rule.validLine(line)) {
//                LOG.fine("  >>> accepted rule $rule")
                return true
            }
        }
        return false
    }

    private fun initFileIfNecessary() {
        check(originalTxt.isEmpty() && revisedTxt.isEmpty())
        if (::actualFile.isInitialized.not()) {
            actualFile = UnifiedDiffFile()
            data.addFile(actualFile)
        }
    }

    private fun processDiff(match: MatchResult, line: String) {
        //initFileIfNecessary();
//        LOG.log(java.util.logging.Level.FINE, "start {0}", line)
        val fromTo = parseFileNames(READER.lastLine)
        actualFile.fromFile = fromTo[0]
        actualFile.toFile = fromTo[1]
        actualFile.diffCommand = line
    }

    private fun processSimilarityIndex(match: MatchResult, line: String) {
        actualFile.similarityIndex = match.groupValues[1].toInt()
    }

    private val originalTxt: MutableList<String> = mutableListOf()
    private val revisedTxt: MutableList<String> = mutableListOf()
    private val addLineIdxList: MutableList<Int> = mutableListOf()
    private val delLineIdxList: MutableList<Int> = mutableListOf()
    private var old_ln = 0
    private var old_size = 0
    private var new_ln = 0
    private var new_size = 0
    private var delLineIdx = 0
    private var addLineIdx = 0

    private fun finalizeChunk() {
        if (!originalTxt.isEmpty() || !revisedTxt.isEmpty()) {
            actualFile!!.patch.addDelta(
                ChangeDelta(
                    Chunk(
                        old_ln - 1, originalTxt, delLineIdxList
                    ), Chunk(
                        new_ln - 1, revisedTxt, addLineIdxList
                    )
                )
            )
            old_ln = 0
            new_ln = 0
            originalTxt.clear()
            revisedTxt.clear()
            addLineIdxList.clear()
            delLineIdxList.clear()
            delLineIdx = 0
            addLineIdx = 0
        }
    }

    private fun processNormalLine(match: MatchResult, line: String) {
        val cline = line.substring(1)
        originalTxt.add(cline)
        revisedTxt.add(cline)
        delLineIdx++
        addLineIdx++
    }

    private fun processAddLine(match: MatchResult, line: String) {
        val cline = line.substring(1)
        revisedTxt.add(cline)
        addLineIdx++
        addLineIdxList.add(new_ln - 1 + addLineIdx)
    }

    private fun processDelLine(match: MatchResult, line: String) {
        val cline = line.substring(1)
        originalTxt.add(cline)
        delLineIdx++
        delLineIdxList.add(old_ln - 1 + delLineIdx)
    }

    private fun processChunk(match: MatchResult, chunkStart: String) {
        // finalizeChunk();
        old_ln = toInteger(match, 1, 1)
        old_size = toInteger(match, 2, 1)
        new_ln = toInteger(match, 3, 1)
        new_size = toInteger(match, 4, 1)
        if (old_ln == 0) {
            old_ln = 1
        }
        if (new_ln == 0) {
            new_ln = 1
        }
    }

    private fun processIndex(match: MatchResult, line: String) {
        //initFileIfNecessary();
//        LOG.log(java.util.logging.Level.FINE, "index {0}", line)
        actualFile.index = line.substring(6)
    }

    private fun processFromFile(match: MatchResult, line: String) {
        //initFileIfNecessary();
        actualFile.fromFile = extractFileName(line)
        actualFile.fromTimestamp = extractTimestamp(line)
    }

    private fun processToFile(match: MatchResult, line: String) {
        //initFileIfNecessary();
        actualFile.toFile = extractFileName(line)
        actualFile.toTimestamp = extractTimestamp(line)
    }

    private fun processRenameFrom(match: MatchResult, line: String) {
        actualFile.renameFrom = match.groupValues[1]
    }

    private fun processRenameTo(match: MatchResult, line: String) {
        actualFile.renameTo = match.groupValues[1]
    }

    private fun processNewFileMode(match: MatchResult, line: String) {
        //initFileIfNecessary();
        actualFile.newFileMode = match.groupValues[1]
    }

    private fun processDeletedFileMode(match: MatchResult, line: String) {
        //initFileIfNecessary();
        actualFile.deletedFileMode = match.groupValues[1]
    }

    private fun extractFileName(_line: String): String {
        val matcher = TIMESTAMP_REGEXP.find(_line)
        var line = _line
        if (matcher != null) {
            line = line.substring(0, matcher.range.first)
        }
        line = line.split("\t").toTypedArray()[0]
        return line.substring(4).replaceFirst("^(a|b|old|new)(\\/)?".toRegex(), "")
            .trim { it <= ' ' }
    }

    private fun extractTimestamp(line: String): String? {
        return TIMESTAMP_REGEXP.find(line)?.value
    }

    public companion object {
        public val UNIFIED_DIFF_CHUNK_REGEXP: Regex =
            Regex("^@@\\s+-(?:(\\d+)(?:,(\\d+))?)\\s+\\+(?:(\\d+)(?:,(\\d+))?)\\s+@@")
        public val TIMESTAMP_REGEXP: Regex =
            Regex("(\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}\\.\\d{3,})(?: [+-]\\d+)?")

        public fun parseFileNames(line: String?): Array<String> {
            val split = line!!.split(" ").toTypedArray()
            return arrayOf(
                split[2].replace("^a/".toRegex(), ""),
                split[3].replace("^b/".toRegex(), "")
            )
        }

//        private val LOG: java.util.logging.Logger =
//            java.util.logging.Logger.getLogger(UnifiedDiffReader::class.java.getName())

        /**
         * To parse a diff file use this method.
         *
         * @param stream This is the diff file data.
         * @return In a UnifiedDiff structure this diff file data is returned.
         * @throws IOException
         * @throws UnifiedDiffParserException
         */
        @Throws(IOException::class, UnifiedDiffParserException::class)
        public fun parseUnifiedDiff(stream: Source): UnifiedDiff {
            val parser = UnifiedDiffReader(stream.buffer())
            return parser.parse()
        }

        @Throws(NumberFormatException::class)
        private fun toInteger(match: MatchResult, group: Int, defValue: Int): Int {
            return (match.groupValues.getOrNull(group) ?: ("" + defValue)).toInt()
        }
    }
}

internal class InternalUnifiedDiffReader(source: Source) {
    internal val buffer = source.buffer()

    var lastLine: String? = null
    private set

    fun readLine(): String? {
        lastLine = buffer.readUtf8Line()
        return lastLine
    }
}