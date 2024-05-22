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

package io.github.petertrr.diffutils

import kotlin.jvm.JvmName

/**
 * Replaces all opening and closing tags (`<` and `>`)
 * with their escaped sequences (`&lt;` and `&gt;`).
 */
internal fun String.htmlEntities(): String =
    replace("<", "&lt;").replace(">", "&gt;")

/**
 * Normalizes a string by escaping some HTML meta characters
 * and replacing tabs with 4 spaces each.
 */
internal fun String.normalize(): String =
    htmlEntities().replace("\t", "    ")

/**
 * Wrap the text with the given column width.
 */
internal fun String.wrapText(columnWidth: Int): String {
    require(columnWidth >= 0) { "Column width must be greater than or equal to 0" }

    if (columnWidth == 0) {
        return this
    }

    val length = length
    val delimiterLength = "<br/>".length
    var widthIndex = columnWidth
    val sb = StringBuilder(this)
    var count = 0

    while (length > widthIndex) {
        var breakPoint = widthIndex + delimiterLength * count

        if (sb[breakPoint - 1].isHighSurrogate() && sb[breakPoint].isLowSurrogate()) {
            // Shift a breakpoint that would split a supplemental code-point.
            breakPoint += 1

            if (breakPoint == sb.length) {
                // Break before instead of after if this is the last code-point.
                breakPoint -= 2
            }
        }

        sb.insert(breakPoint, "<br/>")
        widthIndex += columnWidth
        count++
    }

    return sb.toString()
}
