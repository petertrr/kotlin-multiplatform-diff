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
package io.github.petertrr.diffutils.algorithm.myers

import io.github.petertrr.diffutils.algorithm.Change
import io.github.petertrr.diffutils.algorithm.DiffAlgorithm
import io.github.petertrr.diffutils.algorithm.DiffAlgorithmListener
import io.github.petertrr.diffutils.patch.DeltaType

/**
 * A clean-room implementation of Eugene Myers greedy differencing algorithm.
 */
public class MyersDiff<T>(private val equalizer: (T, T) -> Boolean = { t1, t2 -> t1 == t2 }) : DiffAlgorithm<T> {
    /**
     * Returns an empty diff if we get an error while procession the difference.
     */
    override fun computeDiff(source: List<T>, target: List<T>, progress: DiffAlgorithmListener): List<Change> {
        progress.diffStart()

        val path = buildPath(source, target, progress) ?: error("Expected a non-null path node")
        val result = buildRevision(path)
        progress.diffEnd()

        return result
    }

    /**
     * Computes the minimum diffpath that expresses the differences between the original and revised
     * sequences, according to Eugene Myers differencing algorithm.
     *
     * @param orig The original sequence
     * @param rev The revised sequence
     * @return A minimum [PathNode] across the differences graph
     * @throws IllegalStateException If a diff path could not be found
     */
    private fun buildPath(orig: List<T>, rev: List<T>, progress: DiffAlgorithmListener): PathNode? {
        // These are local constants
        val origSize = orig.size
        val revSize = rev.size
        val max = origSize + revSize + 1
        val size = 1 + 2 * max
        val middle = size / 2
        val diagonal: Array<PathNode?> = arrayOfNulls(size)
        diagonal[middle + 1] = PathNode(0, -1, snake = true, bootstrap = true, prev = null)

        for (d in 0..<max) {
            progress.diffStep(d, max)
            var k = -d

            while (k <= d) {
                val kmiddle = middle + k
                val kplus = kmiddle + 1
                val kminus = kmiddle - 1
                var prev: PathNode?
                var i: Int

                if (k == -d || k != d && diagonal[kminus]!!.i < diagonal[kplus]!!.i) {
                    i = diagonal[kplus]!!.i
                    prev = diagonal[kplus]
                } else {
                    i = diagonal[kminus]!!.i + 1
                    prev = diagonal[kminus]
                }

                diagonal[kminus] = null // No longer used
                var j = i - k
                var node = PathNode(i, j, snake = false, bootstrap = false, prev = prev)

                while (i < origSize && j < revSize && equalizer.invoke(orig[i], rev[j])) {
                    i++
                    j++
                }

                if (i != node.i) {
                    node = PathNode(i, j, snake = true, bootstrap = false, prev = node)
                }

                diagonal[kmiddle] = node

                if (i >= origSize && j >= revSize) {
                    return diagonal[kmiddle]
                }

                k += 2
            }

            diagonal[middle + d - 1] = null
        }

        error("Could not find a diff path")
    }

    /**
     * Constructs a patch from a difference path.
     *
     * @param actualPath The path
     * @return A list of [Change]s corresponding to the path
     * @throws IllegalStateException If a patch could not be built from the given path
     */
    private fun buildRevision(actualPath: PathNode): List<Change> {
        var path = if (actualPath.snake) {
            actualPath.prev
        } else {
            actualPath
        }

        val changes = ArrayList<Change>()

        // This can be improved to avoid the non-null assertion on prev
        while (path?.prev != null && path.prev!!.j >= 0) {
            check(!path.snake) { "Bad diffpath: found snake when looking for diff" }

            val i = path.i
            val j = path.j
            path = path.prev ?: error("Expected a non-null previous path node")

            val iAnchor = path.i
            val jAnchor = path.j

            if (iAnchor == i && jAnchor != j) {
                changes.add(Change(DeltaType.INSERT, iAnchor, i, jAnchor, j))
            } else if (iAnchor != i && jAnchor == j) {
                changes.add(Change(DeltaType.DELETE, iAnchor, i, jAnchor, j))
            } else {
                changes.add(Change(DeltaType.CHANGE, iAnchor, i, jAnchor, j))
            }

            if (path.snake) {
                path = path.prev
            }
        }

        return changes
    }
}
