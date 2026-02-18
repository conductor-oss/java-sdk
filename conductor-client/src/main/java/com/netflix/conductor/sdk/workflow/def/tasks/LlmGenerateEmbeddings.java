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

import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;

/**
 * LLM Generate Embeddings task for converting text to vector embeddings.
 * <p>
 * This task converts text input into numerical vector representations (embeddings)
 * that can be used for semantic similarity, clustering, or storage in vector databases.
 * 
 * <p>Example usage:
 * <pre>
 * LlmGenerateEmbeddings embedTask = new LlmGenerateEmbeddings("generate_embeddings", "embed_ref")
 *     .llmProvider("openai")
 *     .model("text-embedding-ada-002")
 *     .text("${workflow.input.document_text}");
 * 
 * workflow.add(embedTask);
 * </pre>
 */
public class LlmGenerateEmbeddings extends Task<LlmGenerateEmbeddings> {

    /** Task type identifier for LLM embedding generation tasks */
    public static final String TASK_TYPE_LLM_GENERATE_EMBEDDINGS = "LLM_GENERATE_EMBEDDINGS";

    private String llmProvider;
    private String model;
    private Object text;

    /**
     * Creates a new LLM Generate Embeddings task.
     *
     * @param taskDefName the task definition name
     * @param taskReferenceName the unique reference name for this task in the workflow
     */
    public LlmGenerateEmbeddings(String taskDefName, String taskReferenceName) {
        super(taskReferenceName, TaskType.LLM_GENERATE_EMBEDDINGS);
        super.name(taskDefName);
    }

    LlmGenerateEmbeddings(WorkflowTask workflowTask) {
        super(workflowTask);
    }

    /**
     * Sets the LLM provider integration name.
     *
     * @param llmProvider the integration name (e.g., "openai", "cohere")
     * @return this task for method chaining
     */
    public LlmGenerateEmbeddings llmProvider(String llmProvider) {
        this.llmProvider = llmProvider;
        return this;
    }

    /**
     * Sets the embedding model to use.
     *
     * @param model the model identifier (e.g., "text-embedding-ada-002", "embed-english-v3.0")
     * @return this task for method chaining
     */
    public LlmGenerateEmbeddings model(String model) {
        this.model = model;
        return this;
    }

    /**
     * Sets the text to convert to embeddings.
     * <p>
     * Can be a string or a workflow expression that resolves to text.
     *
     * @param text the input text or workflow expression
     * @return this task for method chaining
     */
    public LlmGenerateEmbeddings text(Object text) {
        this.text = text;
        return this;
    }

    @Override
    protected void updateWorkflowTask(WorkflowTask workflowTask) {
        workflowTask.setType(TASK_TYPE_LLM_GENERATE_EMBEDDINGS);
        
        if (llmProvider != null) {
            workflowTask.getInputParameters().put("llmProvider", llmProvider);
        }
        if (model != null) {
            workflowTask.getInputParameters().put("model", model);
        }
        if (text != null) {
            workflowTask.getInputParameters().put("text", text);
        }
    }

    // Getters
    public String getLlmProvider() { return llmProvider; }
    public String getModel() { return model; }
    public Object getText() { return text; }
}
