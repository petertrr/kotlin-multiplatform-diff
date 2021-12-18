/*
 * Copyright 2021-2021 Peter Trifanov.
 * Copyright 2009-2020 java-diff-utils.
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
import kotlin.test.fail

class PatchTest {
    @Test
    fun testPatch_Insert() {
        val insertTest_from: List<String> = listOf("hhh")
        val insertTest_to: List<String> = listOf("hhh", "jjj", "kkk", "lll")
        val patch: Patch<String> = diff(insertTest_from, insertTest_to)
        try {
            assertEquals(insertTest_to, patch(insertTest_from, patch))
        } catch (e: PatchFailedException) {
            fail(e.message)
        }
    }

    @Test
    fun testPatch_Delete() {
        val deleteTest_from: List<String> = listOf("ddd", "fff", "ggg", "hhh")
        val deleteTest_to: List<String> = listOf("ggg")
        val patch: Patch<String> = diff(deleteTest_from, deleteTest_to)
        try {
            assertEquals(deleteTest_to, patch(deleteTest_from, patch))
        } catch (e: PatchFailedException) {
            fail(e.message)
        }
    }

    @Test
    fun testPatch_Change() {
        val changeTest_from: List<String> = listOf("aaa", "bbb", "ccc", "ddd")
        val changeTest_to: List<String> = listOf("aaa", "bxb", "cxc", "ddd")
        val patch: Patch<String> = diff(changeTest_from, changeTest_to)
        try {
            assertEquals(changeTest_to, patch(changeTest_from, patch))
        } catch (e: PatchFailedException) {
            fail(e.message)
        }
    }
}
