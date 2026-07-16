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
package org.conductoross.conductor.ai.handoff;

/**
 * Base class for condition-based handoff triggers.
 *
 * <p>Handoffs are evaluated when no transfer tool was called and allow the
 * agent to transfer control based on text mentions or tool results.
 *
 * <p>Use {@link OnTextMention} or {@link OnToolResult} to create handoff conditions.
 */
public abstract class Handoff {
    private final String target;

    protected Handoff(String target) {
        this.target = target;
    }

    public String getTarget() {
        return target;
    }
}
