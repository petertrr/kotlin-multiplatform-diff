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
 * Specifies the type of the delta. There are three types of modifications from
 * the original to get the revised text.
 *
 * CHANGE: a block of data of the original is replaced by another block of data.
 * DELETE: a block of data of the original is removed
 * INSERT: at a position of the original a block of data is inserted
 *
 * to be complete there is also
 *
 * EQUAL: a block of data of original and the revised text is equal
 *
 * which is no change at all.
 *
 */
public enum class DeltaType {
    /**
     * A change in the original.
     */
    CHANGE,

    /**
     * A delete from the original.
     */
    DELETE,

    /**
     * An insert into the original.
     */
    INSERT,

    /**
     * An do nothing.
     */
    EQUAL
}

public sealed class Delta<T>(public val type: DeltaType) {
    public abstract val source: Chunk<T>
    public abstract val target: Chunk<T>

    /**
     * Verify the chunk of this delta, to fit the target.
     * @param target
     * @throws PatchFailedException
     */
    @Throws(PatchFailedException::class)
    protected open fun verifyChunkToFitTarget(target: List<T>): VerifyChunk {
        return source.verify(target)
    }

    @Throws(PatchFailedException::class)
    public open fun verifyAndApplyTo(target: MutableList<T>): VerifyChunk {
        val verify: VerifyChunk = verifyChunkToFitTarget(target)
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
public data class ChangeDelta<T>(override val source: Chunk<T>, override val target: Chunk<T>) : Delta<T>(
    DeltaType.CHANGE
) {
    protected override fun applyTo(target: MutableList<T>) {
        val position: Int = source.position
        val size: Int = source.size()
        for (i in 0 until size) {
            target.removeAt(position)
        }
        this.target.lines.forEachIndexed { i, line ->
            target.add(position + i, line)
        }
    }

    override fun restore(target: MutableList<T>) {
        val position: Int = this.target.position
        val size: Int = this.target.size()
        for (i in 0 until size) {
            target.removeAt(position)
        }
        source.lines.forEachIndexed { i, line ->
            target.add(position + i, line)
        }
    }

    override fun withChunks(original: Chunk<T>, revised: Chunk<T>): Delta<T> = ChangeDelta(original, revised)
}

public data class DeleteDelta<T>(override val source: Chunk<T>, override val target: Chunk<T>) : Delta<T>(
    DeltaType.DELETE
) {
    protected override fun applyTo(target: MutableList<T>) {
        val position = source.position
        for (i in 0 until source.size()) {
            target.removeAt(position)
        }
    }

    override fun restore(target: MutableList<T>) {
        val position: Int = this.target.position
        val lines: List<T> = this.source.lines
        lines.forEachIndexed { i, line ->
            target.add(position + i, line)
        }
    }

    override fun withChunks(original: Chunk<T>, revised: Chunk<T>): Delta<T> = DeleteDelta(original, revised)
}

public data class InsertDelta<T>(override val source: Chunk<T>, override val target: Chunk<T>) : Delta<T>(
    DeltaType.INSERT
) {
    protected override fun applyTo(target: MutableList<T>) {
        val position = this.source.position
        this.target.lines.forEachIndexed { i, line ->
            target.add(position + i, line)
        }
    }

    override fun restore(target: MutableList<T>) {
        val position = this.target.position
        for (i in 0 until this.target.size()) {
            target.removeAt(position)
        }
    }

    override fun withChunks(original: Chunk<T>, revised: Chunk<T>): Delta<T> = InsertDelta(original, revised)
}

public data class EqualDelta<T>(override val source: Chunk<T>, override val target: Chunk<T>) : Delta<T>(
    DeltaType.EQUAL
) {
    protected override fun applyTo(target: MutableList<T>): Unit = Unit

    override fun restore(target: MutableList<T>): Unit = Unit

    override fun withChunks(original: Chunk<T>, revised: Chunk<T>): Delta<T> = EqualDelta(original, revised)
}
