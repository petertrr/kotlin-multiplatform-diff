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
internal class MyersDiff<T>(private val equalizer: (T, T) -> Boolean = { t1, t2 -> t1 == t2 }) : DiffAlgorithm<T> {
    /**
     * Return empty diff if get the error while procession the difference.
     */
    override fun computeDiff(source: List<T>, target: List<T>, progress: DiffAlgorithmListener?): List<Change> {
        progress?.diffStart()
        val path = buildPath(source, target, progress)
        val result = buildRevision(path, source, target)
        progress?.diffEnd()
        return result
    }

    /**
     * Computes the minimum diffpath that expresses the differences between the original and revised
     * sequences, according to Gene Myers differencing algorithm.
     *
     * @param orig The original sequence.
     * @param rev The revised sequence.
     * @return A minimum [PathNode] across the differences graph.
     * @throws DifferentiationFailedException if a diff path could not be found.
     */
    private fun buildPath(orig: List<T>, rev: List<T>, progress: DiffAlgorithmListener?): PathNode? {
        // these are local constants
        val N = orig.size
        val M = rev.size
        val MAX = N + M + 1
        val size = 1 + 2 * MAX
        val middle = size / 2
        val diagonal: Array<PathNode?> = arrayOfNulls(size)
        diagonal[middle + 1] = PathNode(0, -1, snake = true, bootstrap = true, prev = null)
        for (d in 0 until MAX) {
            progress?.diffStep(d, MAX)
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
                diagonal[kminus] = null // no longer used
                var j = i - k
                var node = PathNode(i, j, false, false, prev)
                while (i < N && j < M && equalizer.invoke(orig[i], rev[j])) {
                    i++
                    j++
                }
                if (i != node.i) {
                    node = PathNode(i, j, true, false, node)
                }
                diagonal[kmiddle] = node
                if (i >= N && j >= M) {
                    return diagonal[kmiddle]
                }
                k += 2
            }
            diagonal[middle + d - 1] = null
        }
        throw IllegalStateException("could not find a diff path")
    }

    /**
     * Constructs a [Patch] from a difference path.
     *
     * @param actualPath The path.
     * @param orig The original sequence.
     * @param rev The revised sequence.
     * @return A [Patch] script corresponding to the path.
     * @throws DifferentiationFailedException if a [Patch] could not be built from the given
     * path.
     */
    private fun buildRevision(actualPath: PathNode?, orig: List<T>, rev: List<T>): List<Change> {
        var path: PathNode? = actualPath
        val changes: MutableList<Change> = mutableListOf()
        if (path!!.snake) {
            path = path.prev
        }
        while (path?.prev != null && path.prev!!.j >= 0) {
            check(!path.snake) { "bad diffpath: found snake when looking for diff" }
            val i: Int = path.i
            val j: Int = path.j
            path = path.prev
            val ianchor: Int = path!!.i
            val janchor: Int = path.j
            if (ianchor == i && janchor != j) {
                changes.add(Change(DeltaType.INSERT, ianchor, i, janchor, j))
            } else if (ianchor != i && janchor == j) {
                changes.add(Change(DeltaType.DELETE, ianchor, i, janchor, j))
            } else {
                changes.add(Change(DeltaType.CHANGE, ianchor, i, janchor, j))
            }
            if (path.snake) {
                path = path.prev
            }
        }
        return changes
    }
}
