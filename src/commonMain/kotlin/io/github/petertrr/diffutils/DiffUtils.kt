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
/**
 * Implements the difference and patching engine
 */

package io.github.petertrr.diffutils

import io.github.petertrr.diffutils.algorithm.DiffAlgorithm
import io.github.petertrr.diffutils.algorithm.DiffAlgorithmListener
import io.github.petertrr.diffutils.algorithm.myers.MyersDiff
import io.github.petertrr.diffutils.patch.Patch
import io.github.petertrr.diffutils.patch.PatchFailedException

/**
 * Computes the difference between the original and revised list of elements with default diff
 * algorithm
 *
 * @param T types to be diffed
 * @param original The original text.
 * @param revised The revised text.
 * @param progress progress listener
 * @return The patch describing the difference between the original and revised sequences.
 */
public fun <T> diff(original: List<T>, revised: List<T>, progress: DiffAlgorithmListener?): Patch<T> {
    return diff(original, revised, MyersDiff(), progress)
}

public fun <T> diff(original: List<T>, revised: List<T>): Patch<T> {
    return diff(original, revised, MyersDiff(), null)
}

public fun <T> diff(original: List<T>, revised: List<T>, includeEqualParts: Boolean): Patch<T> {
    return diff(original, revised, MyersDiff(), null, includeEqualParts)
}

/**
 * Computes the difference between the original and revised text.
 */
public fun diff(sourceText: String, targetText: String,
         progress: DiffAlgorithmListener?): Patch<String> {
    return diff(sourceText.split("\n"),
        targetText.split("\n"),
        progress
    )
}

/**
 * Computes the difference between the original and revised list of elements with default diff
 * algorithm
 *
 * @param source The original text.
 * @param target The revised text.
 *
 * @param equalizer the equalizer object to replace the default compare algorithm
 * (Object.equals). If `null` the default equalizer of the default algorithm is used..
 * @return The patch describing the difference between the original and revised sequences.
 */
public fun <T> diff(
    source: List<T>, target: List<T>,
    equalizer: ((T, T) -> Boolean)?
): Patch<T> {
    return if (equalizer != null) {
        diff(
            source, target,
            MyersDiff(equalizer)
        )
    } else diff(source, target, MyersDiff())
}

public fun <T> diff(
    original: List<T>, revised: List<T>,
    algorithm: DiffAlgorithm<T>, progress: DiffAlgorithmListener?
): Patch<T> {
    return diff(original, revised, algorithm, progress, false)
}

/**
 * Computes the difference between the original and revised list of elements with default diff
 * algorithm
 *
 * @param original The original text. Must not be `null`.
 * @param revised The revised text. Must not be `null`.
 * @param algorithm The diff algorithm. Must not be `null`.
 * @param progress The diff algorithm listener.
 * @param includeEqualParts Include equal data parts into the patch.
 * @return The patch describing the difference between the original and revised sequences. Never
 * `null`.
 */
public fun <T> diff(
    original: List<T>, revised: List<T>,
    algorithm: DiffAlgorithm<T>, progress: DiffAlgorithmListener?,
    includeEqualParts: Boolean
): Patch<T> {
    return Patch.generate(original, revised, algorithm.computeDiff(original, revised, progress), includeEqualParts)
}

/**
 * Computes the difference between the original and revised list of elements with default diff
 * algorithm
 *
 * @param original The original text. Must not be `null`.
 * @param revised The revised text. Must not be `null`.
 * @param algorithm The diff algorithm. Must not be `null`.
 * @return The patch describing the difference between the original and revised sequences. Never
 * `null`.
 */
public fun <T> diff(original: List<T>, revised: List<T>, algorithm: DiffAlgorithm<T>): Patch<T> {
    return diff(original, revised, algorithm, null)
}

/**
 * Computes the difference between the given texts inline. This one uses the "trick" to make out
 * of texts lists of characters, like DiffRowGenerator does and merges those changes at the end
 * together again.
 *
 * @param original
 * @param revised
 * @return
 */
public fun diffInline(original: String, revised: String): Patch<String> {
    val origList: MutableList<String> = arrayListOf()
    val revList: MutableList<String> = arrayListOf()
    for (character in original.toCharArray()) {
        origList.add(character.toString())
    }
    for (character in revised.toCharArray()) {
        revList.add(character.toString())
    }
    val patch: Patch<String> = diff(origList, revList)
    patch.deltas.map { delta ->
        delta.withChunks(
            delta.source.copy(lines = compressLines(delta.source.lines, "")),
            delta.target.copy(lines = compressLines(delta.target.lines, ""))
        )
    }
        .let { patch.deltas = it.toMutableList() }
    return patch
}

private fun compressLines(lines: List<String>, delimiter: String): List<String> {
    return if (lines.isEmpty()) {
        emptyList()
    } else listOf(lines.joinToString(delimiter))
}

/**
 * Patch the original text with given patch
 *
 * @param original the original text
 * @param patch the given patch
 * @return the revised text
 * @throws PatchFailedException if can't apply patch
 */
@Throws(PatchFailedException::class)
public fun <T> patch(original: List<T>, patch: Patch<T>): List<T> {
    return patch.applyTo(original)
}

/**
 * Unpatch the revised text for a given patch
 *
 * @param revised the revised text
 * @param patch the given patch
 * @return the original text
 */
@Suppress("UNUSED")
public fun <T> unpatch(revised: List<T>, patch: Patch<T>): List<T> {
    return patch.restore(revised)
}
