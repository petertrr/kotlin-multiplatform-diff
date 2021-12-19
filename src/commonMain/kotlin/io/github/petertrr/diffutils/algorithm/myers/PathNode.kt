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

/**
 * A node in a diffpath.
 */
internal class PathNode(
    /**
     * Position in the original sequence.
     */
    val i: Int,
    /**
     * Position in the revised sequence.
     */
    val j: Int,
    val snake: Boolean,
    /**
     * Is this a bootstrap node?
     * In bootstrap nodes one of the two coordinates is less than zero.
     */
    val bootstrap: Boolean,
    prev: PathNode? = null
) {
    /**
     * The previous node in the path.
     */
    val prev: PathNode?

    init {
        if (snake) {
            this.prev = prev
        } else {
            this.prev = prev?.previousSnake()
        }
    }

    /**
     * Skips sequences of [PathNodes][PathNode] until a snake or bootstrap node is found, or the end of the
     * path is reached.
     *
     * @return The next first [PathNode] or bootstrap node in the path, or `null` if none found.
     */
    fun previousSnake(): PathNode? {
        if (bootstrap) {
            return null
        }
        return if (!snake && prev != null) {
            prev.previousSnake()
        } else {
            this
        }
    }

    override fun toString() = generateSequence(this) { it.prev }
        .joinToString(prefix = "[", postfix = "]") { "(${it.i}, ${it.j})" }
}
