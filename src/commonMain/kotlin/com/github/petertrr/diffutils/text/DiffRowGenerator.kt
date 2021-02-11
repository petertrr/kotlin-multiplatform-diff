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
 * This class for generating DiffRows for side-by-sidy view. You can customize
 * the way of generating. For example, show inline diffs on not, ignoring white
 * spaces or/and blank lines and so on. All parameters for generating are
 * optional. If you do not specify them, the class will use the default values.
 *
 * These values are: showInlineDiffs = false; ignoreWhiteSpaces = true;
 * ignoreBlankLines = true; ...
 *
 * For instantiating the DiffRowGenerator you should use the its builder. Like
 * in example  `
 * DiffRowGenerator generator = new DiffRowGenerator.Builder().showInlineDiffs(true).
 * ignoreWhiteSpaces(true).columnWidth(100).build();
` *
 */
class DiffRowGenerator private constructor(builder: Builder) {
    private val columnWidth: Int
    private var equalizer: ((String, String) -> Boolean)? = null
    private val ignoreWhiteSpaces: Boolean
    private val inlineDiffSplitter: (String) -> List<String>
    private val mergeOriginalRevised: Boolean
    private val newTag: (DiffRow.Tag, Boolean) -> String
    private val oldTag: (DiffRow.Tag, Boolean) -> String
    private val reportLinesUnchanged: Boolean
    private val lineNormalizer: (String) -> String
    private val processDiffs: ((String) -> String)?
    private val showInlineDiffs: Boolean
    private val replaceOriginalLinefeedInChangesWithSpaces: Boolean

