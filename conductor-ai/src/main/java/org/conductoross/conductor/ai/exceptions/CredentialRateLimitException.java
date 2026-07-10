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
package org.conductoross.conductor.ai.exceptions;

import io.orkes.conductor.client.exceptions.AgentspanException;

/**
 * Rate limit hit on {@code POST /api/workers/secrets} (HTTP 429).
 *
 * <p>Non-retryable from the worker's perspective — reduce resolve frequency
 * or raise the server-side limit.</p>
 */
public class CredentialRateLimitException extends AgentspanException {
    public CredentialRateLimitException() {
        super("Credential resolution rate limit exceeded (HTTP 429). "
                + "Reduce resolve frequency or increase the server rate limit.");
    }
}
