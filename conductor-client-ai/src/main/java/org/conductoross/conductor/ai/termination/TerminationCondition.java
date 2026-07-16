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

import java.util.Map;

/**
 * Base class for composable termination conditions.
 *
 * <p>Conditions can be combined with {@link #and(TerminationCondition)} and
 * {@link #or(TerminationCondition)} to build complex logic.
 *
 * <p>Example:
 * <pre>{@code
 * TerminationCondition cond = MaxMessageTermination.of(10)
 *     .or(TextMentionTermination.of("DONE"));
 * }</pre>
 */
public abstract class TerminationCondition {

    /**
     * Combine this condition with another using AND logic.
     * Both conditions must be met for termination.
     */
    public TerminationCondition and(TerminationCondition other) {
        return new AndTermination(this, other);
    }

    /**
     * Combine this condition with another using OR logic.
     * Either condition being met triggers termination.
     */
    public TerminationCondition or(TerminationCondition other) {
        return new OrTermination(this, other);
    }

    /**
     * Serialize this condition to a map for JSON serialization.
     */
    public abstract Map<String, Object> toMap();
}
