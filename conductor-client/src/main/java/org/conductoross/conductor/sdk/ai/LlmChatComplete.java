/*
 * Copyright 2026 Conductor Authors.
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
package org.conductoross.conductor.sdk.ai;

import java.util.List;
import java.util.Map;

import com.netflix.conductor.common.metadata.SchemaDef;
import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.sdk.workflow.def.tasks.Task;

import lombok.Getter;

/**
 * Task for LLM chat completion.
 * Corresponds to server-side task type {@code LLM_CHAT_COMPLETE} (model: {@code ChatCompletion}
 * extending {@code LLMWorkerInput}).
 */
@Getter
public class LlmChatComplete extends Task<LlmChatComplete> {

    public static final String TASK_TYPE_LLM_CHAT_COMPLETE = "LLM_CHAT_COMPLETE";

    // ── LLMWorkerInput base fields ────────────────────────────────────────────

    /** Named LLM provider integration (shorthand; maps to the "AI_MODEL" key in integrationNames). */
    private String llmProvider;
    /** Direct integration name (alternative to llmProvider). */
    private String integrationName;
    /** Map of role → integration name (e.g. "AI_MODEL" → "openai"). */
    private Map<String, String> integrationNames;
    private String model;

    /** Prompt template version. */
    private Integer promptVersion;
    private Object promptVariables;

    // Sampling / decoding
    private Double temperature;
    private Double frequencyPenalty;
    private Double topP;
    private Integer topK;
    private Double presencePenalty;
    private List<String> stopWords;
    private Integer maxTokens;
    /** Maximum number of results to return (default 1). */
    private Integer maxResults;
    /** Allow raw (unformatted) prompts to be passed directly to the model. */
    private Boolean allowRawPrompts;

    // ── ChatCompletion-specific fields ────────────────────────────────────────

    /** System-level instructions; maps to server-side {@code instructions} / {@code getPrompt()}. */
    private String instructions;

    /** Conversation history. Accepts {@code List<ChatMessage>} or a workflow expression string. */
    private Object messages;

    private String userInput;

    /** Map of participant name → role string (e.g. "assistant" → "assistant"). */
    private Map<String, String> participants;

    // Output control
    private Boolean jsonOutput;
    private String outputMimeType;
    private String outputLocation;

    /** Optional schema the prompt inputs must conform to. */
    private SchemaDef inputSchema;
    /** Optional schema the LLM output is validated against (used with jsonOutput). */
    private SchemaDef outputSchema;

    /** Tools available to the LLM. Use {@link ToolSpec} for Conductor-native tools. */
    private List<ToolSpec> tools;

    // Extended capabilities
    private Boolean googleSearchRetrieval;
    /** Token allowance for thinking (Anthropic models). */
    private Integer thinkingTokenLimit;
    /** Reasoning effort: "low", "medium", or "high" (OpenAI o-series models). */
    private String reasoningEffort;
    private String voice;

    public LlmChatComplete(String taskDefName, String taskReferenceName) {
        super(taskReferenceName, TaskType.LLM_CHAT_COMPLETE);
        super.name(taskDefName);
    }

    LlmChatComplete(WorkflowTask workflowTask) {
        super(workflowTask);
    }

    // ── Fluent setters ────────────────────────────────────────────────────────

