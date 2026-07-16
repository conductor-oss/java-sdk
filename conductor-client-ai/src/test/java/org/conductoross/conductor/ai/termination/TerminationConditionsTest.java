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
package org.conductoross.conductor.ai.termination;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pure unit tests for termination conditions: builders, toMap wire shape, composition. */
class TerminationConditionsTest {

    @Test
    void maxMessage() {
        MaxMessageTermination t = MaxMessageTermination.of(5);
        assertEquals(5, t.getMaxMessages());
        assertEquals("max_message", t.toMap().get("type"));
        assertEquals(5, t.toMap().get("maxMessages"));
    }

    @Test
    void stopMessage() {
        StopMessageTermination t = StopMessageTermination.of("DONE");
        assertEquals("DONE", t.getStopMessage());
        assertEquals("stop_message", t.toMap().get("type"));
    }

    @Test
    void textMention() {
        TextMentionTermination t = TextMentionTermination.of("bye", true);
        assertEquals("bye", t.getText());
        assertTrue(t.isCaseSensitive());
        assertFalse(TextMentionTermination.of("bye").isCaseSensitive());
    }

    @Test
    void andComposition() {
        TerminationCondition and = MaxMessageTermination.of(3).and(StopMessageTermination.of("x"));
        assertInstanceOf(AndTermination.class, and);
        assertNotNull(and.toMap());
    }

    @Test
    void orComposition() {
        TerminationCondition or = MaxMessageTermination.of(3).or(StopMessageTermination.of("x"));
        assertInstanceOf(OrTermination.class, or);
        assertNotNull(or.toMap());
    }

    @Test
    void terminationResult() {
        TerminationResult stop = TerminationResult.stop("done");
        assertTrue(stop.isShouldTerminate());
        assertEquals("done", stop.getReason());
        assertFalse(TerminationResult.continueRunning().isShouldTerminate());
    }
}
