/*
 * Copyright 2026 Conductor Authors.
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
package org.conductoross.conductor.sdk.file;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileHandlerTest {

    private static final String RAW_ID = "abc-123";
    private static final String HANDLE_ID = FileHandler.PREFIX + RAW_ID;

    @Test
    void toFileHandleIdAddsPrefixOnce() {
        assertEquals(HANDLE_ID, FileHandler.toFileHandleId(RAW_ID));
    }

    @Test
    void toFileHandleIdIsIdempotent() {
        String already = FileHandler.toFileHandleId(HANDLE_ID);
        assertSame(HANDLE_ID, already, "must not re-prefix an already-prefixed id");
    }

    @Test
    void toFileIdStripsPrefix() {
        assertEquals(RAW_ID, FileHandler.toFileId(HANDLE_ID));
    }

    @Test
    void toFileIdReturnsInputWhenPrefixAbsent() {
        assertEquals(RAW_ID, FileHandler.toFileId(RAW_ID));
    }

    @Test
    void roundTripFromRawIdIsStable() {
        String handle = FileHandler.toFileHandleId(RAW_ID);
        String raw = FileHandler.toFileId(handle);
        assertEquals(RAW_ID, raw);
    }

    @Test
    void isFileHandleIdTrueForPrefixedString() {
        assertTrue(FileHandler.isFileHandleId(HANDLE_ID));
    }

    @Test
    void isFileHandleIdFalseForUnprefixedString() {
        assertFalse(FileHandler.isFileHandleId(RAW_ID));
    }

    @Test
    void isFileHandleIdFalseForNonStringValues() {
        assertFalse(FileHandler.isFileHandleId(null));
        assertFalse(FileHandler.isFileHandleId(42));
        assertFalse(FileHandler.isFileHandleId(new Object()));
    }

    @Test
    void prefixIsTheDocumentedScheme() {
        assertEquals("conductor://file/", FileHandler.PREFIX);
    }
}
