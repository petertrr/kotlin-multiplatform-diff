/*
 * Copyright 2021 Peter Trifanov.
 * Copyright 2018 java-diff-utils.
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

/**
 * A delta between a source and a target.
 */
public sealed class Delta<T>(public val type: DeltaType) {
    public abstract val source: Chunk<T>
    public abstract val target: Chunk<T>

    /**
     * Verify the chunk of this delta, to fit the target.
     */
    @Throws(PatchFailedException::class)
    protected open fun verifyChunkToFitTarget(target: List<T>): VerifyChunk {
        return source.verify(target)
    }

    @Throws(PatchFailedException::class)
    public open fun verifyAndApplyTo(target: MutableList<T>): VerifyChunk {
        val verify = verifyChunkToFitTarget(target)

        if (verify == VerifyChunk.OK) {
            applyTo(target)
        }

        return verify
    }

    @Throws(PatchFailedException::class)
    protected abstract fun applyTo(target: MutableList<T>)

    public abstract fun restore(target: MutableList<T>)

    /**
     * Create a new delta of the actual instance with customized chunk data.
     */
    public abstract fun withChunks(original: Chunk<T>, revised: Chunk<T>): Delta<T>
}
