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

import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.sdk.workflow.def.tasks.Task;

import lombok.Getter;

/**
 * Task for retrieving embeddings from a vector database.
 * Corresponds to server-side task type {@code LLM_GET_EMBEDDINGS} (model: {@code VectorDBInput}).
 */
@Getter
public class LlmGetEmbeddings extends Task<LlmGetEmbeddings> {

    public static final String TASK_TYPE_LLM_GET_EMBEDDINGS = "LLM_GET_EMBEDDINGS";

    // LLM / embedding provider
    private String llmProvider;
    private String embeddingModelProvider;
    private String embeddingModel;

    // Vector DB target
    private String vectorDb;
    private String namespace;
    private String index;

    // Query
    private Object query;
    private List<Float> embeddings;

    // Result control
    private Integer maxResults;

    // Additional
    private Object metadata;
    private Integer dimensions;

    public LlmGetEmbeddings(String taskDefName, String taskReferenceName) {
        super(taskReferenceName, TaskType.LLM_GET_EMBEDDINGS);
        super.name(taskDefName);
    }

    LlmGetEmbeddings(WorkflowTask workflowTask) {
        super(workflowTask);
    }

    public LlmGetEmbeddings llmProvider(String llmProvider) { this.llmProvider = llmProvider; return this; }
    public LlmGetEmbeddings embeddingModelProvider(String embeddingModelProvider) { this.embeddingModelProvider = embeddingModelProvider; return this; }
    public LlmGetEmbeddings embeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; return this; }
    public LlmGetEmbeddings vectorDb(String vectorDb) { this.vectorDb = vectorDb; return this; }
    public LlmGetEmbeddings namespace(String namespace) { this.namespace = namespace; return this; }
    public LlmGetEmbeddings index(String index) { this.index = index; return this; }
    public LlmGetEmbeddings query(Object query) { this.query = query; return this; }
    public LlmGetEmbeddings embeddings(List<Float> embeddings) { this.embeddings = embeddings; return this; }
    public LlmGetEmbeddings maxResults(Integer maxResults) { this.maxResults = maxResults; return this; }
    public LlmGetEmbeddings metadata(Object metadata) { this.metadata = metadata; return this; }
    public LlmGetEmbeddings dimensions(Integer dimensions) { this.dimensions = dimensions; return this; }

    @Override
    protected void updateWorkflowTask(WorkflowTask workflowTask) {
        workflowTask.setType(TASK_TYPE_LLM_GET_EMBEDDINGS);
        if (llmProvider != null)            workflowTask.getInputParameters().put("llmProvider", llmProvider);
        if (embeddingModelProvider != null) workflowTask.getInputParameters().put("embeddingModelProvider", embeddingModelProvider);
        if (embeddingModel != null)         workflowTask.getInputParameters().put("embeddingModel", embeddingModel);
        if (vectorDb != null)               workflowTask.getInputParameters().put("vectorDB", vectorDb);
        if (namespace != null)              workflowTask.getInputParameters().put("namespace", namespace);
        if (index != null)                  workflowTask.getInputParameters().put("index", index);
        if (query != null)                  workflowTask.getInputParameters().put("query", query);
        if (embeddings != null)             workflowTask.getInputParameters().put("embeddings", embeddings);
        if (maxResults != null)             workflowTask.getInputParameters().put("maxResults", maxResults);
        if (metadata != null)               workflowTask.getInputParameters().put("metadata", metadata);
        if (dimensions != null)             workflowTask.getInputParameters().put("dimensions", dimensions);
    }

}
