/*
 * Copyright 2021 Peter Trifanov.
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

public fun interface ConflictOutput<T> {
    @Throws(PatchFailedException::class)
    public fun processConflict(verifyChunk: VerifyChunk, delta: Delta<T>, result: MutableList<T>)
}

public class ExceptionProducingConflictOutput<T> : ConflictOutput<T> {
    override fun processConflict(verifyChunk: VerifyChunk, delta: Delta<T>, result: MutableList<T>) {
        throw PatchFailedException(
            "could not apply patch due to $verifyChunk"
        )
    }
}

public class ConflictProducingConflictOutput : ConflictOutput<String> {
    override fun processConflict(verifyChunk: VerifyChunk, delta: Delta<String>, result: MutableList<String>) {
        if (result.size > delta.source.position) {
            val orgData = mutableListOf<String>()
            (0 until delta.source.size()).forEach { _ ->
                orgData.add(
                    result.removeAt(delta.source.position)
                )
            }
            orgData.add(0, "<<<<<< HEAD")
            orgData.add("======")
            orgData.addAll(delta.source.lines)
            orgData.add(">>>>>>> PATCH")
            result.addAll(delta.source.position, orgData)
        } else {
            TODO("Not supported yet.")
        }
    }
}
