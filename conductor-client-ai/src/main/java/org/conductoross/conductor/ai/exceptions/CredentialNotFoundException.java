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

import java.util.List;

import io.orkes.conductor.client.exceptions.AgentException;

/**
 * One or more declared credentials could not be resolved from the server.
 *
 * <p>Raised when a tool declares {@code credentials = {"X"}} but no value
 * for {@code X} exists in the user's secret store. Maps to a non-retryable
 * task failure — retrying won't fix a missing config.</p>
 *
 * <p>Mirrors Python's {@code CredentialNotFoundError} and .NET's
 * {@code CredentialNotFoundException}.</p>
 */
public class CredentialNotFoundException extends AgentException {

    private final List<String> missingNames;

    public CredentialNotFoundException(List<String> missingNames) {
        super("Required secrets not found: " + String.join(", ", missingNames));
        this.missingNames = List.copyOf(missingNames);
    }

    public CredentialNotFoundException(String singleName) {
        this(List.of(singleName));
    }

    public List<String> getMissingNames() {
        return missingNames;
    }
}