    public LlmChatComplete llmProvider(String llmProvider) { this.llmProvider = llmProvider; return this; }
    public LlmChatComplete integrationName(String integrationName) { this.integrationName = integrationName; return this; }
    public LlmChatComplete integrationNames(Map<String, String> integrationNames) { this.integrationNames = integrationNames; return this; }
    public LlmChatComplete model(String model) { this.model = model; return this; }
    public LlmChatComplete promptVersion(Integer promptVersion) { this.promptVersion = promptVersion; return this; }
    public LlmChatComplete promptVariables(Object promptVariables) { this.promptVariables = promptVariables; return this; }
    public LlmChatComplete temperature(Double temperature) { this.temperature = temperature; return this; }
    public LlmChatComplete frequencyPenalty(Double frequencyPenalty) { this.frequencyPenalty = frequencyPenalty; return this; }
    public LlmChatComplete topP(Double topP) { this.topP = topP; return this; }
    public LlmChatComplete topK(Integer topK) { this.topK = topK; return this; }
    public LlmChatComplete presencePenalty(Double presencePenalty) { this.presencePenalty = presencePenalty; return this; }
    public LlmChatComplete stopWords(List<String> stopWords) { this.stopWords = stopWords; return this; }
    public LlmChatComplete maxTokens(Integer maxTokens) { this.maxTokens = maxTokens; return this; }
    public LlmChatComplete maxResults(Integer maxResults) { this.maxResults = maxResults; return this; }
    public LlmChatComplete allowRawPrompts(Boolean allowRawPrompts) { this.allowRawPrompts = allowRawPrompts; return this; }
    public LlmChatComplete instructions(String instructions) { this.instructions = instructions; return this; }
    public LlmChatComplete messages(Object messages) { this.messages = messages; return this; }
    public LlmChatComplete userInput(String userInput) { this.userInput = userInput; return this; }
    public LlmChatComplete participants(Map<String, String> participants) { this.participants = participants; return this; }
    public LlmChatComplete jsonOutput(Boolean jsonOutput) { this.jsonOutput = jsonOutput; return this; }
    public LlmChatComplete outputMimeType(String outputMimeType) { this.outputMimeType = outputMimeType; return this; }
    public LlmChatComplete outputLocation(String outputLocation) { this.outputLocation = outputLocation; return this; }
    public LlmChatComplete inputSchema(SchemaDef inputSchema) { this.inputSchema = inputSchema; return this; }
    public LlmChatComplete outputSchema(SchemaDef outputSchema) { this.outputSchema = outputSchema; return this; }
    public LlmChatComplete tools(List<ToolSpec> tools) { this.tools = tools; return this; }
    public LlmChatComplete googleSearchRetrieval(Boolean googleSearchRetrieval) { this.googleSearchRetrieval = googleSearchRetrieval; return this; }
    public LlmChatComplete thinkingTokenLimit(Integer thinkingTokenLimit) { this.thinkingTokenLimit = thinkingTokenLimit; return this; }
    public LlmChatComplete reasoningEffort(String reasoningEffort) { this.reasoningEffort = reasoningEffort; return this; }
    public LlmChatComplete voice(String voice) { this.voice = voice; return this; }

    @Override
    protected void updateWorkflowTask(WorkflowTask workflowTask) {
        workflowTask.setType(TASK_TYPE_LLM_CHAT_COMPLETE);

        // LLMWorkerInput base
        if (llmProvider != null)        workflowTask.getInputParameters().put("llmProvider", llmProvider);
        if (integrationName != null)    workflowTask.getInputParameters().put("integrationName", integrationName);
        if (integrationNames != null)   workflowTask.getInputParameters().put("integrationNames", integrationNames);
        if (model != null)              workflowTask.getInputParameters().put("model", model);
        if (promptVersion != null)      workflowTask.getInputParameters().put("promptVersion", promptVersion);
        if (promptVariables != null)    workflowTask.getInputParameters().put("promptVariables", promptVariables);
        if (temperature != null)        workflowTask.getInputParameters().put("temperature", temperature);
        if (frequencyPenalty != null)   workflowTask.getInputParameters().put("frequencyPenalty", frequencyPenalty);
        if (topP != null)               workflowTask.getInputParameters().put("topP", topP);
        if (topK != null)               workflowTask.getInputParameters().put("topK", topK);
        if (presencePenalty != null)    workflowTask.getInputParameters().put("presencePenalty", presencePenalty);
        if (stopWords != null)          workflowTask.getInputParameters().put("stopWords", stopWords);
        if (maxTokens != null)          workflowTask.getInputParameters().put("maxTokens", maxTokens);
        if (maxResults != null)         workflowTask.getInputParameters().put("maxResults", maxResults);
        if (allowRawPrompts != null)    workflowTask.getInputParameters().put("allowRawPrompts", allowRawPrompts);

        // ChatCompletion-specific
        if (instructions != null)           workflowTask.getInputParameters().put("instructions", instructions);
        if (messages != null)               workflowTask.getInputParameters().put("messages", messages);
        if (userInput != null)              workflowTask.getInputParameters().put("userInput", userInput);
        if (participants != null)           workflowTask.getInputParameters().put("participants", participants);
        if (jsonOutput != null)             workflowTask.getInputParameters().put("jsonOutput", jsonOutput);
        if (outputMimeType != null)         workflowTask.getInputParameters().put("outputMimeType", outputMimeType);
        if (outputLocation != null)         workflowTask.getInputParameters().put("outputLocation", outputLocation);
        if (inputSchema != null)            workflowTask.getInputParameters().put("inputSchema", inputSchema);
        if (outputSchema != null)           workflowTask.getInputParameters().put("outputSchema", outputSchema);
        if (tools != null)                  workflowTask.getInputParameters().put("tools", tools);
        if (googleSearchRetrieval != null)  workflowTask.getInputParameters().put("googleSearchRetrieval", googleSearchRetrieval);
        if (thinkingTokenLimit != null)     workflowTask.getInputParameters().put("thinkingTokenLimit", thinkingTokenLimit);
        if (reasoningEffort != null)        workflowTask.getInputParameters().put("reasoningEffort", reasoningEffort);
        if (voice != null)                  workflowTask.getInputParameters().put("voice", voice);
    }

}
