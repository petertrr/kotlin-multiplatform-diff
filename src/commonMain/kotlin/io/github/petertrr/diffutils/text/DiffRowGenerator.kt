/*
 * Copyright 2021 Peter Trifanov.
 * Copyright 2009-2017 java-diff-utils.
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
package io.github.petertrr.diffutils.text

import io.github.petertrr.diffutils.diff
import io.github.petertrr.diffutils.patch.ChangeDelta
import io.github.petertrr.diffutils.patch.Chunk
import io.github.petertrr.diffutils.patch.DeleteDelta
import io.github.petertrr.diffutils.patch.Delta
import io.github.petertrr.diffutils.patch.DeltaType
import io.github.petertrr.diffutils.patch.InsertDelta
import io.github.petertrr.diffutils.patch.Patch
import kotlin.math.max
import kotlin.math.min

/**
 * This class for generating DiffRows for side-by-side view. You can customize
 * the way of generating. For example, show inline diffs on not, ignoring white
 * spaces or/and blank lines and so on. All parameters for generating are
 * optional. If you do not specify them, the class will use the default values.
 */
@Suppress("LongParameterList")
public class DiffRowGenerator(
    /**
     * Set the column width of generated lines of original and revised
     * texts. Making it < 0 doesn't make any sense.
     */
    public val columnWidth: Int = 80,

    /**
     * Ignore white spaces in generating diff rows or not.
     */
    public val ignoreWhiteSpaces: Boolean = false,

    /**
     * Provide an equalizer for diff processing.
     */
    private var equalizer: ((String, String) -> Boolean) = if (ignoreWhiteSpaces) IGNORE_WHITESPACE_EQUALIZER else DEFAULT_EQUALIZER,

    /**
     * Per default each character is separately processed. Setting this parameter to `true`
     * introduces processing by word, which does not deliver in word
     * changes. Therefore, the whole word will be tagged as changed:
     *
     * ```
     * <pre>
     * false:    (aBa : aba) --  changed: a(B)a : a(b)a
     * true:     (aBa : aba) --  changed: (aBa) : (aba)
     * </pre> *
     * ```
     */
    inlineDiffByWord: Boolean = false,

    /**
     * To provide some customized splitting a splitter can be provided. Here
     * someone could think about sentence splitter, comma splitter or stuff
     * like that.
     */
    public val inlineDiffSplitter: (String) -> List<String> = if (inlineDiffByWord) SPLITTER_BY_WORD else SPLITTER_BY_CHARACTER,

    /**
     * Merge the complete result within the original text. This makes sense
     * for one line display.
     */
    public val mergeOriginalRevised: Boolean = false,

    /**
     * Generator for New-Text-Tags.
     */
    public val newTag: (DiffRow.Tag, Boolean) -> String = { _: DiffRow.Tag, f: Boolean -> if (f) "<span class=\"editNewInline\">" else "</span>" },

    /**
     * Generator for Old-Text-Tags.
     */
    public val oldTag: (DiffRow.Tag, Boolean) -> String = { _: DiffRow.Tag, f: Boolean -> if (f) "<span class=\"editOldInline\">" else "</span>" },

    /**
     * Give the original old and new text lines to DiffRow without any
     * additional processing and without any tags to highlight the change.
     */
    public val reportLinesUnchanged: Boolean = false,

    /**
     * By default DiffRowGenerator preprocesses lines for HTML output. Tabs
     * and special HTML characters like "&lt;" are replaced with its encoded
     * value. To change this you can provide a customized line normalizer
     * here.
     */
    public val lineNormalizer: (String) -> String = LINE_NORMALIZER_FOR_HTML,

    /**
     * Optional processor for diffed text parts. Here e.g. white characters could be
     * replaced by something visible.
     */
    public val processDiffs: ((String) -> String)? = null,

    /**
     * Show inline diffs in generating diff rows or not. Default: false.
     */
    public val showInlineDiffs: Boolean = false,

    /**
     * Sometimes it happens that a change contains multiple lines. If there
     * is no correspondence in old and new. To keep the merged line more
     * readable the linefeeds could be replaced by spaces.
     */
    public val replaceOriginalLinefeedInChangesWithSpaces: Boolean = false,
) {
    /**
     * Get the DiffRows describing the difference between original and revised
     * texts. Useful for displaying side-by-side diff.
     *
     * @param original the original text
     * @param revised the revised text
     * @return the DiffRows between original and revised texts
     */
    public fun generateDiffRows(original: List<String>, revised: List<String>): List<DiffRow> {
        return generateDiffRows(original, diff(original, revised, equalizer))
    }

    /**
     * Generates the DiffRows describing the difference between original and
     * revised texts using the given patch. Useful for displaying side-by-side
     * diff.
     *
     * @param original the original text
     * @param patch the given patch
     * @return the DiffRows between original and revised texts
     */
    public fun generateDiffRows(original: List<String>, patch: Patch<String>): List<DiffRow> {
        val diffRows: MutableList<DiffRow> = ArrayList()
        var endPos = 0
        for (originalDelta in patch.deltas) {
            for (delta in decompressDeltas(originalDelta)) {
                endPos = transformDeltaIntoDiffRow(original, endPos, diffRows, delta)
            }
        }

        // Copy the final matching chunk if any.
        for (line in original.subList(endPos, original.size)) {
            diffRows.add(buildDiffRow(DiffRow.Tag.EQUAL, line, line))
        }
        return diffRows
    }

    /**
     * Transforms one patch delta into a DiffRow object.
     *
     * @param endPos line number after previous delta end
     */
    private fun transformDeltaIntoDiffRow(
        original: List<String>,
        endPos: Int,
        diffRows: MutableList<DiffRow>,
        delta: Delta<String>
    ): Int {
        val orig: Chunk<String> = delta.source
        val rev: Chunk<String> = delta.target
        for (line in original.subList(endPos, orig.position)) {
            // all lines since previous delta until the start of the current delta
            diffRows.add(buildDiffRow(DiffRow.Tag.EQUAL, line, line))
        }
        when (delta.type) {
            DeltaType.INSERT -> for (line in rev.lines) {
                diffRows.add(buildDiffRow(DiffRow.Tag.INSERT, "", line))
            }
            DeltaType.DELETE -> for (line in orig.lines) {
                diffRows.add(buildDiffRow(DiffRow.Tag.DELETE, line, ""))
            }
            else -> if (showInlineDiffs) {
                diffRows.addAll(generateInlineDiffs(delta))
            } else {
                for (j in 0 until max(orig.size(), rev.size())) {
                    diffRows.add(
                        buildDiffRow(
                            DiffRow.Tag.CHANGE,
                            orig.lines.getOrElse(j) { "" },
                            rev.lines.getOrElse(j) { "" }
                        )
                    )
                }
            }
        }
        return orig.last() + 1
    }

    /**
     * Decompresses ChangeDeltas with different source and target size to a
     * ChangeDelta with same size and a following InsertDelta or DeleteDelta.
     * With this, problems of building DiffRows getting smaller.
     * If sizes are equal, returns a list with single element - [delta].
     */
    private fun decompressDeltas(delta: Delta<String>): List<Delta<String>> {
        if (delta.type == DeltaType.CHANGE && delta.source.size() != delta.target.size()) {
            val deltas: MutableList<Delta<String>> = ArrayList()
            val minSize: Int = min(delta.source.size(), delta.target.size())
            val orig: Chunk<String> = delta.source
            val rev: Chunk<String> = delta.target
            deltas.add(
                ChangeDelta(
                    Chunk(orig.position, orig.lines.subList(0, minSize)),
                    Chunk(rev.position, rev.lines.subList(0, minSize))
                )
            )
            if (orig.lines.size < rev.lines.size) {
                deltas.add(
                    InsertDelta(
                        Chunk(orig.position + minSize, emptyList()),
                        Chunk(rev.position + minSize, rev.lines.subList(minSize, rev.lines.size))
                    )
                )
            } else {
                deltas.add(
                    DeleteDelta(
                        Chunk(orig.position + minSize, orig.lines.subList(minSize, orig.lines.size)),
                        Chunk(rev.position + minSize, emptyList())
                    )
                )
            }
            return deltas
        }
        return listOf(delta)
    }

    private fun buildDiffRow(type: DiffRow.Tag, orgline: String, newline: String): DiffRow {
        return if (reportLinesUnchanged) {
            DiffRow(type, orgline, newline)
        } else {
            var wrapOrg = preprocessLine(orgline)
            if (DiffRow.Tag.DELETE == type) {
                if (mergeOriginalRevised || showInlineDiffs) {
                    wrapOrg = oldTag.invoke(type, true) + wrapOrg + oldTag.invoke(type, false)
                }
            }
            var wrapNew = preprocessLine(newline)
            if (DiffRow.Tag.INSERT == type) {
                if (mergeOriginalRevised) {
                    wrapOrg = newTag.invoke(type, true) + wrapNew + newTag.invoke(type, false)
                } else if (showInlineDiffs) {
                    wrapNew = newTag.invoke(type, true) + wrapNew + newTag.invoke(type, false)
                }
            }
            DiffRow(type, wrapOrg, wrapNew)
        }
    }

    private fun buildDiffRowWithoutNormalizing(type: DiffRow.Tag, orgline: String, newline: String): DiffRow {
        return DiffRow(
            type,
            wrapText(orgline, columnWidth),
            wrapText(newline, columnWidth)
        )
    }

    internal fun normalizeLines(list: List<String>): List<String> {
        return if (reportLinesUnchanged) list else list.map { lineNormalizer.invoke(it) }
    }

    /**
     * Add the inline diffs for given delta
     *
     * @param delta the given delta
     */
    @Suppress("LongMethod")
    private fun generateInlineDiffs(delta: Delta<String>): List<DiffRow> {
        val orig = normalizeLines(delta.source.lines)
        val rev = normalizeLines(delta.target.lines)
        val joinedOrig: String = orig.joinToString("\n")
        val joinedRev: String = rev.joinToString("\n")
        val origList = inlineDiffSplitter.invoke(joinedOrig).toMutableList()
        val revList = inlineDiffSplitter.invoke(joinedRev).toMutableList()
        // copying of `origList` and `revList` is needed because otherwise `wrapInTag` results in ConcurrentModificationException
        val inlineDeltas: MutableList<Delta<String>> = diff(origList.toList(), revList.toList(), equalizer)
            .deltas
            .asReversed()
            .toMutableList()
        for (inlineDelta in inlineDeltas) {
            val inlineOrig: Chunk<String> = inlineDelta.source
            val inlineRev: Chunk<String> = inlineDelta.target
            when (inlineDelta.type) {
                DeltaType.DELETE -> {
                    wrapInTag(
                        origList,
                        inlineOrig.position,
                        inlineOrig.position + inlineOrig.size(),
                        DiffRow.Tag.DELETE,
                        oldTag,
                        processDiffs,
                        replaceOriginalLinefeedInChangesWithSpaces && mergeOriginalRevised
                    )
                }
                DeltaType.INSERT -> {
                    if (mergeOriginalRevised) {
                        origList.addAll(
                            inlineOrig.position,
                            revList.subList(
                                inlineRev.position,
                                inlineRev.position + inlineRev.size()
                            )
                        )
                        wrapInTag(
                            origList,
                            inlineOrig.position,
                            inlineOrig.position + inlineRev.size(),
                            DiffRow.Tag.INSERT,
                            newTag,
                            processDiffs,
                            false
                        )
                    } else {
                        wrapInTag(
                            revList,
                            inlineRev.position,
                            inlineRev.position + inlineRev.size(),
                            DiffRow.Tag.INSERT,
                            newTag,
                            processDiffs,
                            false
                        )
                    }
                }
                DeltaType.CHANGE -> {
                    if (mergeOriginalRevised) {
                        origList.addAll(
                            inlineOrig.position + inlineOrig.size(),
                            revList.subList(
                                inlineRev.position,
                                inlineRev.position + inlineRev.size()
                            )
                        )
                        wrapInTag(
                            origList,
                            inlineOrig.position + inlineOrig.size(),
                            inlineOrig.position + inlineOrig.size() + inlineRev.size(),
                            DiffRow.Tag.CHANGE,
                            newTag,
                            processDiffs,
                            false
                        )
                    } else {
                        wrapInTag(
                            revList,
                            inlineRev.position,
                            inlineRev.position + inlineRev.size(),
                            DiffRow.Tag.CHANGE,
                            newTag,
                            processDiffs,
                            false
                        )
                    }
                    wrapInTag(
                        origList,
                        inlineOrig.position,
                        inlineOrig.position + inlineOrig.size(),
                        DiffRow.Tag.CHANGE,
                        oldTag,
                        processDiffs,
                        replaceOriginalLinefeedInChangesWithSpaces && mergeOriginalRevised
                    )
                }
                else -> error("Unexpected delta type ${inlineDelta.type}")
            }
        }
        val origResult = StringBuilder()
        val revResult = StringBuilder()
        for (character in origList) {
            origResult.append(character)
        }
        for (character in revList) {
            revResult.append(character)
        }
        // note: dropLastWhile here arose from compatibility with Java: Java's `split` discard trailing empty string by default
        val original: List<String> = origResult.toString().split("\n").dropLastWhile { it.isEmpty() }
        val revised: List<String> = revResult.toString().split("\n").dropLastWhile { it.isEmpty() }
        val diffRows: MutableList<DiffRow> = ArrayList()
        for (j in 0 until max(original.size, revised.size)) {
            diffRows.add(
                buildDiffRowWithoutNormalizing(
                    DiffRow.Tag.CHANGE,
                    original.getOrElse(j) { "" },
                    revised.getOrElse(j) { "" }
                )
            )
        }
        return diffRows
    }

    private fun preprocessLine(line: String): String {
        return if (columnWidth == 0) {
            lineNormalizer.invoke(line)
        } else {
            wrapText(lineNormalizer.invoke(line), columnWidth)
        }
    }

    public companion object {
        internal val DEFAULT_EQUALIZER: (Any?, Any?) -> Boolean = { o1: Any?, o2: Any? -> o1 == o2 }
        internal val IGNORE_WHITESPACE_EQUALIZER: (String, String) -> Boolean = { original: String, revised: String ->
            adjustWhitespace(
                original
            ) == adjustWhitespace(revised)
        }
        internal val LINE_NORMALIZER_FOR_HTML: (String) -> String = { normalize(it) }

        /**
         * Splitting lines by character to achieve char by char diff checking.
         */
        internal val SPLITTER_BY_CHARACTER = { line: String ->
            val list: MutableList<String> = ArrayList(line.length)
            for (character in line.toCharArray()) {
                list.add(character.toString())
            }
            list.toList()
        }
        internal val SPLIT_BY_WORD_PATTERN = Regex("\\s+|[,.\\[\\](){}/\\\\*+\\-#]")

        /**
         * Splitting lines by word to achieve word by word diff checking.
         */
        internal val SPLITTER_BY_WORD = { line: String ->
            splitStringPreserveDelimiter(
                line,
                SPLIT_BY_WORD_PATTERN
            )
        }
        internal val WHITESPACE_PATTERN = Regex("\\s+")

        private fun adjustWhitespace(raw: String): String {
            return WHITESPACE_PATTERN.replace(raw.trim(), " ")
        }

        internal fun splitStringPreserveDelimiter(str: String?, splitPattern: Regex): List<String> {
            val list: MutableList<String> = mutableListOf()
            if (str != null) {
                val matchResults = splitPattern.findAll(str)
                var pos = 0
                for (matchResult in matchResults) {
                    if (pos < matchResult.range.first) {
                        list.add(str.substring(pos, matchResult.range.first))
                    }
                    list.add(matchResult.value)
                    pos = matchResult.range.last + 1
                }
                if (pos < str.length) {
                    list.add(str.substring(pos))
                }
            }
            return list
        }

        /**
         * Wrap the elements in the sequence with the given tag
         *
         * @param startPosition the position from which tag should start. The
         * counting start from a zero.
         * @param endPosition the position before which tag should should be closed.
         * @param tagGenerator the tag generator
         */
        @Suppress("LongParameterList", "ComplexMethod", "LoopWithTooManyJumpStatements", "NestedBlockDepth")
        internal fun wrapInTag(
            sequence: MutableList<String>,
            startPosition: Int,
            endPosition: Int,
            tag: DiffRow.Tag,
            tagGenerator: (DiffRow.Tag, Boolean) -> String,
            processDiffs: ((String) -> String)?,
            replaceLinefeedWithSpace: Boolean,
        ): MutableList<String> {
            var endPos = endPosition
            while (endPos >= startPosition) {
                // search position for end tag
                while (endPos > startPosition) {
                    if ("\n" != sequence[endPos - 1]) {
                        break
                    } else if (replaceLinefeedWithSpace) {
                        sequence[endPos - 1] = " "
                        break
                    }
                    endPos--
                }
                if (endPos == startPosition) {
                    break
                }
                sequence.add(endPos, tagGenerator.invoke(tag, false))
                if (processDiffs != null) {
                    sequence[endPos - 1] = processDiffs.invoke(sequence[endPos - 1])
                }
                endPos--

                // search position for end tag
                while (endPos > startPosition) {
                    if ("\n" == sequence[endPos - 1]) {
                        if (replaceLinefeedWithSpace) {
                            sequence[endPos - 1] = " "
                        } else {
                            break
                        }
                    }
                    if (processDiffs != null) {
                        sequence[endPos - 1] = processDiffs.invoke(sequence[endPos - 1])
                    }
                    endPos--
                }
                sequence.add(endPos, tagGenerator.invoke(tag, true))
                endPos--
            }
            return sequence
        }
    }
}
