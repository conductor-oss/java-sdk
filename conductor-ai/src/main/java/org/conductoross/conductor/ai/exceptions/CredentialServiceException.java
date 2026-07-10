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
 * Credential resolution service returned 5xx or was unreachable.
 *
 * <p>Treated as fatal — no env-var fallback. Mirrors Python's
 * {@code CredentialServiceError}.</p>
 */
public class CredentialServiceException extends AgentspanException {

    private final int statusCode;

    public CredentialServiceException(int statusCode, String detail) {
        super("Credential service error (HTTP " + statusCode + "): " + detail);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
