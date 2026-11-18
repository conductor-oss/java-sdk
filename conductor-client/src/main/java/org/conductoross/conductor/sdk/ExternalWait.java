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
import java.util.Optional;

import org.conductoross.conductor.sdk.internal.Command;
import org.conductoross.conductor.sdk.internal.Conductors;
import org.conductoross.conductor.sdk.internal.Engine;

/**
 * Waits for an external system to call {@code POST /api/tasks/{workflowId}/{ref}/COMPLETED} with
 * a JSON body, which becomes the result: {@code external("delivered").await(Delivery.class,
 * Duration.ofDays(1))}. Like {@link HumanCall#await}, the result type and the timeout are given
 * when waiting.
 */
public final class ExternalWait {

    private final Command command;

    ExternalWait(String ref) {
        command = new Command(Command.Kind.WAIT, ref, ref);
        command.explicitRef = ref;
        command.optional = true;
        command.emptyIsEmpty = true;
    }

    /** The posted body as {@code type}, or empty when nothing arrives within the timeout. */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> await(Class<T> type, Duration timeout) {
        Command c = command.copy();
        c.resultType = type;
        c.input.put("duration", Conductors.duration(timeout));
        c.seconds = timeout.toSeconds();
        return (Optional<T>) Engine.current().call(c);
    }
}
