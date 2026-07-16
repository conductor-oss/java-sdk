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
package org.conductoross.conductor.ai.termination;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Terminates when the agent output mentions a specific text.
 */
public class TextMentionTermination extends TerminationCondition {
    private final String text;
    private final boolean caseSensitive;

    public TextMentionTermination(String text) {
        this(text, false);
    }

    public TextMentionTermination(String text, boolean caseSensitive) {
        this.text = text;
        this.caseSensitive = caseSensitive;
    }

    /** Create a TextMentionTermination for the given text (case-insensitive). */
    public static TextMentionTermination of(String text) {
        return new TextMentionTermination(text, false);
    }

    /** Create a TextMentionTermination with explicit case sensitivity. */
    public static TextMentionTermination of(String text, boolean caseSensitive) {
        return new TextMentionTermination(text, caseSensitive);
    }

    public String getText() {
        return text;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "text_mention");
        map.put("text", text);
        map.put("caseSensitive", caseSensitive);
        return map;
    }
}
