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

import org.conductoross.conductor.sdk.internal.Command;
import org.conductoross.conductor.sdk.internal.Engine;

/** A chat completion executed by the server (LLM_CHAT_COMPLETE). Models are {@code provider/model}. */
public final class LlmCall {

    private final Command command;

    LlmCall(String ref) {
        command = new Command(Command.Kind.LLM, ref, ref);
        command.explicitRef = ref;
        retries(3, java.time.Duration.ofSeconds(1));
    }

    /** Retries after a failure, with exponential backoff from the given delay; default 3 and 1s. */
    public LlmCall retries(int count, java.time.Duration delay) {
        command.fields.put(
                "taskDefinition",
                org.conductoross.conductor.sdk.internal.Entries.retryingDefinition(
                        command.name, count, Math.max(1, delay.toSeconds())));
        return this;
    }

    public LlmCall model(String providerAndModel) {
        int slash = providerAndModel.indexOf('/');
        if (slash > 0) {
            command.input.put("llmProvider", providerAndModel.substring(0, slash));
            command.input.put("model", providerAndModel.substring(slash + 1));
        } else {
            command.input.put("model", providerAndModel);
        }
        return this;
    }

    public LlmCall system(String instructions) {
        command.input.put("instructions", instructions);
        return this;
    }

    public LlmCall prompt(String prompt) {
        command.input.put("userInput", prompt);
        return this;
    }

    public <T> T as(Class<T> type) {
        command.resultType = type;
        return type.cast(Engine.current().call(command));
    }
}
