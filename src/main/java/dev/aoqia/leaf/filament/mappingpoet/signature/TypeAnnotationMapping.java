/*
 * Copyright (c) 2020 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.aoqia.leaf.filament.mappingpoet.signature;

import org.objectweb.asm.TypeReference;

/**
 * The collection of type annotations from a bytecode structure that stores type annotations.
 */
public interface TypeAnnotationMapping {
    TypeAnnotationMapping EMPTY = reference -> TypeAnnotationBank.EMPTY;

    // implNote: TypeReference is not a pojo! No equals or hash!
    TypeAnnotationBank getBank(TypeReference reference);
}
