/*
 * Copyright 2021 Peter Trifanov.
 * Copyright 2017 java-diff-utils.
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
import io.github.petertrr.diffutils.patch.Patch
import io.github.petertrr.diffutils.utils.deleteDeltaOf
import io.github.petertrr.diffutils.utils.insertDeltaOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MyersDiffTest {
    @Test
    fun testDiffMyersExample1Forward() {
        val original: List<String> = listOf("A", "B", "C", "A", "B", "B", "A")
        val revised: List<String> = listOf("C", "B", "A", "B", "A", "C")
        val patch: Patch<String> = Patch.generate(original, revised, MyersDiff<String>().computeDiff(original, revised))
        assertNotNull(patch)
        assertEquals(4, patch.deltas.size)
        assertEquals(
            "Patch{deltas=[${deleteDeltaOf(0, listOf("A", "B"))}, ${insertDeltaOf(3, 1, listOf("B"))}, ${deleteDeltaOf(5, listOf("B"), 4)}, ${insertDeltaOf(7, 5, listOf("C"))}]}",
            patch.toString()
        )
    }

    @Test
    fun testDiffMyersExample1ForwardWithListener() {
        val original: List<String> = listOf("A", "B", "C", "A", "B", "B", "A")
        val revised: List<String> = listOf("C", "B", "A", "B", "A", "C")
        val logdata: MutableList<String> = ArrayList()
        val patch: Patch<String> = Patch.generate(original, revised,
            MyersDiff<String>().computeDiff(original, revised, object : DiffAlgorithmListener {
                override fun diffStart() {
                    logdata.add("start")
                }

                override fun diffStep(value: Int, max: Int) {
                    logdata.add("$value - $max")
                }

                override fun diffEnd() {
                    logdata.add("end")
                }
            })
        )
        assertNotNull(patch)
        assertEquals(4, patch.deltas.size)
        assertEquals(
            "Patch{deltas=[${deleteDeltaOf(0, listOf("A", "B"))}, ${insertDeltaOf(3, 1, listOf("B"))}, " +
                    "${deleteDeltaOf(5, listOf("B"), 4)}, ${insertDeltaOf(7, 5, listOf("C"))}]}",
            patch.toString()
        )
        println(logdata)
        assertEquals(8, logdata.size)
    }
}
