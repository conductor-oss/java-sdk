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
 * Execution token rejected by {@code POST /api/workers/secrets} (HTTP 401).
 *
 * <p>Non-retryable. Token has expired, been revoked, or is structurally
 * invalid. Mirrors Python's {@code CredentialAuthError}.</p>
 */
public class CredentialAuthException extends AgentspanException {
    public CredentialAuthException(String detail) {
        super("Credential authentication failed (token expired or revoked): " + detail);
    }
}
