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
package io.github.petertrr.diffutils.patch

import io.github.petertrr.diffutils.algorithm.Change

/**
 * Describes the patch holding all deltas between the original and revised texts.
 *
 * @param T The type of the compared elements in the 'lines'.
 */
public class Patch<T> {
    public var deltas: MutableList<Delta<T>> = arrayListOf()
        get() {
            field.sortBy { it.source.position }
            return field
        }

    /**
     * Apply this patch to the given target
     *
     * @return the patched text
     */
    @Throws(PatchFailedException::class)
    public fun applyTo(target: List<T>): List<T> {
        val result = target.toMutableList()
        val it = deltas.listIterator(deltas.size)
        while (it.hasPrevious()) {
            val delta = it.previous()
            delta.verifyAndApplyTo(result)
        }
        return result
    }

    /**
     * Restore the text to original. Opposite to applyTo() method.
     *
     * @param target the given target
     * @return the restored text
     */
    public fun restore(target: List<T>): List<T> {
        val result = target.toMutableList()
        val it = deltas.listIterator(deltas.size)
        while (it.hasPrevious()) {
            val delta = it.previous()
            delta.restore(result)
        }
        return result
    }

    /**
     * Add the given delta to this patch
     *
     * @param delta the given delta
     */
    public fun addDelta(delta: Delta<T>): Boolean = deltas.add(delta)

    override fun toString(): String {
        return "Patch{deltas=$deltas}"
    }

    public companion object {
        public fun <T> generate(original: List<T>, revised: List<T>, changes: List<Change>): Patch<T> {
            return generate(original, revised, changes, false)
        }

        private fun <T> buildChunk(start: Int, end: Int, data: List<T>): Chunk<T> {
            return Chunk(start, data.subList(start, end))
        }

        public fun <T> generate(original: List<T>, revised: List<T>, changes: List<Change>, includeEquals: Boolean): Patch<T> {
            val patch = Patch<T>()
            var startOriginal = 0
            var startRevised = 0
            changes.run {
                if (includeEquals) sortedBy { it.startOriginal } else this
            }.forEach { change ->
                if (includeEquals && startOriginal < change.startOriginal) {
                    patch.addDelta(
                        EqualDelta(
                            buildChunk(startOriginal, change.startOriginal, original),
                            buildChunk(startRevised, change.startRevised, revised)
                        )
                    )
                }
                val orgChunk = buildChunk(change.startOriginal, change.endOriginal, original)
                val revChunk = buildChunk(change.startRevised, change.endRevised, revised)
                when (change.deltaType) {
                    DeltaType.DELETE -> patch.addDelta(DeleteDelta(orgChunk, revChunk))
                    DeltaType.INSERT -> patch.addDelta(InsertDelta(orgChunk, revChunk))
                    DeltaType.CHANGE -> patch.addDelta(ChangeDelta(orgChunk, revChunk))
                    else -> {
                    }
                }
                startOriginal = change.endOriginal
                startRevised = change.endRevised
            }
            if (includeEquals && startOriginal < original.size) {
                patch.addDelta(
                    EqualDelta(
                        buildChunk(startOriginal, original.size, original),
                        buildChunk(startRevised, revised.size, revised)
                    )
                )
            }
            return patch
        }
    }
}
