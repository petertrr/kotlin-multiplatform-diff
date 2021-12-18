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
package io.github.petertrr.diffutils.unifieddiff

import io.github.petertrr.diffutils.patch.PatchFailedException

public class UnifiedDiff(
    public var header: String? = null,
    public var tail: String? = null
) {
    private val files: MutableList<UnifiedDiffFile> = mutableListOf<UnifiedDiffFile>()

    public fun addFile(file: UnifiedDiffFile) {
        files.add(file)
    }

    public fun getFiles(): List<UnifiedDiffFile> {
        return files.toList()
    }

    @Throws(PatchFailedException::class)
    public fun applyPatchTo(findFile: (String?) -> Boolean, originalLines: List<String>): List<String> {
        val file: UnifiedDiffFile? = files.firstOrNull { findFile(it.fromFile) }
        return file?.patch?.applyTo(originalLines) ?: originalLines
    }

    public companion object {
        public fun from(header: String?, tail: String?, vararg files: UnifiedDiffFile): UnifiedDiff {
            val diff = UnifiedDiff(
                header = header,
                tail = tail,
            )
            for (file in files) {
                diff.addFile(file)
            }
            return diff
        }
    }
}