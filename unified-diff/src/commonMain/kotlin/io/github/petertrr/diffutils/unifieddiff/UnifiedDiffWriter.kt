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

import io.github.petertrr.diffutils.patch.Delta
import okio.BufferedSink
import okio.IOException

public object UnifiedDiffWriter {
//    private val LOG: Logger = Logger.getLogger(UnifiedDiffWriter::class.java.getName())
    @Throws(IOException::class)
    public fun write(
        diff: UnifiedDiff,
        originalLinesProvider: (String) -> List<String>,
        writer: BufferedSink,
        contextSize: Int
    ) {
        write(diff, originalLinesProvider, contextSize) { line: String? ->
            try {
                writer.write("$line\n".encodeToByteArray())
            } catch (ex: IOException) {
//                LOG.log(Level.SEVERE, null, ex)
            }
        }
    }

    @Throws(IOException::class)
    public fun write(
        diff: UnifiedDiff,
        originalLinesProvider: (String) -> List<String>,
        contextSize: Int,
        writer: (String) -> Unit,
    ) {
        if (diff.header != null) {
            writer(diff.header!!)
        }
        for (file in diff.getFiles()) {
            val patchDeltas: List<Delta<String>> = file.patch.deltas
            if (!patchDeltas.isEmpty()) {
                writeOrNothing(writer, file.diffCommand)
                if (file.index != null) {
                    writer("index " + file.index)
                }
                writer("--- " + if (file.fromFile == null) "/dev/null" else file.fromFile)
                if (file.toFile != null) {
                    writer("+++ " + file.toFile)
                }
                val originalLines: List<String> = originalLinesProvider(file.fromFile!!)
                val deltas: MutableList<Delta<String>> = mutableListOf()
                var delta: Delta<String> = patchDeltas[0]
                deltas.add(delta) // add the first Delta to the current set
                // if there's more than 1 Delta, we may need to output them together
                if (patchDeltas.size > 1) {
                    for (i in 1 until patchDeltas.size) {
                        val position: Int = delta.source.position

                        // Check if the next Delta is too close to the current
                        // position.
                        // And if it is, add it to the current set
                        val nextDelta: Delta<String> = patchDeltas[i]
                        if (position + delta.source.size() + contextSize >= nextDelta
                                .source.position - contextSize
                        ) {
                            deltas.add(nextDelta)
                        } else {
                            // if it isn't, output the current set,
                            // then create a new set and add the current Delta to
                            // it.
                            processDeltas(writer, originalLines, deltas, contextSize, false)
                            deltas.clear()
                            deltas.add(nextDelta)
                        }
                        delta = nextDelta
                    }
                }
                // don't forget to process the last set of Deltas
                processDeltas(
                    writer, originalLines, deltas, contextSize,
                    patchDeltas.size == 1 && file.fromFile == null
                )
            }
        }
        if (diff.tail != null) {
            writer("--")
            writer(diff.tail!!)
        }
    }

    private fun processDeltas(
        writer: (String) -> Unit,
        origLines: List<String>, deltas: List<Delta<String>>,
        contextSize: Int, newFile: Boolean
    ) {
        val buffer: MutableList<String> = mutableListOf<String>()
        var origTotal = 0 // counter for total lines output from Original
        var revTotal = 0 // counter for total lines output from Original
        var line: Int
        var curDelta: Delta<String> = deltas[0]
        var origStart: Int
        if (newFile) {
            origStart = 0
        } else {
            // NOTE: +1 to overcome the 0-offset Position
            origStart = curDelta.source.position + 1 - contextSize
            if (origStart < 1) {
                origStart = 1
            }
        }
        var revStart: Int = curDelta.target.position + 1 - contextSize
        if (revStart < 1) {
            revStart = 1
        }

        // find the start of the wrapper context code
        var contextStart: Int = curDelta.source.position - contextSize
        if (contextStart < 0) {
            contextStart = 0 // clamp to the start of the file
        }

        // output the context before the first Delta
        line = contextStart
        while (line < curDelta.source.position
            && line < origLines.size
        ) {
            //
            buffer.add(" " + origLines[line])
            origTotal++
            revTotal++
            line++
        }
        // output the first Delta
        getDeltaText({ txt: String -> buffer.add(txt) }, curDelta)
        origTotal += curDelta.source.lines.size
        revTotal += curDelta.target.lines.size
        var deltaIndex = 1
        while (deltaIndex < deltas.size) { // for each of the other Deltas
            val nextDelta: Delta<String> = deltas[deltaIndex]
            val intermediateStart: Int = (curDelta.source.position
                    + curDelta.source.lines.size)
            line = intermediateStart
            while (line < nextDelta.source.position
                && line < origLines.size
            ) {

                // output the code between the last Delta and this one
                buffer.add(" " + origLines[line])
                origTotal++
                revTotal++
                line++
            }
            getDeltaText({ txt ->
                buffer.add(
                    txt
                )
            }, nextDelta) // output the Delta
            origTotal += nextDelta.target.lines.size
            revTotal += nextDelta.target.lines.size
            curDelta = nextDelta
            deltaIndex++
        }

        // Now output the post-Delta context code, clamping the end of the file
        contextStart = (curDelta.source.position
                + curDelta.source.lines.size)
        line = contextStart
        while (line < contextStart + contextSize
            && line < origLines.size
        ) {
            buffer.add(" " + origLines[line])
            origTotal++
            revTotal++
            line++
        }

        // Create and insert the block header, conforming to the Unified Diff
        // standard
        writer("@@ -$origStart,$origTotal +$revStart,$revTotal @@")
        buffer.forEach { txt: String -> writer(txt) }
    }

    /**
     * getDeltaText returns the lines to be added to the Unified Diff text from the Delta parameter.
     *
     * @param writer consumer for the list of String lines of code
     * @param delta the Delta to output
     */
    private fun getDeltaText(writer: (String) -> Unit, delta: Delta<String>) {
        for (line in delta.source.lines) {
            writer("-$line")
        }
        for (line in delta.target.lines) {
            writer("+$line")
        }
    }

    @Throws(IOException::class)
    private fun writeOrNothing(writer: (String) -> Unit, str: String?) {
        if (str != null) {
            writer(str)
        }
    }
}