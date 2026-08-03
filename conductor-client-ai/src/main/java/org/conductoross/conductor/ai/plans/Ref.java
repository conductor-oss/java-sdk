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
import java.util.Objects;

/**
 * A reference to a prior step's whole output.
 *
 * <p>Use {@code new Ref("step_id")} anywhere a literal value would go in an
 * {@link Op}'s {@code args} or a {@link Generate}'s {@code context} to wire
 * one step's output into another step's input — no JSON path, no field
 * selection. The whole result becomes the value at that arg key.
 *
 * <p>The referenced step must be declared in this step's {@code dependsOn}
 * and must exist in the plan; the server rejects the plan at compile time
 * otherwise (no silent broken refs).
 *
 * <p>Self-Refs and Refs to a step not in {@code dependsOn} are compile
 * errors. For a {@code parallel=true} step, the Ref resolves to the array
 * of branch results (the FORK_JOIN aggregator's payload).
 *
 * <p>Serialises to the wire form {@code {"$ref": "<step_id>"}} — same
 * contract as the Python and TypeScript SDKs.
 */
public final class Ref {

    private final String stepId;

    public Ref(String stepId) {
        if (stepId == null || stepId.isEmpty()) {
            throw new IllegalArgumentException("Ref stepId must be a non-empty string");
        }
        this.stepId = stepId;
    }

    public String getStepId() {
        return stepId;
    }

    /** Wire format the server's PAC consumes: {@code {"$ref": "<step_id>"}}. */
    public Map<String, Object> toJson() {
        return Map.of("$ref", stepId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ref other)) return false;
        return Objects.equals(stepId, other.stepId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(stepId);
    }

    @Override
    public String toString() {
        return "Ref(" + stepId + ")";
    }
}
