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
package io.orkes.conductor.client.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EnumConversionTests {

    @Test
    public void testConsistencyValues() {
        Consistency[] values = Consistency.values();
        assertEquals(3, values.length);
        assertNotNull(Consistency.valueOf("SYNCHRONOUS"));
        assertNotNull(Consistency.valueOf("DURABLE"));
        assertNotNull(Consistency.valueOf("REGION_DURABLE"));
    }

    @Test
    public void testConsistencyRoundTrip() {
        for (Consistency value : Consistency.values()) {
            org.conductoross.conductor.common.model.Consistency clientConsistency = value.toClientConsistency();
            assertNotNull(clientConsistency);
            assertEquals(value.name(), clientConsistency.name());

            Consistency roundTripped = Consistency.fromClientConsistency(clientConsistency);
            assertEquals(value, roundTripped);
        }
    }

    @Test
    public void testReturnStrategyValues() {
        ReturnStrategy[] values = ReturnStrategy.values();
        assertEquals(4, values.length);
        assertNotNull(ReturnStrategy.valueOf("TARGET_WORKFLOW"));
        assertNotNull(ReturnStrategy.valueOf("BLOCKING_WORKFLOW"));
        assertNotNull(ReturnStrategy.valueOf("BLOCKING_TASK"));
        assertNotNull(ReturnStrategy.valueOf("BLOCKING_TASK_INPUT"));
    }

    @Test
    public void testReturnStrategyRoundTrip() {
        for (ReturnStrategy value : ReturnStrategy.values()) {
            org.conductoross.conductor.common.model.ReturnStrategy clientStrategy = value.toClientReturnStrategy();
            assertNotNull(clientStrategy);
            assertEquals(value.name(), clientStrategy.name());

            ReturnStrategy roundTripped = ReturnStrategy.fromClientReturnStrategy(clientStrategy);
            assertEquals(value, roundTripped);
        }
    }
}
