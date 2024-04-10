/*
 * Copyright 2021 Peter Trifanov.
 * Copyright 2009-2021 java-diff-utils.
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
package io.github.petertrr.diffutils

import io.github.petertrr.diffutils.patch.ChangeDelta
import io.github.petertrr.diffutils.patch.Chunk
import io.github.petertrr.diffutils.patch.DeleteDelta
import io.github.petertrr.diffutils.patch.EqualDelta
import io.github.petertrr.diffutils.patch.InsertDelta
import io.github.petertrr.diffutils.patch.Patch
import io.github.petertrr.diffutils.utils.changeDeltaOf
import io.github.petertrr.diffutils.utils.deleteDeltaOf
import io.github.petertrr.diffutils.utils.insertDeltaOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DiffUtilsTest {
    @Test
    fun testDiff_Insert() {
        val patch: Patch<String> = diff(listOf("hhh"), listOf("hhh", "jjj", "kkk"))
        assertNotNull(patch)
        assertEquals(1, patch.deltas.size)
        val delta = patch.deltas[0]
        assertTrue(delta is InsertDelta)
        assertEquals(Chunk(1, emptyList()), delta.source)
        assertEquals(Chunk(1, listOf("jjj", "kkk")), delta.target)
    }

    @Test
    fun testDiff_Delete() {
        val patch = diff(listOf("ddd", "fff", "ggg"), listOf("ggg"))
        assertNotNull(patch)
        assertEquals(1, patch.deltas.size)
        val delta = patch.deltas[0]
        assertTrue(delta is DeleteDelta)
        assertEquals(Chunk(0, listOf("ddd", "fff")), delta.source)
        assertEquals(Chunk(0, emptyList()), delta.target)
    }

    @Test
    fun testDiff_Change() {
        val changeTest_from: List<String> = listOf("aaa", "bbb", "ccc")
        val changeTest_to: List<String> = listOf("aaa", "zzz", "ccc")
        val patch: Patch<String> = diff(changeTest_from, changeTest_to)
        assertNotNull(patch)
        assertEquals(1, patch.deltas.size)
        val delta = patch.deltas[0]
        assertTrue(delta is ChangeDelta)
        assertEquals(Chunk(1, listOf("bbb")), delta.source)
        assertEquals(Chunk(1, listOf("zzz")), delta.target)
    }

    @Test
    fun testDiff_EmptyList() {
        val patch: Patch<String> = diff(emptyList(), emptyList())
        assertNotNull(patch)
        assertEquals(0, patch.deltas.size)
    }

    @Test
    fun testDiff_EmptyListWithNonEmpty() {
        val patch: Patch<String> = diff(emptyList(), listOf("aaa"))
        assertNotNull(patch)
        assertEquals(1, patch.deltas.size)
        val delta = patch.deltas[0]
        assertTrue(delta is InsertDelta)
    }

    @Test
    fun testDiffInline() {
        val patch: Patch<String> = diffInline("", "test")
        assertEquals(1, patch.deltas.size)
        assertTrue(patch.deltas[0] is InsertDelta)
        assertEquals(0, patch.deltas[0].source.position)
        assertEquals(0, patch.deltas[0].source.lines.size)
        assertEquals("test", patch.deltas[0].target.lines[0])
    }

    @Test
    fun testDiffInline2() {
        val patch: Patch<String> = diffInline("es", "fest")
        assertEquals(2, patch.deltas.size)
        assertTrue(patch.deltas[0] is InsertDelta)
        assertEquals(0, patch.deltas[0].source.position)
        assertEquals(2, patch.deltas[1].source.position)
        assertEquals(0, patch.deltas[0].source.lines.size)
        assertEquals(0, patch.deltas[1].source.lines.size)
        assertEquals("f", patch.deltas[0].target.lines[0])
        assertEquals("t", patch.deltas[1].target.lines[0])
    }

    @Test
    fun testDiffIntegerList() {
        val original: List<Int> = listOf(1, 2, 3, 4, 5)
        val revised: List<Int> = listOf(2, 3, 4, 6)
        val patch: Patch<Int> = diff(original, revised)
        for (delta in patch.deltas) {
            println(delta)
        }
        assertEquals(2, patch.deltas.size)
        assertEquals(deleteDeltaOf(0, listOf(1)), patch.deltas[0])
        assertEquals(changeDeltaOf(4, listOf(5), 3, listOf(6)), patch.deltas[1])
    }

    @Test
    fun testDiffMissesChangeForkDnaumenkoIssue31() {
        val original: List<String> = listOf("line1", "line2", "line3")
        val revised: List<String> = listOf("line1", "line2-2", "line4")
        val patch: Patch<String> = diff(original, revised)
        assertEquals(1, patch.deltas.size)
        assertEquals(
            changeDeltaOf(1, listOf("line2", "line3"), 1, listOf("line2-2", "line4")),
            patch.deltas[0]
        )
    }

    @Test
    fun testDiffMyersExample1() {
        val patch: Patch<String> = diff(listOf("A", "B", "C", "A", "B", "B", "A"), listOf("C", "B", "A", "B", "A", "C"))
        assertNotNull(patch)
        assertEquals(4, patch.deltas.size)
        assertEquals(
            "Patch{deltas=[${deleteDeltaOf(0, listOf("A", "B"))}, ${insertDeltaOf(3, 1, listOf("B"))}, " +
                    "${deleteDeltaOf(5, listOf("B"), 4)}, ${insertDeltaOf(7, 5, listOf("C"))}]}",
            patch.toString()
        )
    }

    @Test
    fun testDiff_Equal() {
        val patch: Patch<String> = diff(
            source = listOf("hhh", "jjj", "kkk"),
            target = listOf("hhh", "jjj", "kkk"),
            includeEqualParts = true,
        )
        assertNotNull(patch)
        assertEquals(1, patch.deltas.size)
        val delta = patch.deltas[0]
        assertTrue(delta is EqualDelta)
        assertEquals(Chunk(0, listOf("hhh", "jjj", "kkk")), delta.source)
        assertEquals(Chunk(0, listOf("hhh", "jjj", "kkk")), delta.target)
    }

    @Test
    fun testDiff_InsertWithEqual() {
        val patch: Patch<String> = diff(
            source = listOf("hhh"),
            target = listOf("hhh", "jjj", "kkk"),
            includeEqualParts = true,
        )
        assertNotNull(patch)
        assertEquals(2, patch.deltas.size)
        var delta = patch.deltas[0]
        assertTrue(delta is EqualDelta)
        assertEquals(Chunk(0, listOf("hhh")), delta.source)
        assertEquals(Chunk(0, listOf("hhh")), delta.target)
        delta = patch.deltas[1]
        assertTrue(delta is InsertDelta)
        assertEquals(Chunk(1, emptyList()), delta.source)
        assertEquals(Chunk(1, listOf("jjj", "kkk")), delta.target)
    }

    @Test
    fun testDiff_ProblemIssue42() {
        val patch: Patch<String> = diff(
            source = listOf("The", "dog", "is", "brown"),
            target = listOf("The", "fox", "is", "down"),
            includeEqualParts = true,
        )
        println(patch)
        assertNotNull(patch)
        assertEquals(4, patch.deltas.size)
        assertEquals(
            listOf("EQUAL", "CHANGE", "EQUAL", "CHANGE"),
            patch.deltas.map { it.type.name }
        )
        var delta = patch.deltas[0]
        assertTrue(delta is EqualDelta)
        assertEquals(Chunk(0, listOf("The")), delta.source)
        assertEquals(Chunk(0, listOf("The")), delta.target)
        delta = patch.deltas[1]
        assertTrue(delta is ChangeDelta)
        assertEquals(Chunk(1, listOf("dog")), delta.source)
        assertEquals(Chunk(1, listOf("fox")), delta.target)
        delta = patch.deltas[2]
        assertTrue(delta is EqualDelta)
        assertEquals(Chunk(2, listOf("is")), delta.source)
        assertEquals(Chunk(2, listOf("is")), delta.target)
        delta = patch.deltas[3]
        assertTrue(delta is ChangeDelta)
        assertEquals(Chunk(3, listOf("brown")), delta.source)
        assertEquals(Chunk(3, listOf("down")), delta.target)
    }
}
