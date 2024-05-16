/*
 * Copyright 2024 Peter Trifanov.
 * Copyright 2021 java-diff-utils.
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

import io.github.petertrr.diffutils.algorithm.DiffAlgorithmListener
import io.github.petertrr.diffutils.diff
import io.github.petertrr.diffutils.patch.Patch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.measureTimedValue

class MyersDiffWithLinearSpaceTest {
    @Test
    fun testDiffMyersExample1Forward() {
        val original = listOf("A", "B", "C", "A", "B", "B", "A")
        val revised = listOf("C", "B", "A", "B", "A", "C")
        val changes = MyersDiffWithLinearSpace<String>().computeDiff(original, revised)
        val patch = Patch.generate(original, revised, changes)
        println(patch)

        assertEquals(5, patch.deltas.size)
        assertEquals(
            "Patch{deltas=[" +
                    "[InsertDelta, position: 0, lines: [C]], " +
                    "[DeleteDelta, position: 0, lines: [A]], " +
                    "[DeleteDelta, position: 2, lines: [C]], " +
                    "[DeleteDelta, position: 5, lines: [B]], " +
                    "[InsertDelta, position: 7, lines: [C]]" +
                    "]}",
            patch.toString(),
        )
    }

    @Test
    fun testDiffMyersExample1ForwardWithListener() {
        val original = listOf("A", "B", "C", "A", "B", "B", "A")
        val revised = listOf("C", "B", "A", "B", "A", "C")
        val logData = ArrayList<String>()
        val progress = object : DiffAlgorithmListener {
            override fun diffStart() {
                logData.add("start")
            }

            override fun diffStep(value: Int, max: Int) {
                logData.add("$value - $max")
            }

            override fun diffEnd() {
                logData.add("end")
            }
        }

        val patch = Patch.generate(
            original = original,
            revised = revised,
            changes = MyersDiffWithLinearSpace<String>().computeDiff(original, revised, progress),
        )

        println(patch)

        assertEquals(5, patch.deltas.size)
        assertEquals(
            "Patch{deltas=[" +
                    "[InsertDelta, position: 0, lines: [C]], " +
                    "[DeleteDelta, position: 0, lines: [A]], " +
                    "[DeleteDelta, position: 2, lines: [C]], " +
                    "[DeleteDelta, position: 5, lines: [B]], " +
                    "[InsertDelta, position: 7, lines: [C]]" +
                    "]}",
            patch.toString(),
        )

        println(logData)
        assertEquals(11, logData.size)
    }

    @Test
    fun testPerformanceProblemsIssue124() {
        val old = listOf("abcd")
        val new = (0..<90000)
            .map(Int::toString)
            .toList()

        val (patch, duration) = measureTimedValue {
            diff(old, new, MyersDiffWithLinearSpace())
        }

        println("Finished in ${duration.inWholeMilliseconds}ms and resulted ${patch.deltas.size} deltas")
    }
}