    /**
     * Get the DiffRows describing the difference between original and revised
     * texts using the given patch. Useful for displaying side-by-side diff.
     *
     * @param original the original text
     * @param revised the revised text
     * @return the DiffRows between original and revised texts
     */
    fun generateDiffRows(original: List<String>, revised: List<String>): List<DiffRow> {
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
    fun generateDiffRows(original: List<String>, patch: Patch<String>): List<DiffRow> {
        val diffRows: MutableList<DiffRow> = ArrayList()
        var endPos = 0
        val deltaList: List<Delta<String>> = patch.getDeltas()
        for (originalDelta in deltaList) {
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
     */
    private fun transformDeltaIntoDiffRow(original: List<String>, endPos: Int, diffRows: MutableList<DiffRow>, delta: Delta<String>): Int {
        val orig: Chunk<String> = delta.source
        val rev: Chunk<String> = delta.target
        for (line in original.subList(endPos, orig.position)) {
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
                var j = 0
                while (j < max(orig.size(), rev.size())) {
                    diffRows.add(
                        buildDiffRow(
                            DiffRow.Tag.CHANGE,
                            if (orig.lines.size > j) orig.lines[j] else "",
                            if (rev.lines.size > j) rev.lines[j] else ""
                        )
                    )
                    j++
                }
            }
        }
        return orig.last() + 1
    }

    /**
     * Decompresses ChangeDeltas with different source and target size to a
     * ChangeDelta with same size and a following InsertDelta or DeleteDelta.
     * With this problems of building DiffRows getting smaller.
     *
     * @param deltaList
     */
    private fun decompressDeltas(delta: Delta<String>): List<Delta<String>> {
        if (delta.type == DeltaType.CHANGE && delta.source.size() != delta.target.size()) {
            val deltas: MutableList<Delta<String>> = ArrayList()
            // println("decompress this " + delta);
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

    fun normalizeLines(list: List<String>): List<String> {
        return if (reportLinesUnchanged) list else list.map { lineNormalizer.invoke(it) }
    }

    /**
     * Add the inline diffs for given delta
     *
     * @param delta the given delta
     */
    private fun generateInlineDiffs(delta: Delta<String>): List<DiffRow> {
        val orig = normalizeLines(delta.source.lines)
        val rev = normalizeLines(delta.target.lines)
        val joinedOrig: String = orig.joinToString("\n")
        val joinedRev: String = rev.joinToString("\n")
        val origList = inlineDiffSplitter.invoke(joinedOrig).toMutableList()
        val revList = inlineDiffSplitter.invoke(joinedRev).toMutableList()
        // todo: `origList.toList` is needed because otherwise `wrapInTag` results in ConcurrentModificationException
        val inlineDeltas: MutableList<Delta<String>> = diff(origList.toList(), revList, equalizer)
            .getDeltas()
            .reversed()
            .toMutableList()
        val inlineDeltasIterator = inlineDeltas.iterator()
        while (inlineDeltasIterator.hasNext()) {
            val inlineDelta = inlineDeltasIterator.next()
            val inlineOrig: Chunk<String> = inlineDelta.source
            val inlineRev: Chunk<String> = inlineDelta.target
            when(inlineDelta.type) {
                 DeltaType.DELETE -> {
                    wrapInTag(
                        origList, inlineOrig.position, inlineOrig.position + inlineOrig.size(),
                        DiffRow.Tag.DELETE, oldTag, processDiffs, replaceOriginalLinefeedInChangesWithSpaces && mergeOriginalRevised
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
                            origList, inlineOrig.position,
                            inlineOrig.position + inlineRev.size(),
                            DiffRow.Tag.INSERT, newTag, processDiffs, false
                        )
                    } else {
                        wrapInTag(
                            revList, inlineRev.position,
                            inlineRev.position + inlineRev.size(),
                            DiffRow.Tag.INSERT, newTag, processDiffs, false
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
                            origList, inlineOrig.position + inlineOrig.size(),
                            inlineOrig.position + inlineOrig.size() + inlineRev.size(),
                            DiffRow.Tag.CHANGE, newTag, processDiffs, false
                        )
                    } else {
                        wrapInTag(
                            revList, inlineRev.position,
                            inlineRev.position + inlineRev.size(),
                            DiffRow.Tag.CHANGE, newTag, processDiffs, false
                        )
                    }
                    wrapInTag(
                        origList, inlineOrig.position,
                        inlineOrig.position + inlineOrig.size(),
                        DiffRow.Tag.CHANGE, oldTag, processDiffs, replaceOriginalLinefeedInChangesWithSpaces && mergeOriginalRevised
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
        val original: List<String> = origResult.toString().split("\n")
        val revised: List<String> = revResult.toString().split("\n")
        val diffRows: MutableList<DiffRow> = ArrayList<DiffRow>()
        for (j in 0 until max(original.size, revised.size)) {
            diffRows.add(
                buildDiffRowWithoutNormalizing(
                    DiffRow.Tag.CHANGE,
                    if (original.size > j) original[j] else "",
                    if (revised.size > j) revised[j] else ""
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

    /**
     * This class used for building the DiffRowGenerator.
     */
    class Builder internal constructor() {
        internal var showInlineDiffs = false
        internal var ignoreWhiteSpaces = false
        internal var oldTag = { _: DiffRow.Tag, f: Boolean -> if (f) "<span class=\"editOldInline\">" else "</span>" }
        internal var newTag = { _: DiffRow.Tag, f: Boolean -> if (f) "<span class=\"editNewInline\">" else "</span>" }
        internal var columnWidth = 0
        internal var mergeOriginalRevised = false
        internal var reportLinesUnchanged = false
        internal var inlineDiffSplitter = SPLITTER_BY_CHARACTER
        internal var lineNormalizer = LINE_NORMALIZER_FOR_HTML
        internal var processDiffs: ((String) -> String)? = null
        internal var equalizer: ((String, String) -> Boolean)? = null
        internal var replaceOriginalLinefeedInChangesWithSpaces = false

        /**
         * Show inline diffs in generating diff rows or not.
         *
         * @param val the value to set. Default: false.
         * @return builder with configured showInlineDiff parameter
         */
        fun showInlineDiffs(`val`: Boolean): Builder {
            showInlineDiffs = `val`
            return this
        }

        /**
         * Ignore white spaces in generating diff rows or not.
         *
         * @param val the value to set. Default: true.
         * @return builder with configured ignoreWhiteSpaces parameter
         */
        fun ignoreWhiteSpaces(`val`: Boolean): Builder {
            ignoreWhiteSpaces = `val`
            return this
        }

        /**
         * Give the original old and new text lines to DiffRow without any
         * additional processing and without any tags to highlight the change.
         *
         * @param val the value to set. Default: false.
         * @return builder with configured reportLinesUnWrapped parameter
         */
        fun reportLinesUnchanged(`val`: Boolean): Builder {
            reportLinesUnchanged = `val`
            return this
        }

        /**
         * Generator for Old-Text-Tags.
         *
         * @param generator the tag generator
         * @return builder with configured ignoreBlankLines parameter
         */
        fun oldTag(generator: (DiffRow.Tag, Boolean) -> String): Builder {
            oldTag = generator
            return this
        }

        /**
         * Generator for Old-Text-Tags.
         *
         * @param generator the tag generator
         * @return builder with configured ignoreBlankLines parameter
         */
        fun oldTag(generator: (Boolean) -> String): Builder {
            oldTag = { _: DiffRow.Tag, f: Boolean -> generator.invoke(f) }
            return this
        }

        /**
         * Generator for New-Text-Tags.
         *
         * @param generator
         * @return
         */
        fun newTag(generator: (DiffRow.Tag, Boolean) -> String): Builder {
            newTag = generator
            return this
        }

        /**
         * Generator for New-Text-Tags.
         *
         * @param generator
         * @return
         */
        fun newTag(generator: (Boolean) -> String): Builder {
            newTag = { _: DiffRow.Tag, f: Boolean -> generator.invoke(f) }
            return this
        }

        /**
         * Processor for diffed text parts. Here e.g. white characters could be
         * replaced by something visible.
         *
         * @param processDiffs
         * @return
         */
        fun processDiffs(processDiffs: (String) -> String): Builder {
            this.processDiffs = processDiffs
            return this
        }

        /**
         * Set the column width of generated lines of original and revised
         * texts.
         *
         * @param width the width to set. Making it < 0 doesn't make any sense.
         * Default 80. @return builder with config of column width
         */
        fun columnWidth(width: Int): Builder {
            if (width >= 0) {
                columnWidth = width
            }
            return this
        }

        /**
         * Build the DiffRowGenerator. If some parameters is not set, the
         * default values are used.
         *
         * @return the customized DiffRowGenerator
         */
        fun build(): DiffRowGenerator {
            return DiffRowGenerator(this)
        }

        /**
         * Merge the complete result within the original text. This makes sense
         * for one line display.
         *
         * @param mergeOriginalRevised
         * @return
         */
        fun mergeOriginalRevised(mergeOriginalRevised: Boolean): Builder {
            this.mergeOriginalRevised = mergeOriginalRevised
            return this
        }

        /**
         * Per default each character is separatly processed. This variant
         * introduces processing by word, which does not deliver in word
         * changes. Therefore the whole word will be tagged as changed:
         *
         * <pre>
         * false:    (aBa : aba) --  changed: a(B)a : a(b)a
         * true:     (aBa : aba) --  changed: (aBa) : (aba)
        </pre> *
         */
        fun inlineDiffByWord(inlineDiffByWord: Boolean): Builder {
            inlineDiffSplitter = if (inlineDiffByWord) SPLITTER_BY_WORD else SPLITTER_BY_CHARACTER
            return this
        }

        /**
         * To provide some customized splitting a splitter can be provided. Here
         * someone could think about sentence splitter, comma splitter or stuff
         * like that.
         *
         * @param inlineDiffSplitter
         * @return
         */
        fun inlineDiffBySplitter(inlineDiffSplitter: (String) -> List<String>): Builder {
            this.inlineDiffSplitter = inlineDiffSplitter
            return this
        }

        /**
         * By default DiffRowGenerator preprocesses lines for HTML output. Tabs
         * and special HTML characters like "&lt;" are replaced with its encoded
         * value. To change this you can provide a customized line normalizer
         * here.
         *
         * @param lineNormalizer
         * @return
         */
        fun lineNormalizer(lineNormalizer: (String) -> String): Builder {
            this.lineNormalizer = lineNormalizer
            return this
        }

        /**
         * Provide an equalizer for diff processing.
         *
         * @param equalizer equalizer for diff processing.
         * @return builder with configured equalizer parameter
         */
        fun equalizer(equalizer: (String, String) -> Boolean): Builder {
            this.equalizer = equalizer
            return this
        }

        /**
         * Sometimes it happens that a change contains multiple lines. If there
         * is no correspondence in old and new. To keep the merged line more
         * readable the linefeeds could be replaced by spaces.
         *
         * @param replace
         * @return
         */
        fun replaceOriginalLinefeedInChangesWithSpaces(replace: Boolean): Builder {
            replaceOriginalLinefeedInChangesWithSpaces = replace
            return this
        }
    }

    companion object {
        val DEFAULT_EQUALIZER = { o1: Any?, o2: Any? -> o1 == o2 }
        val IGNORE_WHITESPACE_EQUALIZER = { original: String, revised: String ->
                adjustWhitespace(
                    original
                ) == adjustWhitespace(revised)
            }
        val LINE_NORMALIZER_FOR_HTML: (String) -> String = { normalize(it) }

        /**
         * Splitting lines by character to achieve char by char diff checking.
         */
        val SPLITTER_BY_CHARACTER = { line: String ->
                val list: MutableList<String> = ArrayList(line.length)
                for (character in line.toCharArray()) {
                    list.add(character.toString())
                }
                list.toList()
            }
        val SPLIT_BY_WORD_PATTERN = Regex("\\s+|[,.\\[\\](){}/\\\\*+\\-#]")

        /**
         * Splitting lines by word to achieve word by word diff checking.
         */
        val SPLITTER_BY_WORD = { line: String ->
                splitStringPreserveDelimiter(
                    line,
                    SPLIT_BY_WORD_PATTERN
                )
            }
        val WHITESPACE_PATTERN = Regex("\\s+")
        fun create(): Builder {
            return Builder()
        }

        private fun adjustWhitespace(raw: String): String {
            return WHITESPACE_PATTERN.replace(raw.trim { it <= ' ' }, " ")
        }

        protected fun splitStringPreserveDelimiter(str: String?, SPLIT_PATTERN: Regex): List<String> {
            val list: MutableList<String> = mutableListOf()
            if (str != null) {
                var matchResult = SPLIT_PATTERN.matchEntire(str)
                var pos = 0
                while (matchResult != null) {
                    if (pos < matchResult.range.first) {
                        list.add(str.substring(pos, matchResult.range.first))
                    }
                    list.add(matchResult.value)
                    pos = matchResult.range.last
                    matchResult = matchResult.next()
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
        fun wrapInTag(
            sequence: MutableList<String>, startPosition: Int,
            endPosition: Int, tag: DiffRow.Tag, tagGenerator: (DiffRow.Tag, Boolean) -> String,
            processDiffs: ((String) -> String)?, replaceLinefeedWithSpace: Boolean
        ) {
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

                //search position for end tag
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
        }


    }

    init {
        showInlineDiffs = builder.showInlineDiffs
        ignoreWhiteSpaces = builder.ignoreWhiteSpaces
        oldTag = builder.oldTag
        newTag = builder.newTag
        columnWidth = builder.columnWidth
        mergeOriginalRevised = builder.mergeOriginalRevised
        inlineDiffSplitter = builder.inlineDiffSplitter
        equalizer = if (builder.equalizer != null) {
            builder.equalizer
        } else {
            if (ignoreWhiteSpaces) IGNORE_WHITESPACE_EQUALIZER else DEFAULT_EQUALIZER
        }
        reportLinesUnchanged = builder.reportLinesUnchanged
        lineNormalizer = builder.lineNormalizer
        processDiffs = builder.processDiffs
        replaceOriginalLinefeedInChangesWithSpaces = builder.replaceOriginalLinefeedInChangesWithSpaces
        requireNotNull(inlineDiffSplitter)
        requireNotNull(lineNormalizer)
    }
}