/*
 * Copyright 2024 Peter Trifanov.
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
 */
package io.github.petertrr.diffutils.text

/**
 * Splitting lines by word to achieve word by word diff checking.
 */
internal class WordDiffSplitter(private val pattern: Regex = Regex("\\s+|[,.\\[\\](){}/\\\\*+\\-#]")) : DiffSplitter {
    override fun split(line: String): MutableList<String> {
        val matchResults = pattern.findAll(line)
        val list = ArrayList<String>()
        var pos = 0

        for (matchResult in matchResults) {
            if (pos < matchResult.range.first) {
                list.add(line.substring(pos, matchResult.range.first))
            }

            list.add(matchResult.value)
            pos = matchResult.range.last + 1
        }

        if (pos < line.length) {
            list.add(line.substring(pos))
        }

        return list
    }
}
