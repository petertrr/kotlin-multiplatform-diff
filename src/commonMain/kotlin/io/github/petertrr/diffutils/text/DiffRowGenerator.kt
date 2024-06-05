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

import io.github.petertrr.diffutils.algorithm.DiffEqualizer
import io.github.petertrr.diffutils.algorithm.EqualsDiffEqualizer
import io.github.petertrr.diffutils.algorithm.IgnoreWsStringDiffEqualizer
import io.github.petertrr.diffutils.diff
import io.github.petertrr.diffutils.patch.ChangeDelta
import io.github.petertrr.diffutils.patch.Chunk
import io.github.petertrr.diffutils.patch.DeleteDelta
import io.github.petertrr.diffutils.patch.Delta
import io.github.petertrr.diffutils.patch.DeltaType
import io.github.petertrr.diffutils.patch.InsertDelta
import io.github.petertrr.diffutils.patch.Patch
import io.github.petertrr.diffutils.wrapText
import kotlin.math.max
import kotlin.math.min

/**
 * This class for generating [DiffRow]s for side-by-side view. You can customize
 * the way of generating. For example, show inline diffs on not, ignoring white
 * spaces or/and blank lines and so on. All parameters for generating are
 * optional. If you do not specify them, the class will use the default values.
 *
 * @param columnWidth Set the column width of generated lines of original and revised texts.
 *   Making it < 0 doesn't make any sense.
 * @param ignoreWhiteSpaces Ignore white spaces in generating diff rows or not
 * @param equalizer Provide an equalizer for diff processing
 * @param inlineDiffByWord Per default each character is separately processed.
 *   Setting this parameter to `true` introduces processing by word, which does
 *   not deliver in word changes. Therefore, the whole word will be tagged as changed:
 *   ```
 *   false:    (aBa : aba) --  changed: a(B)a : a(b)a
 *   true:     (aBa : aba) --  changed: (aBa) : (aba)
 *   ```
 *   Default: `false`
 * @param inlineDiffSplitter To provide some customized splitting a splitter can be provided.
 *   Here someone could think about sentence splitter, comma splitter or stuff like that.
 * @param mergeOriginalRevised Merge the complete result within the original text.
 *   This makes sense for one line display.
 *   Default: `false`
 * @param newTag Generator for New-Text-Tags
 * @param oldTag Generator for Old-Text-Tags
 * @param reportLinesUnchanged Report all lines without markup on the old or new text.
 *   Default: `false`
 * @param lineNormalizer By default, [DiffRowGenerator] preprocesses lines for HTML output.
 *   Tabs and special HTML characters like "&lt;" are replaced with its encoded value.
 *   To change this you can provide a customized line normalizer here.
 * @param processDiffs Optional processor for diffed text parts.
 *   Here e.g. white characters could be replaced by something visible.
 * @param showInlineDiffs Show inline diffs in generating diff rows or not.
 *   Default: `false`
 * @param replaceOriginalLinefeedInChangesWithSpaces Sometimes it happens that a change
 *   contains multiple lines. If there is no correspondence in old and new.
 *   To keep the merged line more readable the line feeds could be replaced by spaces.
 *   Default: `false`
 * @param decompressDeltas Deltas could be in a state, that would produce some unreasonable
 *   results within an inline diff. So the deltas are decompressed into smaller parts and rebuild.
 *   But this could result in more differences.
 *   Default: `true`
 */
