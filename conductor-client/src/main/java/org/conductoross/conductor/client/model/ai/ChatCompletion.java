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
package org.conductoross.conductor.client.model.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.netflix.conductor.common.metadata.SchemaDef;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ChatCompletion extends LLMWorkerInput {

    public static final String NAME = "LLM_CHAT_COMPLETE";

    private String instructions;

    private List<ChatMessage> messages = new ArrayList<>();

    private boolean jsonOutput;

    private boolean googleSearchRetrieval;

    private SchemaDef inputSchema;

    private SchemaDef outputSchema;

    private String userInput;

    private List<ToolSpec> tools = new ArrayList<>();

    private Map<String, ChatMessage.Role> participants = Map.of();

    // refers to HTTP content type
    private String outputMimeType;

    // Used for thinking models
    private int thinkingTokenLimit;

    private String reasoningEffort;

    private String outputLocation;

    // Audio output
    private String voice;

    public String getPrompt() {
        return instructions;
    }
}
