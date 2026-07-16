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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A post-execution check. Runs after all {@link Step}s complete. PAC routes
 * the workflow to {@code on_success} when every validation passes, else to
 * {@code on_failure}.
 */
public final class Validation {
    private final String tool;
    private final Map<String, Object> args;
    private final String successCondition;

    private Validation(Builder b) {
        this.tool = b.tool;
        this.args = b.args;
        this.successCondition = b.successCondition;
    }

    public Map<String, Object> toJson() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("tool", tool);
        if (args != null) out.put("args", PlanValues.serializeArgs(args));
        if (successCondition != null) out.put("success_condition", successCondition);
        return out;
    }

    public static Builder builder(String tool) {
        return new Builder(tool);
    }

    public static final class Builder {
        private final String tool;
        private Map<String, Object> args;
        private String successCondition;

        private Builder(String tool) {
            this.tool = tool;
        }

        public Builder args(Map<String, Object> args) {
            this.args = args;
            return this;
        }

        /**
         * Optional JS expression evaluated against the tool's output ({@code $}
         * is the parsed output map). Returns truthy on pass.
         */
        public Builder successCondition(String expr) {
            this.successCondition = expr;
            return this;
        }

        public Validation build() {
            return new Validation(this);
        }
    }
}
