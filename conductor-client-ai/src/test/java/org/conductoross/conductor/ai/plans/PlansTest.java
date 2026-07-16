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
package org.conductoross.conductor.ai.plans;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Pure unit tests for the PLAN_EXECUTE DSL builders (no server). */
class PlansTest {

    @Test
    void refCarriesStepIdAndEquals() {
        Ref r = new Ref("step1");
        assertEquals("step1", r.getStepId());
        assertNotNull(r.toJson());
        assertEquals(new Ref("step1"), r);
        assertNotEquals(new Ref("other"), r);
    }

    @Test
    void opSerializes() {
        Op op = Op.builder("git").args(Map.of("cmd", "status")).build();
        Map<String, Object> json = op.toJson();
        assertNotNull(json);
        assertFalse(json.isEmpty());
    }

    @Test
    void actionSerializes() {
        assertNotNull(Action.builder("notify").args(Map.of("msg", "hi")).build().toJson());
    }

    @Test
    void opRequiresExactlyOneOfArgsOrGenerate() {
        // Invariant enforced in Op's constructor.
        assertThrows(IllegalArgumentException.class, () -> Op.builder("git").build());
    }

    @Test
    void stepWithOperationSerializes() {
        Step s = Step.builder("s1")
                .operation(Op.builder("git").args(Map.of("cmd", "status")).build())
                .build();
        assertNotNull(s.toJson());
    }

    @Test
    void stepParallelAndDependsOn() {
        Step s = Step.builder("s2")
                .parallel(true)
                .dependsOn("s1")
                .operation(Op.builder("x").args(Map.of("k", "v")).build())
                .build();
        assertNotNull(s.toJson());
    }

    @Test
    void planWithStepsSerializesToJson() {
        Plan plan = Plan.builder()
                .step(Step.builder("s1")
                        .operation(
                                Op.builder("git").args(Map.of("cmd", "status")).build())
                        .build())
                .build();
        Map<String, Object> json = plan.toJson();
        assertNotNull(json);
        assertTrue(json.containsKey("steps"), "plan json should expose its steps; got keys: " + json.keySet());
    }
}
