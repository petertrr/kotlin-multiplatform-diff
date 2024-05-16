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
@file:JvmName("DiffUtils")

package io.github.petertrr.diffutils

import io.github.petertrr.diffutils.algorithm.DiffAlgorithm
import io.github.petertrr.diffutils.algorithm.DiffAlgorithmListener
import io.github.petertrr.diffutils.algorithm.DiffEqualizer
import io.github.petertrr.diffutils.algorithm.NoopAlgorithmListener
import io.github.petertrr.diffutils.algorithm.myers.MyersDiff
import io.github.petertrr.diffutils.patch.Patch
import io.github.petertrr.diffutils.patch.PatchFailedException
import io.github.petertrr.diffutils.text.DiffRowGenerator
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads

// Instead of asking consumers to normalize their line endings, we simply catch them all.
private val lineBreak = Regex("\r\n|\r|\n")

/**
 * Computes the difference between the source and target text.
 *
 * By default, uses the Myers algorithm.
 *
 * @param sourceText The original text
 * @param targetText The target text
 * @param algorithm The diff algorithm to use
 * @param progress The diff algorithm progress listener
 * @param includeEqualParts Whether to include equal data parts into the patch. `false` by default.
 * @return The patch describing the difference between the original and target text
 */
@JvmOverloads
public fun diff(
    sourceText: String,
    targetText: String,
    algorithm: DiffAlgorithm<String> = MyersDiff(),
    progress: DiffAlgorithmListener = NoopAlgorithmListener(),
    includeEqualParts: Boolean = false,
): Patch<String> =
    diff(
        source = sourceText.split(lineBreak),
        target = targetText.split(lineBreak),
        algorithm = algorithm,
        progress = progress,
        includeEqualParts = includeEqualParts,
    )

/**
 * Computes the difference between the source and target list of elements using the Myers algorithm.
 *
 * @param source The original elements
 * @param target The target elements
 * @param equalizer The equalizer to replace the default compare algorithm [Any.equals].
 *   If `null`, the default equalizer of the default algorithm is used.
 * @return The patch describing the difference between the source and target sequences
 */
public fun <T> diff(
    source: List<T>,
    target: List<T>,
    equalizer: DiffEqualizer<T>,
): Patch<T> =
    diff(
        source = source,
        target = target,
        algorithm = MyersDiff(equalizer),
    )

/**
 * Computes the difference between the original and target list of elements.
 *
 * By default, uses the Meyers algorithm.
 *
 * @param source The original elements
 * @param target The target elements
 * @param algorithm The diff algorithm to use
 * @param progress The diff algorithm progress listener
 * @param includeEqualParts Whether to include equal data parts into the patch. `false` by default.
 * @return The patch describing the difference between the original and target sequences
 */
@JvmOverloads
public fun <T> diff(
    source: List<T>,
    target: List<T>,
    algorithm: DiffAlgorithm<T> = MyersDiff(),
    progress: DiffAlgorithmListener = NoopAlgorithmListener(),
    includeEqualParts: Boolean = false,
): Patch<T> =
    Patch.generate(
        original = source,
        revised = target,
        changes = algorithm.computeDiff(source, target, progress),
        includeEquals = includeEqualParts,
    )

/**
 * Computes the difference between the given texts inline.
 *
 * This one uses the "trick" to make out of texts lists of characters,
 * like [DiffRowGenerator] does and merges those changes at the end together again.
 */
public fun diffInline(original: String, revised: String): Patch<String> {
    val origChars = original.toCharArray()
    val origList = ArrayList<String>(origChars.size)

    val revChars = revised.toCharArray()
    val revList = ArrayList<String>(revChars.size)

    for (character in origChars) {
        origList.add(character.toString())
    }

    for (character in revChars) {
        revList.add(character.toString())
    }

    val patch = diff(origList, revList)
    patch.deltas = patch.deltas.mapTo(ArrayList(patch.deltas.size)) {
        it.withChunks(
            it.source.copy(lines = compressLines(it.source.lines, "")),
            it.target.copy(lines = compressLines(it.target.lines, "")),
        )
    }

    return patch
}

/**
 * Patch the original text with the given patch.
 *
 * @param original The original text
 * @param patch The patch to apply
 * @return The revised text
 * @throws PatchFailedException If the patch cannot be applied
 */
public fun <T> patch(original: List<T>, patch: Patch<T>): List<T> =
    patch.applyTo(original)

/**
 * Unpatch the revised text for a given patch
 *
 * @param revised The revised text
 * @param patch The given patch
 * @return The original text
 */
public fun <T> unpatch(revised: List<T>, patch: Patch<T>): List<T> =
    patch.restore(revised)

private fun compressLines(lines: List<String>, delimiter: String): List<String> =
    if (lines.isEmpty()) {
        emptyList()
    } else {
        listOf(lines.joinToString(delimiter))
    }
