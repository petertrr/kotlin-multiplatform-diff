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

import kotlin.jvm.JvmField

/**
 * A node in a diffpath.
 *
 * @param i Position in the original sequence
 * @param j Position in the revised sequence
 * @param snake
 * @param bootstrap Is this a bootstrap node?
 *   In bootstrap nodes one of the two coordinates is less than zero.
 * @param prev The previous node in the path, if any
 */
internal class PathNode(
    @JvmField val i: Int,
    @JvmField val j: Int,
    @JvmField val snake: Boolean,
    @JvmField val bootstrap: Boolean,
    prev: PathNode? = null,
) {
    /**
     * The previous node in the path.
     */
    @JvmField
    val prev: PathNode? = if (snake) prev else prev?.previousSnake()

    /**
     * Skips sequences of [PathNodes][PathNode] until a snake or bootstrap node is found, or the end of the
     * path is reached.
     *
     * @return The next first [PathNode] or bootstrap node in the path, or `null` if none found
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun previousSnake(): PathNode? {
        if (bootstrap) {
            return null
        }

        return if (!snake && prev != null) prev.previousSnake() else this
    }

    override fun toString(): String {
        val buf = StringBuilder("[")
        var node: PathNode? = this

        while (node != null) {
            buf.append("(")
            buf.append(node.i)
            buf.append(",")
            buf.append(node.j)
            buf.append(")")
            node = node.prev
        }

        buf.append("]")
        return buf.toString()
    }
}
