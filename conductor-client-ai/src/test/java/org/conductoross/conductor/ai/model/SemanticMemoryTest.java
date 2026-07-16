/*
 * Copyright 2025 Conductor Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.conductoross.conductor.ai.model;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for {@link SemanticMemory} — client-side similarity store.
 *
 * <p>Mirrors the Python ({@code semantic_memory.py}) and C# ({@code SemanticMemory.cs})
 * reference: a Jaccard-keyword-overlap in-memory store, NOT a wire config.
 */
class SemanticMemoryTest {

    @Test
    void addReturnsIdAndSearchFindsRelevant() {
        SemanticMemory mem = new SemanticMemory();
        String id = mem.add("User prefers Python over JavaScript");
        mem.add("The weather today is sunny");
        assertNotNull(id);
        assertFalse(id.isEmpty());

        List<String> results = mem.search("What language does the user like?");
        assertFalse(results.isEmpty(), "search should return the relevant memory");
        assertEquals("User prefers Python over JavaScript", results.get(0));
    }

    @Test
    void emptyStoreReturnsEmpty() {
        SemanticMemory mem = new SemanticMemory();
        assertTrue(mem.search("anything").isEmpty());
        assertEquals("", mem.getContext("anything"), "empty memory yields empty context");
    }

    @Test
    void getContextFormatsMemories() {
        SemanticMemory mem = new SemanticMemory();
        mem.add("User name is Alice");
        String ctx = mem.getContext("What is the user name");
        assertTrue(ctx.startsWith("Relevant context from memory:"), "context: " + ctx);
        assertTrue(ctx.contains("Alice"), "context: " + ctx);
    }

    @Test
    void maxResultsCapsSearch() {
        SemanticMemory mem = new SemanticMemory(null, 1, null);
        mem.add("apple banana cherry");
        mem.add("apple banana date");
        mem.add("apple elderberry fig");
        List<String> results = mem.search("apple banana cherry");
        assertEquals(1, results.size(), "search must respect maxResults cap");
    }

    @Test
    void deleteAndClear() {
        SemanticMemory mem = new SemanticMemory();
        String id = mem.add("ephemeral fact");
        assertTrue(mem.delete(id));
        assertFalse(mem.delete(id), "deleting twice returns false");

        mem.add("a");
        mem.add("b");
        mem.clear();
        assertTrue(mem.listAll().isEmpty());
    }

    @Test
    void sessionIdAttachedToMetadata() {
        SemanticMemory mem = new SemanticMemory(null, 5, "sess-1");
        mem.add("fact one", Map.of("type", "fact"));
        List<MemoryEntry> entries = mem.listAll();
        assertEquals(1, entries.size());
        assertEquals("sess-1", entries.get(0).getMetadata().get("session_id"));
        assertEquals("fact", entries.get(0).getMetadata().get("type"));
    }
}
