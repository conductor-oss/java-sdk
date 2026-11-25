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

import org.conductoross.conductor.sdk.internal.Command;
import org.conductoross.conductor.sdk.internal.Engine;
import org.conductoross.conductor.sdk.internal.Entries;

/** A chat completion executed by the server (LLM_CHAT_COMPLETE). Models are {@code provider/model}. */
public final class LlmCall {

    private final Command command;

    LlmCall(String ref) {
        command = new Command(Command.Kind.LLM, ref, ref);
        command.explicitRef = ref;
        command.outputKey = "result"; // the model's answer; the rest of the output is token counts
        retries(3, Duration.ofSeconds(1));
    }

    /**
     * Retries after a failure, with exponential backoff from the given delay; default 3 and 1s.
     * The server retries an AI task at least once: a count below 1 becomes 3, with linear backoff
     * from 2 seconds ({@code AIModelTaskMapper}).
     */
    public LlmCall retries(int count, Duration delay) {
        command.fields.put(
                "taskDefinition",
                Entries.retryingDefinition(
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

    /**
     * Asks the model and returns its answer as {@code type}: a {@code String} for text, or a record
     * or class for a JSON answer, which the server then requires (an answer that is not JSON fails
     * the call, and is retried). Say in the prompt which fields to answer with.
     */
    public <T> T as(Class<T> type) {
        Command c = command.copy();
        c.resultType = type;
        if (type != String.class) {
            c.input.put("jsonOutput", true);
        }
        return type.cast(Engine.current().call(c));
    }
}
