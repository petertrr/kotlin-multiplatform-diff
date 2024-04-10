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
@file:JvmName("StringUtils")

package io.github.petertrr.diffutils.text

import kotlin.jvm.JvmName

/**
 * Replaces all opening and closing tags (`<` and `>`)
 * with their escaped sequences (`&lt;` and `&gt;`).
 */
internal fun htmlEntities(str: String): String =
    str.replace("<", "&lt;").replace(">", "&gt;")

/**
 * Normalizes a string by escaping some HTML meta characters
 * and replacing tabs with 4 spaces each.
 */
internal fun normalize(str: String): String =
    htmlEntities(str).replace("\t", "    ")

/**
 * Wrap the text with the given column width
 */
internal fun wrapText(line: String, columnWidth: Int): String {
    require(columnWidth >= 0) { "Column width must be greater than or equal to 0" }

    if (columnWidth == 0) {
        return line
    }

    val length = line.length
    val delimiter = "<br/>".length
    var widthIndex = columnWidth
    val b = StringBuilder(line)
    var count = 0

    while (length > widthIndex) {
        var breakPoint = widthIndex + delimiter * count

        if (b[breakPoint - 1].isHighSurrogate() && b[breakPoint].isLowSurrogate()) {
            // Shift a breakpoint that would split a supplemental code-point.
            breakPoint += 1

            if (breakPoint == b.length) {
                // Break before instead of after if this is the last code-point.
                breakPoint -= 2
            }
        }

        b.insert(breakPoint, "<br/>")
        widthIndex += columnWidth
        count++
    }

    return b.toString()
}