@Suppress("LongParameterList")
public class DiffRowGenerator(
    private val columnWidth: Int = 80,
    private val ignoreWhiteSpaces: Boolean = false,
    private var equalizer: DiffEqualizer<String> = if (ignoreWhiteSpaces) IgnoreWsStringDiffEqualizer() else EqualsDiffEqualizer(),
    inlineDiffByWord: Boolean = false,
    private val inlineDiffSplitter: DiffSplitter = if (inlineDiffByWord) WordDiffSplitter() else CharDiffSplitter(),
    private val mergeOriginalRevised: Boolean = false,
    private val newTag: DiffTagGenerator = HtmlDiffTagGenerator("editNewInline"),
    private val oldTag: DiffTagGenerator = HtmlDiffTagGenerator("editOldInline"),
    private val reportLinesUnchanged: Boolean = false,
    private val lineNormalizer: DiffLineNormalizer = HtmlLineNormalizer(),
    private val processDiffs: DiffLineProcessor? = null,
    private val showInlineDiffs: Boolean = false,
    private val replaceOriginalLinefeedInChangesWithSpaces: Boolean = false,
    private val decompressDeltas: Boolean = true,
) {
    /**
     * Get the [DiffRow]s describing the difference between original and revised
     * texts. Useful for displaying side-by-side diff.
     *
     * @param original The original text
     * @param revised The revised text
     * @return The [DiffRow]s between original and revised texts
     */
    public fun generateDiffRows(original: List<String>, revised: List<String>): List<DiffRow> =
        generateDiffRows(original, diff(original, revised, equalizer))

    /**
     * Generates the [DiffRow]s describing the difference between original and
     * revised texts using the given patch. Useful for displaying side-by-side
     * diff.
     *
     * @param original The original text
     * @param patch The given patch
     * @return The [DiffRow]s between original and revised texts
     */
    @Suppress("MemberVisibilityCanBePrivate")
    public fun generateDiffRows(original: List<String>, patch: Patch<String>): List<DiffRow> {
        val diffRows = ArrayList<DiffRow>()
        var endPos = 0

        if (decompressDeltas) {
            for (originalDelta in patch.deltas) {
                for (delta in decompressDeltas(originalDelta)) {
                    endPos = transformDeltaIntoDiffRow(original, endPos, diffRows, delta)
                }
            }
        } else {
            for (delta in patch.deltas) {
                endPos = transformDeltaIntoDiffRow(original, endPos, diffRows, delta)
            }
        }

        // Copy the final matching chunk if any
        for (line in original.subList(endPos, original.size)) {
            diffRows.add(buildDiffRow(DiffRow.Tag.EQUAL, line, line))
        }

        return diffRows
    }

    private fun normalizeLines(list: List<String>): List<String> =
        if (reportLinesUnchanged) list else list.map(lineNormalizer::normalize)

    /**
     * Transforms one patch delta into a [DiffRow] object.
     *
     * @param endPos Line number after previous delta end
     */
    private fun transformDeltaIntoDiffRow(
        original: List<String>,
        endPos: Int,
        diffRows: MutableList<DiffRow>,
        delta: Delta<String>,
    ): Int {
        val orig = delta.source
        val rev = delta.target

        for (line in original.subList(endPos, orig.position)) {
            // All lines since previous delta until the start of the current delta
            diffRows.add(buildDiffRow(DiffRow.Tag.EQUAL, line, line))
        }

        when (delta.type) {
            DeltaType.INSERT -> {
                for (line in rev.lines) {
                    val row = buildDiffRow(DiffRow.Tag.INSERT, "", line)
                    diffRows.add(row)
                }
            }
            DeltaType.DELETE -> {
                for (line in orig.lines) {
                    val row = buildDiffRow(DiffRow.Tag.DELETE, line, "")
                    diffRows.add(row)
                }
            }
            else -> {
                if (showInlineDiffs) {
                    diffRows.addAll(generateInlineDiffs(delta))
                } else {
                    for (j in 0..<max(orig.size(), rev.size())) {
                        val row = buildDiffRow(
                            type = DiffRow.Tag.CHANGE,
                            orgLine = orig.lines.getOrNull(j) ?: "",
                            newLine = rev.lines.getOrNull(j) ?: "",
                        )

                        diffRows.add(row)
                    }
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
            val deltas = ArrayList<Delta<String>>()
            val minSize = min(delta.source.size(), delta.target.size())
            val orig = delta.source
            val rev = delta.target

            deltas.add(
                ChangeDelta(
                    Chunk(orig.position, orig.lines.subList(0, minSize)),
                    Chunk(rev.position, rev.lines.subList(0, minSize)),
                )
            )

            if (orig.lines.size < rev.lines.size) {
                deltas.add(
                    InsertDelta(
                        Chunk(orig.position + minSize, emptyList()),
                        Chunk(rev.position + minSize, rev.lines.subList(minSize, rev.lines.size)),
                    )
                )
            } else {
                deltas.add(
                    DeleteDelta(
                        Chunk(orig.position + minSize, orig.lines.subList(minSize, orig.lines.size)),
                        Chunk(rev.position + minSize, emptyList()),
                    )
                )
            }

            return deltas
        }

        return listOf(delta)
    }

    private fun buildDiffRow(type: DiffRow.Tag, orgLine: String, newLine: String): DiffRow {
        if (reportLinesUnchanged) {
            return DiffRow(type, orgLine, newLine)
        }

        var wrapOrg = preprocessLine(orgLine)

        if (DiffRow.Tag.DELETE == type) {
            if (mergeOriginalRevised || showInlineDiffs) {
                wrapOrg = oldTag.generateOpen(type) + wrapOrg + oldTag.generateClose(type)
            }
        }

        var wrapNew = preprocessLine(newLine)

        if (DiffRow.Tag.INSERT == type) {
            if (mergeOriginalRevised) {
                wrapOrg = newTag.generateOpen(type) + wrapNew + newTag.generateClose(type)
            } else if (showInlineDiffs) {
                wrapNew = newTag.generateOpen(type) + wrapNew + newTag.generateClose(type)
            }
        }

        return DiffRow(type, wrapOrg, wrapNew)
    }

    private fun buildDiffRowWithoutNormalizing(type: DiffRow.Tag, oldLine: String, newLine: String): DiffRow =
        DiffRow(type, oldLine.wrapText(columnWidth), newLine.wrapText(columnWidth))

    /**
     * Add the inline diffs for given delta
     *
     * @param delta the given delta
     */
    @Suppress("LongMethod")
    private fun generateInlineDiffs(delta: Delta<String>): List<DiffRow> {
        val orig = normalizeLines(delta.source.lines)
        val rev = normalizeLines(delta.target.lines)
        val joinedOrig = orig.joinToString("\n")
        val joinedRev = rev.joinToString("\n")
        val origList = inlineDiffSplitter.split(joinedOrig)
        val revList = inlineDiffSplitter.split(joinedRev)

        val diff = diff(origList, revList, equalizer)
        val inlineDeltas = diff.deltas.reversed()

        for (inlineDelta in inlineDeltas) {
            val inlineOrig = inlineDelta.source
            val inlineRev = inlineDelta.target

            when (inlineDelta.type) {
                DeltaType.DELETE -> {
                    wrapInTag(
                        sequence = origList,
                        startPosition = inlineOrig.position,
                        endPosition = inlineOrig.position + inlineOrig.size(),
                        tag = DiffRow.Tag.DELETE,
                        tagGenerator = oldTag,
                        processDiffs = processDiffs,
                        replaceLinefeedWithSpace = replaceOriginalLinefeedInChangesWithSpaces && mergeOriginalRevised,
                    )
                }
                DeltaType.INSERT -> {
                    if (mergeOriginalRevised) {
                        origList.addAll(
                            inlineOrig.position,
                            revList.subList(inlineRev.position, inlineRev.position + inlineRev.size()),
                        )
                        wrapInTag(
                            sequence = origList,
                            startPosition = inlineOrig.position,
                            endPosition = inlineOrig.position + inlineRev.size(),
                            tag = DiffRow.Tag.INSERT,
                            tagGenerator = newTag,
                            processDiffs = processDiffs,
                            replaceLinefeedWithSpace = false,
                        )
                    } else {
                        wrapInTag(
                            sequence = revList,
                            startPosition = inlineRev.position,
                            endPosition = inlineRev.position + inlineRev.size(),
                            tag = DiffRow.Tag.INSERT,
                            tagGenerator = newTag,
                            processDiffs = processDiffs,
                            replaceLinefeedWithSpace = false,
                        )
                    }
                }
                DeltaType.CHANGE -> {
                    if (mergeOriginalRevised) {
                        origList.addAll(
                            inlineOrig.position + inlineOrig.size(),
                            revList.subList(inlineRev.position, inlineRev.position + inlineRev.size()),
                        )
                        wrapInTag(
                            sequence = origList,
                            startPosition = inlineOrig.position + inlineOrig.size(),
                            endPosition = inlineOrig.position + inlineOrig.size() + inlineRev.size(),
                            tag = DiffRow.Tag.CHANGE,
                            tagGenerator = newTag,
                            processDiffs = processDiffs,
                            replaceLinefeedWithSpace = false,
                        )
                    } else {
                        wrapInTag(
                            sequence = revList,
                            startPosition = inlineRev.position,
                            endPosition = inlineRev.position + inlineRev.size(),
                            tag = DiffRow.Tag.CHANGE,
                            tagGenerator = newTag,
                            processDiffs = processDiffs,
                            replaceLinefeedWithSpace = false,
                        )
                    }
                    wrapInTag(
                        sequence = origList,
                        startPosition = inlineOrig.position,
                        endPosition = inlineOrig.position + inlineOrig.size(),
                        tag = DiffRow.Tag.CHANGE,
                        tagGenerator = oldTag,
                        processDiffs = processDiffs,
                        replaceLinefeedWithSpace = replaceOriginalLinefeedInChangesWithSpaces && mergeOriginalRevised,
                    )
                }
                else -> error("Unexpected delta type ${inlineDelta.type}")
            }
        }

        val origResult = StringBuilder(origList.size)
        val revResult = StringBuilder(revList.size)

        for (character in origList) {
            origResult.append(character)
        }

        for (character in revList) {
            revResult.append(character)
        }

        // Note: dropLastWhile here arose from compatibility with Java: Java's `split` discard
        //  trailing empty string by default
        val original = origResult.split("\n").dropLastWhile(String::isEmpty)
        val revised = revResult.split("\n").dropLastWhile(String::isEmpty)

        val size = max(original.size, revised.size)
        val diffRows = ArrayList<DiffRow>(size)

        for (j in 0..<size) {
            diffRows.add(
                buildDiffRowWithoutNormalizing(
                    type = DiffRow.Tag.CHANGE,
                    oldLine = original.getOrNull(j) ?: "",
                    newLine = revised.getOrNull(j) ?: "",
                )
            )
        }

        return diffRows
    }

    private fun preprocessLine(line: String): String {
        val normalized = lineNormalizer.normalize(line)
        return if (columnWidth == 0) normalized else normalized.wrapText(columnWidth)
    }

    /**
     * Wrap the elements in the sequence with the given tag
     *
     * @param startPosition The position from which tag should start.
     *   The counting start from a zero.
     * @param endPosition The position before which tag should be closed.
     * @param tagGenerator The tag generator
     */
    @Suppress("LongParameterList", "ComplexMethod", "LoopWithTooManyJumpStatements", "NestedBlockDepth")
    private fun wrapInTag(
        sequence: MutableList<String>,
        startPosition: Int,
        endPosition: Int,
        tag: DiffRow.Tag,
        tagGenerator: DiffTagGenerator,
        processDiffs: DiffLineProcessor?,
        replaceLinefeedWithSpace: Boolean,
    ): List<String> {
        var endPos = endPosition

        while (endPos >= startPosition) {
            // Search position for end tag
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

            sequence.add(endPos, tagGenerator.generateClose(tag))

            if (processDiffs != null) {
                sequence[endPos - 1] = processDiffs.process(sequence[endPos - 1])
            }

            endPos--

            // Search position for end tag
            while (endPos > startPosition) {
                if ("\n" == sequence[endPos - 1]) {
                    if (replaceLinefeedWithSpace) {
                        sequence[endPos - 1] = " "
                    } else {
                        break
                    }
                }

                if (processDiffs != null) {
                    sequence[endPos - 1] = processDiffs.process(sequence[endPos - 1])
                }

                endPos--
            }

            sequence.add(endPos, tagGenerator.generateOpen(tag))
            endPos--
        }

        return sequence
    }
}
