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

/**
 * Holds the information about the part of text involved in the diff process
 *
 * Text is represented as `Object[]` because the diff engine is capable of handling more
 * than plain ascci. In fact, arrays or lists of any type that implements
 * [hashCode()][java.lang.Object.hashCode] and [equals()][java.lang.Object.equals]
 * correctly can be subject to differencing using this library.
 * @param T The type of the compared elements in the 'lines'.
) */
data class Chunk<T>(
    /**
     * the start position of chunk in the text
     */
    val position: Int,
    /**
     * the affected lines
     */
    val lines: List<T>,
    /**
     * the positions of changed lines of chunk in the text
     */
    val changePosition: List<Int>? = null
    ) {
    /**
     * Verifies that this chunk's saved text matches the corresponding text in the given sequence.
     *
     * @param target the sequence to verify against.
     * @throws com.github.difflib.patch.PatchFailedException
     */
    @Throws(PatchFailedException::class)
    fun verify(target: List<T>) {
        if (position > target.size || last() > target.size) {
            throw PatchFailedException("Incorrect Chunk: the position of chunk > target size")
        }
        for (i in 0 until size()) {
            if (target[position + i] != lines[i]) {
                throw PatchFailedException(
                    "Incorrect Chunk: the chunk content doesn't match the target"
                )
            }
        }
    }

    fun size() = lines.size

    /**
     * Returns the index of the last line of the chunk.
     */
    fun last(): Int {
        return position + size() - 1
    }

    override fun toString() = "[position: $position, size: ${size()}, lines: $lines]"
}
