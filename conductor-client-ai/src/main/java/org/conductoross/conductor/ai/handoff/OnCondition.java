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

import java.util.Map;
import java.util.function.Function;

/**
 * Hand off when a custom callable returns {@code true}.
 *
 * <p>The condition function receives the current agent context map and returns
 * a boolean. Serialized as a worker task — the function is registered as a
 * Conductor worker under the name {@code {agentName}_handoff_{target}}.
 *
 * <pre>{@code
 * new OnCondition("supervisor", ctx -> {
 *     Object iter = ctx.get("iteration");
 *     return iter instanceof Number && ((Number) iter).intValue() > 5;
 * })
 * }</pre>
 */
public class OnCondition extends Handoff {

    private final Function<Map<String, Object>, Boolean> condition;

    public OnCondition(String target, Function<Map<String, Object>, Boolean> condition) {
        super(target);
        this.condition = condition;
    }

    public Function<Map<String, Object>, Boolean> getCondition() {
        return condition;
    }
}
