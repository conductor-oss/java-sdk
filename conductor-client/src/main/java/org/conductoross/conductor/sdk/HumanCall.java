/*
 * Copyright 2026 Conductor Authors.
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
package org.conductoross.conductor.sdk;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.conductoross.conductor.sdk.internal.Command;
import org.conductoross.conductor.sdk.internal.Engine;
import org.conductoross.conductor.sdk.internal.Json;

/**
 * A HUMAN task: shown in the UI and the human task API, completed by a person (or any system)
 * with {@code POST /api/tasks/{workflowId}/{ref}/COMPLETED}.
 */
public final class HumanCall {

    private final Command command;

    HumanCall(String ref) {
        command = new Command(Command.Kind.HUMAN, ref, ref);
        command.explicitRef = ref;
    }

    public HumanCall assignee(String assignee) {
        command.input.put("assignee", assignee);
        return this;
    }

    public HumanCall input(Map<String, ?> input) {
        input.forEach((k, v) -> command.input.put(k, Json.plain(v)));
        return this;
    }

    /** Waits for the answer; empty when nobody answers within the timeout. */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> await(Class<T> type, Duration timeout) {
        Map<String, Object> def = new LinkedHashMap<>();
        def.put("name", command.name);
        def.put("timeoutSeconds", Math.max(1, timeout.toSeconds()));
        def.put("responseTimeoutSeconds", Math.max(1, timeout.toSeconds()));
        // A timeout policy of RETRY with no retries makes a timeout a TIMED_OUT task the code
        // sees as an empty result; TIME_OUT_WF would end the workflow instead.
        def.put("timeoutPolicy", "RETRY");
        def.put("retryCount", 0);
        def.put("ownerEmail", "native@conductor.local");
        command.fields.put("taskDefinition", def);
        command.resultType = type;
        command.optional = true;
        command.timeoutIsEmpty = true;
        command.seconds = timeout.toSeconds();
        return (Optional<T>) Engine.current().call(command);
    }
}
