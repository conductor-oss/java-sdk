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
package org.conductoross.conductor.ai.model;

import java.util.Map;
import java.util.function.Function;

import org.conductoross.conductor.ai.enums.OnFail;
import org.conductoross.conductor.ai.enums.Position;

/**
 * Runtime model for a guardrail definition.
 *
 * <p>This is the runtime counterpart to the {@link org.conductoross.conductor.ai.annotations.GuardrailDef}
 * annotation. Use {@link Builder} to create instances.
 */
public class GuardrailDef {
    private final String name;
    private final Position position;
    private final OnFail onFail;
    private final int maxRetries;
    private final Function<String, GuardrailResult> func;
    private final String guardrailType;
    private final Map<String, Object> config;

    private GuardrailDef(Builder builder) {
        this.name = builder.name;
        this.position = builder.position;
        this.onFail = builder.onFail;
        this.maxRetries = builder.maxRetries;
        this.func = builder.func;
        this.guardrailType = builder.guardrailType;
        this.config = builder.config;
    }

    public String getName() {
        return name;
    }

    public Position getPosition() {
        return position;
    }

    public OnFail getOnFail() {
        return onFail;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public Function<String, GuardrailResult> getFunc() {
        return func;
    }

    public String getGuardrailType() {
        return guardrailType;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private Position position = Position.OUTPUT;
        private OnFail onFail = OnFail.RAISE;
        private int maxRetries = 3;
        private Function<String, GuardrailResult> func;
        private String guardrailType = "custom";
        private Map<String, Object> config;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder position(Position position) {
            this.position = position;
            return this;
        }

        public Builder onFail(OnFail onFail) {
            this.onFail = onFail;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder func(Function<String, GuardrailResult> func) {
            this.func = func;
            return this;
        }

        public Builder guardrailType(String guardrailType) {
            this.guardrailType = guardrailType;
            return this;
        }

        public Builder config(Map<String, Object> config) {
            this.config = config;
            return this;
        }

        public GuardrailDef build() {
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("GuardrailDef requires a name");
            }
            if (maxRetries < 0) {
                throw new IllegalArgumentException("GuardrailDef maxRetries must be nonnegative");
            }
            if (onFail == OnFail.HUMAN && position != Position.OUTPUT) {
                throw new IllegalArgumentException("onFail=HUMAN is only valid for position=OUTPUT");
            }
            return new GuardrailDef(this);
        }
    }
}
