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

import io.github.petertrr.diffutils.patch.Patch

/**
 * Data structure for one patched file from a unified diff file.
 */
public class UnifiedDiffFile(
    public var diffCommand: String? = null,
    public var fromFile: String? = null,
    public var fromTimestamp: String? = null,
    public var toFile: String? = null,
    public var renameFrom: String? = null,
    public var renameTo: String? = null,
    public var toTimestamp: String? = null,
    public var index: String? = null,
    public var newFileMode: String? = null,
    public var deletedFileMode: String? = null,
    public val patch: Patch<String> = Patch(),
    public var isNoNewLineAtTheEndOfTheFile: Boolean = false,
    public var similarityIndex: Int? = null,
) {
    public companion object {
        public fun from(fromFile: String, toFile: String, patch: Patch<String>): UnifiedDiffFile {
            return UnifiedDiffFile(
                fromFile = fromFile,
                toFile = toFile,
                patch = patch,
            )
        }
    }
}
