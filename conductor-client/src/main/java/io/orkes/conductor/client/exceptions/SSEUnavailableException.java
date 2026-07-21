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
package io.orkes.conductor.client.exceptions;

/**
 * Thrown when the server rejects SSE streaming outright (non-2xx or transport
 * failure on the initial connect). Mirrors {@code SSEUnavailableError} in the
 * Python SDK. Callers should degrade to status polling rather than treating
 * the stream as silently empty.
 */
public class SSEUnavailableException extends AgentException {

    public SSEUnavailableException(String message) {
        super(message);
    }

    public SSEUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
