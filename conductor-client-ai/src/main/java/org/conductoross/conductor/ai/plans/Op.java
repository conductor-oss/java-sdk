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
 * A single tool invocation within a plan step.
 *
 * <p>Exactly one of {@code args} or {@code generate} should be set.
 * {@code args} runs the tool deterministically with literal values;
 * {@code generate} defers arg construction to a per-op LLM call at run
 * time.
 */
public final class Op {
    private final String tool;
    private final Map<String, Object> args;
    private final Generate generate;

    private Op(Builder b) {
        if ((b.args == null) == (b.generate == null)) {
            throw new IllegalArgumentException("Op('" + b.tool + "'): exactly one of args or generate must be set");
        }
        this.tool = b.tool;
        this.args = b.args;
        this.generate = b.generate;
    }

    public Map<String, Object> toJson() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("tool", tool);
        if (args != null) out.put("args", PlanValues.serializeArgs(args));
        if (generate != null) out.put("generate", generate.toJson());
        return out;
    }

    public static Builder builder(String tool) {
        return new Builder(tool);
    }

    public static final class Builder {
        private final String tool;
        private Map<String, Object> args;
        private Generate generate;

        private Builder(String tool) {
            this.tool = tool;
        }

        public Builder args(Map<String, Object> args) {
            this.args = args;
            return this;
        }

        public Builder generate(Generate g) {
            this.generate = g;
            return this;
        }

        public Op build() {
            return new Op(this);
        }
    }
}
