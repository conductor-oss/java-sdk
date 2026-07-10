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

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for {@code POST /api/agent/{executionId}/respond}.
 *
 * <p>The server merges this body into the pending HUMAN task's output data. Three
 * patterns are supported:
 *
 * <ul>
 *   <li><b>Approve/reject</b> — standard HITL flows: use {@link #approve()},
 *       {@link #approve(String)}, {@link #reject(String)}.</li>
 *   <li><b>MANUAL strategy</b> — agent selection: use {@link #of(Map)} with
 *       e.g. {@code Map.of("selected", "writer")}.</li>
 *   <li><b>Custom schema</b> — arbitrary tool response schemas: use
 *       {@link #of(Map)} with the schema-defined keys.</li>
 * </ul>
 *
 * <p>{@code @JsonAnyGetter} / {@code @JsonAnySetter} flatten {@code extraFields}
 * into the top-level JSON object so all fields appear at the root level, matching
 * how the server reads the body as a plain {@code Map<String,Object>}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class RespondBody {

    @JsonProperty("approved")
    private final Boolean approved;

    @JsonProperty("reason")
    private final String reason;

    /** Arbitrary extra fields serialized at the top level via {@link JsonAnyGetter}. */
    private final Map<String, Object> extraFields;

    private RespondBody(Boolean approved, String reason, Map<String, Object> extraFields) {
        this.approved = approved;
        this.reason = reason;
        this.extraFields = extraFields;
    }

    // ── Factories ─────────────────────────────────────────────────────────

    /** Standard approval — sends {@code {"approved": true}}. */
    public static RespondBody approve() {
        return new RespondBody(true, null, null);
    }

    /** Approval with a human-readable comment — sends {@code {"approved": true, "reason": comment}}. */
    public static RespondBody approve(String comment) {
        return new RespondBody(true, comment != null && !comment.isEmpty() ? comment : null, null);
    }

    /** Rejection with a reason — sends {@code {"approved": false, "reason": reason}}. */
    public static RespondBody reject(String reason) {
        return new RespondBody(false, reason != null && !reason.isEmpty() ? reason : null, null);
    }

    /**
     * Arbitrary response body — wraps the given map directly. Used for MANUAL strategy
     * agent selection ({@code Map.of("selected", "writer")}) and custom HITL schemas.
     */
    public static RespondBody of(Map<String, Object> data) {
        return new RespondBody(null, null, data != null ? new LinkedHashMap<>(data) : null);
    }

    // ── Jackson ───────────────────────────────────────────────────────────

    @JsonAnyGetter
    public Map<String, Object> getExtraFields() {
        return extraFields;
    }

    @JsonAnySetter
    void setExtraField(String key, Object value) {
        // no-op: this class is write-only (we never deserialize RespondBody)
    }

    @Override
    public String toString() {
        if (extraFields != null) return "RespondBody" + extraFields;
        return "RespondBody{approved=" + approved + ", reason=" + reason + "}";
    }
}
