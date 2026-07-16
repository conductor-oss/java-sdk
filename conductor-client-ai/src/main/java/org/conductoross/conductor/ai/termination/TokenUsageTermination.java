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
 * Terminates when a token usage threshold is exceeded.
 */
public class TokenUsageTermination extends TerminationCondition {
    private final Integer maxTotalTokens;
    private final Integer maxPromptTokens;
    private final Integer maxCompletionTokens;

    private TokenUsageTermination(Integer maxTotalTokens, Integer maxPromptTokens, Integer maxCompletionTokens) {
        this.maxTotalTokens = maxTotalTokens;
        this.maxPromptTokens = maxPromptTokens;
        this.maxCompletionTokens = maxCompletionTokens;
    }

    /** Terminate when total tokens exceed the limit. */
    public static TokenUsageTermination ofTotal(int maxTotalTokens) {
        return new TokenUsageTermination(maxTotalTokens, null, null);
    }

    /** Terminate when prompt tokens exceed the limit. */
    public static TokenUsageTermination ofPrompt(int maxPromptTokens) {
        return new TokenUsageTermination(null, maxPromptTokens, null);
    }

    /** Terminate when completion tokens exceed the limit. */
    public static TokenUsageTermination ofCompletion(int maxCompletionTokens) {
        return new TokenUsageTermination(null, null, maxCompletionTokens);
    }

    public Integer getMaxTotalTokens() {
        return maxTotalTokens;
    }

    public Integer getMaxPromptTokens() {
        return maxPromptTokens;
    }

    public Integer getMaxCompletionTokens() {
        return maxCompletionTokens;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", "token_usage");
        if (maxTotalTokens != null) map.put("maxTotalTokens", maxTotalTokens);
        if (maxPromptTokens != null) map.put("maxPromptTokens", maxPromptTokens);
        if (maxCompletionTokens != null) map.put("maxCompletionTokens", maxCompletionTokens);
        return map;
    }
}
