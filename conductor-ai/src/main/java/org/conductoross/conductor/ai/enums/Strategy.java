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
package org.conductoross.conductor.ai.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * How sub-agents are orchestrated in a multi-agent system.
 */
public enum Strategy {
    @JsonProperty("handoff")
    HANDOFF,

    @JsonProperty("sequential")
    SEQUENTIAL,

    @JsonProperty("parallel")
    PARALLEL,

    @JsonProperty("router")
    ROUTER,

    @JsonProperty("round_robin")
    ROUND_ROBIN,

    @JsonProperty("random")
    RANDOM,

    @JsonProperty("swarm")
    SWARM,

    @JsonProperty("manual")
    MANUAL,

    @JsonProperty("plan_execute")
    PLAN_EXECUTE;

    public String toJsonValue() {
        try {
            return Strategy.class
                    .getField(name())
                    .getAnnotation(JsonProperty.class)
                    .value();
        } catch (NoSuchFieldException e) {
            return name().toLowerCase();
        }
    }
}
