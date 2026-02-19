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
 * Task for indexing text/documents into a vector database.
 * Corresponds to server-side task type {@code LLM_INDEX_TEXT} (model: {@code IndexDocInput}).
 */
@Getter
public class LlmIndexText extends Task<LlmIndexText> {

    public static final String TASK_TYPE_LLM_INDEX_TEXT = "LLM_INDEX_TEXT";

    // LLM / embedding provider
    private String llmProvider;
    private String embeddingModelProvider;
    private String embeddingModel;
    private String integrationName;

    // Vector DB target
    private String vectorDb;
    private String namespace;
    private String index;

    // Content
    private Object text;
    private Object docId;
    private String url;
    private String mediaType;

    // Chunking
    private Integer chunkSize;
    private Integer chunkOverlap;

    // Additional
    private Object metadata;
    private Integer dimensions;

    public LlmIndexText(String taskDefName, String taskReferenceName) {
        super(taskReferenceName, TaskType.LLM_INDEX_TEXT);
        super.name(taskDefName);
    }

    LlmIndexText(WorkflowTask workflowTask) {
        super(workflowTask);
    }

    public LlmIndexText llmProvider(String llmProvider) { this.llmProvider = llmProvider; return this; }
    public LlmIndexText embeddingModelProvider(String embeddingModelProvider) { this.embeddingModelProvider = embeddingModelProvider; return this; }
    public LlmIndexText embeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; return this; }
    public LlmIndexText integrationName(String integrationName) { this.integrationName = integrationName; return this; }
    public LlmIndexText vectorDb(String vectorDb) { this.vectorDb = vectorDb; return this; }
    public LlmIndexText namespace(String namespace) { this.namespace = namespace; return this; }
    public LlmIndexText index(String index) { this.index = index; return this; }
    public LlmIndexText text(Object text) { this.text = text; return this; }
    public LlmIndexText docId(Object docId) { this.docId = docId; return this; }
    public LlmIndexText url(String url) { this.url = url; return this; }
    public LlmIndexText mediaType(String mediaType) { this.mediaType = mediaType; return this; }
    public LlmIndexText chunkSize(Integer chunkSize) { this.chunkSize = chunkSize; return this; }
    public LlmIndexText chunkOverlap(Integer chunkOverlap) { this.chunkOverlap = chunkOverlap; return this; }
    public LlmIndexText metadata(Object metadata) { this.metadata = metadata; return this; }
    public LlmIndexText dimensions(Integer dimensions) { this.dimensions = dimensions; return this; }

    @Override
    protected void updateWorkflowTask(WorkflowTask workflowTask) {
        workflowTask.setType(TASK_TYPE_LLM_INDEX_TEXT);
        if (llmProvider != null)            workflowTask.getInputParameters().put("llmProvider", llmProvider);
        if (embeddingModelProvider != null) workflowTask.getInputParameters().put("embeddingModelProvider", embeddingModelProvider);
        if (embeddingModel != null)         workflowTask.getInputParameters().put("embeddingModel", embeddingModel);
        if (integrationName != null)        workflowTask.getInputParameters().put("integrationName", integrationName);
        if (vectorDb != null)               workflowTask.getInputParameters().put("vectorDB", vectorDb);
        if (namespace != null)              workflowTask.getInputParameters().put("namespace", namespace);
        if (index != null)                  workflowTask.getInputParameters().put("index", index);
        if (text != null)                   workflowTask.getInputParameters().put("text", text);
        if (docId != null)                  workflowTask.getInputParameters().put("docId", docId);
        if (url != null)                    workflowTask.getInputParameters().put("url", url);
        if (mediaType != null)              workflowTask.getInputParameters().put("mediaType", mediaType);
        if (chunkSize != null)              workflowTask.getInputParameters().put("chunkSize", chunkSize);
        if (chunkOverlap != null)           workflowTask.getInputParameters().put("chunkOverlap", chunkOverlap);
        if (metadata != null)               workflowTask.getInputParameters().put("metadata", metadata);
        if (dimensions != null)             workflowTask.getInputParameters().put("dimensions", dimensions);
    }

}
