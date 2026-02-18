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
 * LLM Index Document task for indexing documents into a vector database for RAG pipelines.
 * <p>
 * This task converts documents to embeddings and stores them in a vector database,
 * enabling semantic search capabilities for retrieval-augmented generation (RAG).
 * 
 * <p>Example usage:
 * <pre>
 * LlmIndexDocument indexTask = new LlmIndexDocument("index_docs", "index_ref")
 *     .vectorDb("pinecone")
 *     .namespace("my-documents")
 *     .index("knowledge-base")
 *     .embeddingModelProvider("openai")
 *     .embeddingModel("text-embedding-ada-002")
 *     .text("${workflow.input.document_text}")
 *     .docId("${workflow.input.doc_id}")
 *     .metadata(Map.of("source", "user-upload"));
 * 
 * workflow.add(indexTask);
 * </pre>
 */
public class LlmIndexDocument extends Task<LlmIndexDocument> {

    /** Task type identifier for LLM document indexing tasks */
    public static final String TASK_TYPE_LLM_INDEX_DOCUMENT = "LLM_INDEX_DOCUMENT";

    private String vectorDb;
    private String namespace;
    private String index;
    private String embeddingModelProvider;
    private String embeddingModel;
    private Object text;
    private Object docId;
    private Object metadata;
    private Integer chunkSize;
    private Integer chunkOverlap;

    /**
     * Creates a new LLM Index Document task.
     *
     * @param taskDefName the task definition name
     * @param taskReferenceName the unique reference name for this task in the workflow
     */
    public LlmIndexDocument(String taskDefName, String taskReferenceName) {
        super(taskReferenceName, TaskType.LLM_INDEX_DOCUMENT);
        super.name(taskDefName);
    }

    LlmIndexDocument(WorkflowTask workflowTask) {
        super(workflowTask);
    }

    /**
     * Sets the vector database integration name.
     *
     * @param vectorDb the vector DB integration (e.g., "pinecone", "weaviate", "pgvector")
     * @return this task for method chaining
     */
    public LlmIndexDocument vectorDb(String vectorDb) {
        this.vectorDb = vectorDb;
        return this;
    }

    /**
     * Sets the namespace within the vector database.
     *
     * @param namespace the namespace for organizing documents
     * @return this task for method chaining
     */
    public LlmIndexDocument namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    /**
     * Sets the index name in the vector database.
     *
     * @param index the index name
     * @return this task for method chaining
     */
    public LlmIndexDocument index(String index) {
        this.index = index;
        return this;
    }

    /**
     * Sets the embedding model provider integration name.
     *
     * @param embeddingModelProvider the provider (e.g., "openai", "cohere")
     * @return this task for method chaining
     */
    public LlmIndexDocument embeddingModelProvider(String embeddingModelProvider) {
        this.embeddingModelProvider = embeddingModelProvider;
        return this;
    }

    /**
     * Sets the embedding model to use.
     *
     * @param embeddingModel the model identifier (e.g., "text-embedding-ada-002")
     * @return this task for method chaining
     */
    public LlmIndexDocument embeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
        return this;
    }

    /**
     * Sets the text content to index.
     *
     * @param text the document text or workflow expression
     * @return this task for method chaining
     */
    public LlmIndexDocument text(Object text) {
        this.text = text;
        return this;
    }

    /**
     * Sets the document ID for the indexed content.
     *
     * @param docId unique document identifier
     * @return this task for method chaining
     */
    public LlmIndexDocument docId(Object docId) {
        this.docId = docId;
        return this;
    }

    /**
     * Sets metadata to store with the document.
     *
     * @param metadata key-value pairs of metadata
     * @return this task for method chaining
     */
    public LlmIndexDocument metadata(Object metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * Sets the chunk size for splitting large documents.
     *
     * @param chunkSize number of characters per chunk
     * @return this task for method chaining
     */
    public LlmIndexDocument chunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }

    /**
     * Sets the overlap between chunks.
     *
     * @param chunkOverlap number of overlapping characters between chunks
     * @return this task for method chaining
     */
    public LlmIndexDocument chunkOverlap(Integer chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
        return this;
    }

    @Override
    protected void updateWorkflowTask(WorkflowTask workflowTask) {
        workflowTask.setType(TASK_TYPE_LLM_INDEX_DOCUMENT);
        
        if (vectorDb != null) {
            workflowTask.getInputParameters().put("vectorDB", vectorDb);
        }
        if (namespace != null) {
            workflowTask.getInputParameters().put("namespace", namespace);
        }
        if (index != null) {
            workflowTask.getInputParameters().put("index", index);
        }
        if (embeddingModelProvider != null) {
            workflowTask.getInputParameters().put("embeddingModelProvider", embeddingModelProvider);
        }
        if (embeddingModel != null) {
            workflowTask.getInputParameters().put("embeddingModel", embeddingModel);
        }
        if (text != null) {
            workflowTask.getInputParameters().put("text", text);
        }
        if (docId != null) {
            workflowTask.getInputParameters().put("docId", docId);
        }
        if (metadata != null) {
            workflowTask.getInputParameters().put("metadata", metadata);
        }
        if (chunkSize != null) {
            workflowTask.getInputParameters().put("chunkSize", chunkSize);
        }
        if (chunkOverlap != null) {
            workflowTask.getInputParameters().put("chunkOverlap", chunkOverlap);
        }
    }

    // Getters
    public String getVectorDb() { return vectorDb; }
    public String getNamespace() { return namespace; }
    public String getIndex() { return index; }
    public String getEmbeddingModelProvider() { return embeddingModelProvider; }
    public String getEmbeddingModel() { return embeddingModel; }
    public Object getText() { return text; }
    public Object getDocId() { return docId; }
    public Object getMetadata() { return metadata; }
    public Integer getChunkSize() { return chunkSize; }
    public Integer getChunkOverlap() { return chunkOverlap; }
}
