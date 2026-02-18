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
 * LLM Search Index task for performing semantic search in a vector database.
 * <p>
 * This task converts a query to embeddings and searches for similar documents
 * in a vector database, returning the most relevant results for RAG pipelines.
 * 
 * <p>Example usage:
 * <pre>
 * LlmSearchIndex searchTask = new LlmSearchIndex("search_docs", "search_ref")
 *     .vectorDb("pinecone")
 *     .namespace("my-documents")
 *     .index("knowledge-base")
 *     .embeddingModelProvider("openai")
 *     .embeddingModel("text-embedding-ada-002")
 *     .query("${workflow.input.user_question}")
 *     .topK(5);
 * 
 * workflow.add(searchTask);
 * </pre>
 */
public class LlmSearchIndex extends Task<LlmSearchIndex> {

    /** Task type identifier for LLM search index tasks */
    public static final String TASK_TYPE_LLM_SEARCH_INDEX = "LLM_SEARCH_INDEX";

    private String vectorDb;
    private String namespace;
    private String index;
    private String embeddingModelProvider;
    private String embeddingModel;
    private Object query;
    private Integer topK;
    private Object filter;

    /**
     * Creates a new LLM Search Index task.
     *
     * @param taskDefName the task definition name
     * @param taskReferenceName the unique reference name for this task in the workflow
     */
    public LlmSearchIndex(String taskDefName, String taskReferenceName) {
        super(taskReferenceName, TaskType.LLM_SEARCH_INDEX);
        super.name(taskDefName);
    }

    LlmSearchIndex(WorkflowTask workflowTask) {
        super(workflowTask);
    }

    /**
     * Sets the vector database integration name.
     *
     * @param vectorDb the vector DB integration (e.g., "pinecone", "weaviate", "pgvector")
     * @return this task for method chaining
     */
    public LlmSearchIndex vectorDb(String vectorDb) {
        this.vectorDb = vectorDb;
        return this;
    }

    /**
     * Sets the namespace within the vector database.
     *
     * @param namespace the namespace to search in
     * @return this task for method chaining
     */
    public LlmSearchIndex namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    /**
     * Sets the index name in the vector database.
     *
     * @param index the index name to search
     * @return this task for method chaining
     */
    public LlmSearchIndex index(String index) {
        this.index = index;
        return this;
    }

    /**
     * Sets the embedding model provider integration name.
     *
     * @param embeddingModelProvider the provider (e.g., "openai", "cohere")
     * @return this task for method chaining
     */
    public LlmSearchIndex embeddingModelProvider(String embeddingModelProvider) {
        this.embeddingModelProvider = embeddingModelProvider;
        return this;
    }

    /**
     * Sets the embedding model to use for query encoding.
     *
     * @param embeddingModel the model identifier (e.g., "text-embedding-ada-002")
     * @return this task for method chaining
     */
    public LlmSearchIndex embeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
        return this;
    }

    /**
     * Sets the search query text.
     *
     * @param query the query text or workflow expression
     * @return this task for method chaining
     */
    public LlmSearchIndex query(Object query) {
        this.query = query;
        return this;
    }

    /**
     * Sets the number of top results to return.
     *
     * @param topK number of results (default varies by provider)
     * @return this task for method chaining
     */
    public LlmSearchIndex topK(Integer topK) {
        this.topK = topK;
        return this;
    }

    /**
     * Sets a filter to apply to the search results.
     * <p>
     * Filter format depends on the vector database provider.
     *
     * @param filter metadata filter expression
     * @return this task for method chaining
     */
    public LlmSearchIndex filter(Object filter) {
        this.filter = filter;
        return this;
    }

    @Override
    protected void updateWorkflowTask(WorkflowTask workflowTask) {
        workflowTask.setType(TASK_TYPE_LLM_SEARCH_INDEX);
        
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
        if (query != null) {
            workflowTask.getInputParameters().put("query", query);
        }
        if (topK != null) {
            workflowTask.getInputParameters().put("topK", topK);
        }
        if (filter != null) {
            workflowTask.getInputParameters().put("filter", filter);
        }
    }

    // Getters
    public String getVectorDb() { return vectorDb; }
    public String getNamespace() { return namespace; }
    public String getIndex() { return index; }
    public String getEmbeddingModelProvider() { return embeddingModelProvider; }
    public String getEmbeddingModel() { return embeddingModel; }
    public Object getQuery() { return query; }
    public Integer getTopK() { return topK; }
    public Object getFilter() { return filter; }
}
