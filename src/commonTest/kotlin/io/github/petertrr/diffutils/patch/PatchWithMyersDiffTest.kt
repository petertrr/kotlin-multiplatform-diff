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
package io.github.petertrr.diffutils.patch

import io.github.petertrr.diffutils.diff
import io.github.petertrr.diffutils.patch
import kotlin.test.Test
import kotlin.test.assertEquals

class PatchWithMyersDiffTest {
    @Test
    fun testPatch_Insert() {
        val insertTestFrom = listOf("hhh")
        val insertTestTo = listOf("hhh", "jjj", "kkk", "lll")
        val patch = diff(insertTestFrom, insertTestTo)
        assertEquals(insertTestTo, patch(insertTestFrom, patch))
    }

    @Test
    fun testPatch_Delete() {
        val deleteTestFrom = listOf("ddd", "fff", "ggg", "hhh")
        val deleteTestTo = listOf("ggg")
        val patch = diff(deleteTestFrom, deleteTestTo)
        assertEquals(deleteTestTo, patch(deleteTestFrom, patch))
    }

    @Test
    fun testPatch_Change() {
        val changeTestFrom = listOf("aaa", "bbb", "ccc", "ddd")
        val changeTestTo = listOf("aaa", "bxb", "cxc", "ddd")
        val patch = diff(changeTestFrom, changeTestTo)
        assertEquals(changeTestTo, patch(changeTestFrom, patch))
    }

    @Test
    fun testPatch_Change_withExceptionProcessor() {
        val changeTestFrom = mutableListOf("aaa", "bbb", "ccc", "ddd")
        val changeTestTo = listOf("aaa", "bxb", "cxc", "ddd")
        val patch = diff(changeTestFrom, changeTestTo)

        changeTestFrom[2] = "CDC"
        patch.withConflictOutput(ConflictProducingConflictOutput())

        val data = patch(changeTestFrom, patch)
        assertEquals(9, data.size)
        assertEquals(
            mutableListOf(
                "aaa",
                "<<<<<< HEAD",
                "bbb",
                "CDC",
                "======",
                "bbb",
                "ccc",
                ">>>>>>> PATCH",
                "ddd"
            ),
            data
        )
    }

    @Test
    fun testPatchThreeWayIssue138() {
        val base = "Imagine there's no heaven".split("\\s+".toRegex())
        val left = "Imagine there's no HEAVEN".split("\\s+".toRegex())
        val right = "IMAGINE there's no heaven".split("\\s+".toRegex())

        val rightPatch = diff(base, right)
        rightPatch.withConflictOutput(ConflictProducingConflictOutput())

        val applied = rightPatch.applyTo(left)
        assertEquals("IMAGINE there's no HEAVEN", applied.joinToString(separator = " "))
    }
}
