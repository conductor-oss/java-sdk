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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A node in the plan DAG.
 *
 * <p>Steps run sequentially by default; {@code dependsOn} overrides to
 * express cross-step concurrency. {@code parallel=true} runs the step's
 * own {@link Op}s concurrently (FORK_JOIN).
 */
public final class Step {
    private final String id;
    private final List<Op> operations;
    private final List<String> dependsOn;
    private final boolean parallel;

    private Step(Builder b) {
        this.id = b.id;
        this.operations = List.copyOf(b.operations);
        this.dependsOn = List.copyOf(b.dependsOn);
        this.parallel = b.parallel;
    }

    public Map<String, Object> toJson() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", id);
        List<Map<String, Object>> ops = new ArrayList<>(operations.size());
        for (Op op : operations) ops.add(op.toJson());
        out.put("operations", ops);
        if (!dependsOn.isEmpty()) out.put("depends_on", new ArrayList<>(dependsOn));
        if (parallel) out.put("parallel", true);
        return out;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private final List<Op> operations = new ArrayList<>();
        private final List<String> dependsOn = new ArrayList<>();
        private boolean parallel = false;

        private Builder(String id) {
            this.id = id;
        }

        public Builder operation(Op op) {
            operations.add(op);
            return this;
        }

        public Builder operations(List<Op> ops) {
            operations.addAll(ops);
            return this;
        }

        public Builder dependsOn(String... ids) {
            for (String s : ids) dependsOn.add(s);
            return this;
        }

        public Builder parallel(boolean p) {
            this.parallel = p;
            return this;
        }

        public Step build() {
            return new Step(this);
        }
    }
}
