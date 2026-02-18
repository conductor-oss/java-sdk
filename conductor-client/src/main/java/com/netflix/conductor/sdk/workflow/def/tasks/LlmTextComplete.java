/*
 * Copyright 2024 Conductor Authors.
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
package com.netflix.conductor.sdk.workflow.def.tasks;

import java.util.List;

import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;

/**
 * LLM Text Completion task for generating text using large language models.
 * <p>
 * This task sends a prompt to an LLM provider and returns the generated text completion.
 * Supports various LLM providers (OpenAI, Azure OpenAI, Anthropic, etc.) configured through
 * Conductor's integration framework.
 * 
 * <p>Example usage:
 * <pre>
 * LlmTextComplete textTask = new LlmTextComplete("generate_summary", "summary_ref")
 *     .llmProvider("openai")
 *     .model("gpt-4")
 *     .promptName("summarization-prompt")
 *     .promptVariables(Map.of("text", "${workflow.input.document}"))
 *     .temperature(0.7)
 *     .maxTokens(500);
 * 
 * workflow.add(textTask);
 * </pre>
 */
public class LlmTextComplete extends Task<LlmTextComplete> {

    /** Task type identifier for LLM text completion tasks */
    public static final String TASK_TYPE_LLM_TEXT_COMPLETE = "LLM_TEXT_COMPLETE";

    private String llmProvider;
    private String model;
    private String promptName;
    private Object promptVariables;
    private Double temperature;
    private Double topP;
    private Integer maxTokens;
    private List<String> stopWords;

    /**
     * Creates a new LLM Text Completion task.
     *
     * @param taskDefName the task definition name
     * @param taskReferenceName the unique reference name for this task in the workflow
     */
    public LlmTextComplete(String taskDefName, String taskReferenceName) {
        super(taskReferenceName, TaskType.LLM_TEXT_COMPLETE);
        super.name(taskDefName);
    }

    LlmTextComplete(WorkflowTask workflowTask) {
        super(workflowTask);
    }

    /**
     * Sets the LLM provider integration name.
     *
     * @param llmProvider the integration name (e.g., "openai", "azure_openai", "anthropic")
     * @return this task for method chaining
     */
    public LlmTextComplete llmProvider(String llmProvider) {
        this.llmProvider = llmProvider;
        return this;
    }

    /**
     * Sets the model to use for text completion.
     *
     * @param model the model identifier (e.g., "gpt-4", "gpt-3.5-turbo", "claude-3-opus")
     * @return this task for method chaining
     */
    public LlmTextComplete model(String model) {
        this.model = model;
        return this;
    }

    /**
     * Sets the prompt template name to use.
     * <p>
     * The prompt template should be pre-registered using the Prompt Client API.
     *
     * @param promptName the name of the registered prompt template
     * @return this task for method chaining
     */
    public LlmTextComplete promptName(String promptName) {
        this.promptName = promptName;
        return this;
    }

    /**
     * Sets variables to substitute in the prompt template.
     *
     * @param promptVariables a map of variable names to values, or a workflow expression
     * @return this task for method chaining
     */
    public LlmTextComplete promptVariables(Object promptVariables) {
        this.promptVariables = promptVariables;
        return this;
    }

    /**
     * Sets the temperature for controlling randomness.
     * <p>
     * Higher values (e.g., 0.8) make output more random, lower values (e.g., 0.2) make it more deterministic.
     *
     * @param temperature value between 0.0 and 2.0 (default varies by provider)
     * @return this task for method chaining
     */
    public LlmTextComplete temperature(Double temperature) {
        this.temperature = temperature;
        return this;
    }

    /**
     * Sets the top-p (nucleus sampling) parameter.
     * <p>
     * The model considers tokens with top_p probability mass. 0.1 means only tokens 
     * comprising the top 10% probability mass are considered.
     *
     * @param topP value between 0.0 and 1.0
     * @return this task for method chaining
     */
    public LlmTextComplete topP(Double topP) {
        this.topP = topP;
        return this;
    }

    /**
     * Sets the maximum number of tokens to generate.
     *
     * @param maxTokens maximum tokens in the response
     * @return this task for method chaining
     */
    public LlmTextComplete maxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    /**
     * Sets stop words/sequences where the model should stop generating.
     *
     * @param stopWords list of stop sequences
     * @return this task for method chaining
     */
    public LlmTextComplete stopWords(List<String> stopWords) {
        this.stopWords = stopWords;
        return this;
    }

    @Override
    protected void updateWorkflowTask(WorkflowTask workflowTask) {
        workflowTask.setType(TASK_TYPE_LLM_TEXT_COMPLETE);
        
        if (llmProvider != null) {
            workflowTask.getInputParameters().put("llmProvider", llmProvider);
        }
        if (model != null) {
            workflowTask.getInputParameters().put("model", model);
        }
        if (promptName != null) {
            workflowTask.getInputParameters().put("promptName", promptName);
        }
        if (promptVariables != null) {
            workflowTask.getInputParameters().put("promptVariables", promptVariables);
        }
        if (temperature != null) {
            workflowTask.getInputParameters().put("temperature", temperature);
        }
        if (topP != null) {
            workflowTask.getInputParameters().put("topP", topP);
        }
        if (maxTokens != null) {
            workflowTask.getInputParameters().put("maxTokens", maxTokens);
        }
        if (stopWords != null) {
            workflowTask.getInputParameters().put("stopWords", stopWords);
        }
    }

    // Getters
    public String getLlmProvider() { return llmProvider; }
    public String getModel() { return model; }
    public String getPromptName() { return promptName; }
    public Object getPromptVariables() { return promptVariables; }
    public Double getTemperature() { return temperature; }
    public Double getTopP() { return topP; }
    public Integer getMaxTokens() { return maxTokens; }
    public List<String> getStopWords() { return stopWords; }
}
