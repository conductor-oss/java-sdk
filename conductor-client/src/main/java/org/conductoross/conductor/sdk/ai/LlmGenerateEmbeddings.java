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

import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.sdk.workflow.def.tasks.Task;

import lombok.Getter;

/**
 * Task for generating embeddings from text.
 * Corresponds to server-side task type {@code LLM_GENERATE_EMBEDDINGS} (model: {@code EmbeddingGenRequest}).
 */
@Getter
public class LlmGenerateEmbeddings extends Task<LlmGenerateEmbeddings> {

    public static final String TASK_TYPE_LLM_GENERATE_EMBEDDINGS = "LLM_GENERATE_EMBEDDINGS";

    private String llmProvider;
    private String model;
    private Object text;
    private Integer dimensions;

    public LlmGenerateEmbeddings(String taskDefName, String taskReferenceName) {
        super(taskReferenceName, TaskType.LLM_GENERATE_EMBEDDINGS);
        super.name(taskDefName);
    }

    LlmGenerateEmbeddings(WorkflowTask workflowTask) {
        super(workflowTask);
    }

    public LlmGenerateEmbeddings llmProvider(String llmProvider) { this.llmProvider = llmProvider; return this; }
    public LlmGenerateEmbeddings model(String model) { this.model = model; return this; }
    public LlmGenerateEmbeddings text(Object text) { this.text = text; return this; }
    public LlmGenerateEmbeddings dimensions(Integer dimensions) { this.dimensions = dimensions; return this; }

    @Override
    protected void updateWorkflowTask(WorkflowTask workflowTask) {
        workflowTask.setType(TASK_TYPE_LLM_GENERATE_EMBEDDINGS);
        if (llmProvider != null) workflowTask.getInputParameters().put("llmProvider", llmProvider);
        if (model != null)       workflowTask.getInputParameters().put("model", model);
        if (text != null)        workflowTask.getInputParameters().put("text", text);
        if (dimensions != null)  workflowTask.getInputParameters().put("dimensions", dimensions);
    }

}
