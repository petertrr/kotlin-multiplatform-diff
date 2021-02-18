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
package io.github.petertrr.diffutils.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StringUtilsTest {
    /**
     * Test of htmlEntities method, of class StringUtils.
     */
    @Test
    fun testHtmlEntites() {
        assertEquals("&lt;test&gt;", htmlEntities("<test>"))
    }

    /**
     * Test of normalize method, of class StringUtils.
     */
    @Test
    fun testNormalize_String() {
        assertEquals("    test", normalize("\ttest"))
    }

    /**
     * Test of wrapText method, of class
     */
    @Test
    fun testWrapText_String_int() {
        assertEquals("te<br/>st", wrapText("test", 2))
        assertEquals("tes<br/>t", wrapText("test", 3))
        assertEquals("test", wrapText("test", 10))
        assertEquals(".\uD800\uDC01<br/>.", wrapText(".\uD800\uDC01.", 2))
        assertEquals("..<br/>\uD800\uDC01", wrapText("..\uD800\uDC01", 3))
    }

    @Test
    fun testWrapText_String_int_zero() {
        assertFailsWith<IllegalArgumentException> { wrapText("test", -1) }
    }
}
