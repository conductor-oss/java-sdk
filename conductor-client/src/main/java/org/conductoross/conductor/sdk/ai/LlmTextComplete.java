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

import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.sdk.workflow.def.tasks.Task;

import lombok.Getter;

/**
 * Task for LLM text completion using a named prompt template.
 * Corresponds to server-side task type {@code LLM_TEXT_COMPLETE} (model: {@code TextCompletion}
 * extending {@code LLMWorkerInput}).
 */
@Getter
public class LlmTextComplete extends Task<LlmTextComplete> {

    public static final String TASK_TYPE_LLM_TEXT_COMPLETE = "LLM_TEXT_COMPLETE";

    // ── LLMWorkerInput base fields ────────────────────────────────────────────

    /** Named LLM provider integration (shorthand; maps to the "AI_MODEL" key in integrationNames). */
    private String llmProvider;
    /** Direct integration name (alternative to llmProvider). */
    private String integrationName;
    /** Map of role → integration name (e.g. "AI_MODEL" → "openai"). */
    private Map<String, String> integrationNames;
    private String model;

    private Object promptVariables;
    /** Version of the prompt template. */
    private Integer promptVersion;

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

    // ── TextCompletion-specific fields ────────────────────────────────────────

    /** Conductor prompt template name. */
    private String promptName;
    private Boolean jsonOutput;

    public LlmTextComplete(String taskDefName, String taskReferenceName) {
        super(taskReferenceName, TaskType.LLM_TEXT_COMPLETE);
        super.name(taskDefName);
    }

    LlmTextComplete(WorkflowTask workflowTask) {
        super(workflowTask);
    }

    // ── Fluent setters ────────────────────────────────────────────────────────

    public LlmTextComplete llmProvider(String llmProvider) { this.llmProvider = llmProvider; return this; }
    public LlmTextComplete integrationName(String integrationName) { this.integrationName = integrationName; return this; }
    public LlmTextComplete integrationNames(Map<String, String> integrationNames) { this.integrationNames = integrationNames; return this; }
    public LlmTextComplete model(String model) { this.model = model; return this; }
    public LlmTextComplete promptVariables(Object promptVariables) { this.promptVariables = promptVariables; return this; }
    public LlmTextComplete promptVersion(Integer promptVersion) { this.promptVersion = promptVersion; return this; }
    public LlmTextComplete temperature(Double temperature) { this.temperature = temperature; return this; }
    public LlmTextComplete frequencyPenalty(Double frequencyPenalty) { this.frequencyPenalty = frequencyPenalty; return this; }
    public LlmTextComplete topP(Double topP) { this.topP = topP; return this; }
    public LlmTextComplete topK(Integer topK) { this.topK = topK; return this; }
    public LlmTextComplete presencePenalty(Double presencePenalty) { this.presencePenalty = presencePenalty; return this; }
    public LlmTextComplete stopWords(List<String> stopWords) { this.stopWords = stopWords; return this; }
    public LlmTextComplete maxTokens(Integer maxTokens) { this.maxTokens = maxTokens; return this; }
    public LlmTextComplete maxResults(Integer maxResults) { this.maxResults = maxResults; return this; }
    public LlmTextComplete allowRawPrompts(Boolean allowRawPrompts) { this.allowRawPrompts = allowRawPrompts; return this; }
    public LlmTextComplete promptName(String promptName) { this.promptName = promptName; return this; }
    public LlmTextComplete jsonOutput(Boolean jsonOutput) { this.jsonOutput = jsonOutput; return this; }

    @Override
    protected void updateWorkflowTask(WorkflowTask workflowTask) {
        workflowTask.setType(TASK_TYPE_LLM_TEXT_COMPLETE);

        // LLMWorkerInput base
        if (llmProvider != null)        workflowTask.getInputParameters().put("llmProvider", llmProvider);
        if (integrationName != null)    workflowTask.getInputParameters().put("integrationName", integrationName);
        if (integrationNames != null)   workflowTask.getInputParameters().put("integrationNames", integrationNames);
        if (model != null)              workflowTask.getInputParameters().put("model", model);
        if (promptVariables != null)    workflowTask.getInputParameters().put("promptVariables", promptVariables);
        if (promptVersion != null)      workflowTask.getInputParameters().put("promptVersion", promptVersion);
        if (temperature != null)        workflowTask.getInputParameters().put("temperature", temperature);
        if (frequencyPenalty != null)   workflowTask.getInputParameters().put("frequencyPenalty", frequencyPenalty);
        if (topP != null)               workflowTask.getInputParameters().put("topP", topP);
        if (topK != null)               workflowTask.getInputParameters().put("topK", topK);
        if (presencePenalty != null)    workflowTask.getInputParameters().put("presencePenalty", presencePenalty);
        if (stopWords != null)          workflowTask.getInputParameters().put("stopWords", stopWords);
        if (maxTokens != null)          workflowTask.getInputParameters().put("maxTokens", maxTokens);
        if (maxResults != null)         workflowTask.getInputParameters().put("maxResults", maxResults);
        if (allowRawPrompts != null)    workflowTask.getInputParameters().put("allowRawPrompts", allowRawPrompts);

        // TextCompletion-specific
        if (promptName != null)         workflowTask.getInputParameters().put("promptName", promptName);
        if (jsonOutput != null)         workflowTask.getInputParameters().put("jsonOutput", jsonOutput);
    }

}
