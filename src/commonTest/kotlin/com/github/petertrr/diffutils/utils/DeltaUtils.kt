/*
 * Copyright 2021 Peter Trifanov.
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
package io.github.petertrr.diffutils.utils

import io.github.petertrr.diffutils.patch.ChangeDelta
import io.github.petertrr.diffutils.patch.Chunk
import io.github.petertrr.diffutils.patch.DeleteDelta
import io.github.petertrr.diffutils.patch.InsertDelta

fun <T> deleteDeltaOf(sourcePosition: Int, sourceLines: List<T>,
                      targetPosition: Int = sourcePosition) = DeleteDelta(
    Chunk(sourcePosition, sourceLines),
    Chunk(targetPosition, emptyList())
)

fun <T> changeDeltaOf(sourcePosition: Int, sourceLines: List<T>,
                      targetPosition: Int, targetLines: List<T>) = ChangeDelta(
    Chunk(sourcePosition, sourceLines),
    Chunk(targetPosition, targetLines)
)

fun <T> insertDeltaOf(sourcePosition: Int, targetPosition: Int, targetLines: List<T>) = InsertDelta(
    Chunk(sourcePosition, emptyList()),
    Chunk(targetPosition, targetLines)
)
