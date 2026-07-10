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
package org.conductoross.conductor.ai.internal;

import java.util.Map;

/**
 * Internal per-call transport for resolved secrets, from {@code WorkerManager} (which
 * resolves declared credentials before invoking a handler) to {@code ToolRegistry}
 * (which snapshots them into the {@code ToolContext} it builds for the call).
 *
 * <p>Not part of the public API — tool code reads secrets via
 * {@code ToolContext.getCredential(...)}, never from here. A {@link ThreadLocal} is used
 * (rather than the input map) so secrets never enter task input/output that may be logged
 * or serialized. {@code WorkerManager} sets the context immediately before invoking the
 * handler and clears it in a {@code finally}, on the same worker thread; concurrent worker
 * threads therefore see independent contexts and cannot leak across each other.
 */
public final class CredentialContext {

    private static final ThreadLocal<Map<String, String>> CURRENT = new ThreadLocal<>();

    private CredentialContext() {}

    /** Establish the per-call secret context (no-op clear when empty/null). */
    public static void set(Map<String, String> credentials) {
        if (credentials == null || credentials.isEmpty()) {
            CURRENT.remove();
        } else {
            CURRENT.set(Map.copyOf(credentials));
        }
    }

    /** Clear the per-call secret context. Always safe to call. */
    public static void clear() {
        CURRENT.remove();
    }

    /** The current call's resolved secrets, or an empty map if none. */
    public static Map<String, String> current() {
        Map<String, String> ctx = CURRENT.get();
        return ctx == null ? Map.of() : ctx;
    }
}
