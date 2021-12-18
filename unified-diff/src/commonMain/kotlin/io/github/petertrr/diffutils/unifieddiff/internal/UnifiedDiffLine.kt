/*
 * Copyright 2021 Peter Trifanov.
 * Copyright 2019 java-diff-utils.
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
package io.github.petertrr.diffutils.unifieddiff.internal

import io.github.petertrr.diffutils.unifieddiff.UnifiedDiffParserException

internal class UnifiedDiffLine(
    val isStopsHeaderParsing: Boolean,
    val pattern: Regex,
    val command: (MatchResult, String) -> Unit,
) {
    constructor(isStopsHeaderParsing: Boolean = false, pattern: String, command: (MatchResult, String) -> Unit) : this(
        isStopsHeaderParsing,
        Regex(pattern),
        command
    )

    fun validLine(line: String): Boolean {
        return pattern.matches(line)
    }

    @Throws(UnifiedDiffParserException::class)
    fun processLine(line: String): Boolean {
        val m = pattern.find(line)
        return if (m != null) {
            command(m, line)
            true
        } else {
            false
        }
    }

    override fun toString(): String {
        return "UnifiedDiffLine{pattern=$pattern, stopsHeaderParsing=$isStopsHeaderParsing}"
    }
}