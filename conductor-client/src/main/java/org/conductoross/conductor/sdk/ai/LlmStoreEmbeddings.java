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
 * Task for storing embeddings into a vector database.
 * Corresponds to server-side task type {@code LLM_STORE_EMBEDDINGS} (model: {@code StoreEmbeddingsInput}).
 */
@Getter
public class LlmStoreEmbeddings extends Task<LlmStoreEmbeddings> {

    public static final String TASK_TYPE_LLM_STORE_EMBEDDINGS = "LLM_STORE_EMBEDDINGS";

    // Embedding provider
    private String embeddingModelProvider;
    private String embeddingModel;

    // Vector DB target
    private String vectorDb;
    private String namespace;
    private String index;

    // Content
    private List<Float> embeddings;
    private Object id;

    // Additional
    private Object metadata;

    public LlmStoreEmbeddings(String taskDefName, String taskReferenceName) {
        super(taskReferenceName, TaskType.LLM_STORE_EMBEDDINGS);
        super.name(taskDefName);
    }

    LlmStoreEmbeddings(WorkflowTask workflowTask) {
        super(workflowTask);
    }

    public LlmStoreEmbeddings embeddingModelProvider(String embeddingModelProvider) { this.embeddingModelProvider = embeddingModelProvider; return this; }
    public LlmStoreEmbeddings embeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; return this; }
    public LlmStoreEmbeddings vectorDb(String vectorDb) { this.vectorDb = vectorDb; return this; }
    public LlmStoreEmbeddings namespace(String namespace) { this.namespace = namespace; return this; }
    public LlmStoreEmbeddings index(String index) { this.index = index; return this; }
    public LlmStoreEmbeddings embeddings(List<Float> embeddings) { this.embeddings = embeddings; return this; }
    public LlmStoreEmbeddings id(Object id) { this.id = id; return this; }
    public LlmStoreEmbeddings metadata(Object metadata) { this.metadata = metadata; return this; }

    @Override
    protected void updateWorkflowTask(WorkflowTask workflowTask) {
        workflowTask.setType(TASK_TYPE_LLM_STORE_EMBEDDINGS);
        if (embeddingModelProvider != null) workflowTask.getInputParameters().put("embeddingModelProvider", embeddingModelProvider);
        if (embeddingModel != null)         workflowTask.getInputParameters().put("embeddingModel", embeddingModel);
        if (vectorDb != null)               workflowTask.getInputParameters().put("vectorDB", vectorDb);
        if (namespace != null)              workflowTask.getInputParameters().put("namespace", namespace);
        if (index != null)                  workflowTask.getInputParameters().put("index", index);
        if (embeddings != null)             workflowTask.getInputParameters().put("embeddings", embeddings);
        if (id != null)                     workflowTask.getInputParameters().put("id", id);
        if (metadata != null)               workflowTask.getInputParameters().put("metadata", metadata);
    }

}
