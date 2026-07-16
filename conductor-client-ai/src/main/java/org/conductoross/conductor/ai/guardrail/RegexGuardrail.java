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
package org.conductoross.conductor.ai.guardrail;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.ai.enums.OnFail;
import org.conductoross.conductor.ai.enums.Position;
import org.conductoross.conductor.ai.model.GuardrailDef;

/**
 * A guardrail that validates content against regex patterns.
 *
 * <p>In {@code "block"} mode (default) the guardrail fails if any pattern matches the content.
 * In {@code "allow"} mode it fails if NO pattern matches.
 *
 * <p>Serialized as {@code guardrailType: "regex"} — the Conductor server evaluates the patterns.
 * No worker process is needed.
 *
 * <pre>{@code
 * // Block emails in output
 * GuardrailDef noPii = RegexGuardrail.builder()
 *     .name("no_pii")
 *     .patterns("[\\w.+-]+@[\\w-]+\\.[\\w.-]+")
 *     .message("Response must not contain email addresses.")
 *     .build();
 *
 * // Only allow JSON output
 * GuardrailDef jsonOnly = RegexGuardrail.builder()
 *     .name("json_output")
 *     .patterns("^\\s*[\\{\\[]")
 *     .mode("allow")
 *     .message("Response must be valid JSON.")
 *     .build();
 * }</pre>
 */
public class RegexGuardrail {

    private RegexGuardrail() {}

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name = "regex_guardrail";
        private List<String> patterns;
        private String mode = "block";
        private Position position = Position.OUTPUT;
        private OnFail onFail = OnFail.RAISE;
        private int maxRetries = 3;
        private String message;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder patterns(List<String> patterns) {
            this.patterns = patterns;
            return this;
        }

        public Builder patterns(String... patterns) {
            this.patterns = Arrays.asList(patterns);
            return this;
        }

        /** {@code "block"} (default) or {@code "allow"}. */
        public Builder mode(String mode) {
            this.mode = mode;
            return this;
        }

        public Builder position(Position position) {
            this.position = position;
            return this;
        }

        public Builder onFail(OnFail onFail) {
            this.onFail = onFail;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public GuardrailDef build() {
            if (patterns == null || patterns.isEmpty()) {
                throw new IllegalArgumentException("RegexGuardrail requires at least one pattern");
            }
            if (!mode.equals("block") && !mode.equals("allow")) {
                throw new IllegalArgumentException("mode must be 'block' or 'allow', got: " + mode);
            }
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("patterns", patterns);
            config.put("mode", mode);
            if (message != null) config.put("message", message);
            return GuardrailDef.builder()
                    .name(name)
                    .position(position)
                    .onFail(onFail)
                    .maxRetries(maxRetries)
                    .guardrailType("regex")
                    .config(config)
                    .build();
        }
    }
}
