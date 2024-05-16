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

import io.github.petertrr.diffutils.algorithm.myers.MyersDiffWithLinearSpace
import io.github.petertrr.diffutils.diff
import io.github.petertrr.diffutils.patch
import kotlin.test.Test
import kotlin.test.assertEquals

class PatchWithMyersDiffWithLinearSpaceTest {
    @Test
    fun testPatch_Change_withExceptionProcessor() {
        val changeTestFrom = mutableListOf("aaa", "bbb", "ccc", "ddd")
        val changeTestTo = mutableListOf("aaa", "bxb", "cxc", "ddd")
        val patch = diff(
            source = changeTestFrom,
            target = changeTestTo,
            algorithm = MyersDiffWithLinearSpace(),
        )

        changeTestFrom[2] = "CDC"
        patch.withConflictOutput(ConflictProducingConflictOutput())

        val data = patch(changeTestFrom, patch)
        assertEquals(11, data.size)
        assertEquals(
            mutableListOf(
                "aaa",
                "bxb",
                "cxc",
                "<<<<<< HEAD",
                "bbb",
                "CDC",
                "======",
                "bbb",
                "ccc",
                ">>>>>>> PATCH",
                "ddd",
            ),
            data,
        )
    }
}
