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
package io.orkes.conductor.sdk.examples.agentic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.sdk.workflow.def.ConductorWorkflow;
import com.netflix.conductor.sdk.workflow.def.tasks.LlmGenerateEmbeddings;
import com.netflix.conductor.sdk.workflow.def.tasks.LlmIndexDocument;
import com.netflix.conductor.sdk.workflow.def.tasks.LlmSearchIndex;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;

import io.orkes.conductor.sdk.examples.util.ClientUtil;

/**
 * Example demonstrating vector database operations with Conductor.
 * <p>
 * This example shows the fundamental vector database operations:
 * <ul>
 *   <li>Text to embedding conversion</li>
 *   <li>Storing embeddings in a vector database</li>
 *   <li>Semantic similarity search</li>
 * </ul>
 * 
 * <p>These operations are the building blocks for:
 * <ul>
 *   <li>Retrieval-Augmented Generation (RAG)</li>
 *   <li>Semantic search applications</li>
 *   <li>Document clustering and classification</li>
 *   <li>Recommendation systems</li>
 * </ul>
 * 
 * <p>Prerequisites:
 * <ul>
 *   <li>Conductor server with vector database integration configured</li>
 *   <li>Embedding model integration (OpenAI, Cohere, etc.)</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>
 * ./gradlew :examples:run -PmainClass=io.orkes.conductor.sdk.examples.agentic.VectorDbExample
 * </pre>
 */
public class VectorDbExample {

    private static final String EMBEDDING_WORKFLOW = "generate_embeddings";
    private static final String INDEX_WORKFLOW = "vector_index";
    private static final String SEARCH_WORKFLOW = "vector_search";
    private static final String LLM_PROVIDER = "openai";
    private static final String EMBEDDING_MODEL = "text-embedding-ada-002";
    private static final String VECTOR_DB = "pinecone";
    private static final String INDEX_NAME = "demo-index";
    private static final String NAMESPACE = "vector-demo";

    public static void main(String[] args) {
        System.out.println("Starting Vector Database Example");
        System.out.println();

        // Initialize Conductor client
        ConductorClient client = ClientUtil.getClient();
        WorkflowExecutor executor = new WorkflowExecutor(client, 10);

        try {
            // Create and register all workflows
            ConductorWorkflow<Map<String, Object>> embeddingWorkflow = createEmbeddingWorkflow(executor);
            embeddingWorkflow.registerWorkflow(true, true);
            System.out.println("Registered: " + EMBEDDING_WORKFLOW);

            ConductorWorkflow<Map<String, Object>> indexWorkflow = createIndexWorkflow(executor);
            indexWorkflow.registerWorkflow(true, true);
            System.out.println("Registered: " + INDEX_WORKFLOW);

            ConductorWorkflow<Map<String, Object>> searchWorkflow = createSearchWorkflow(executor);
            searchWorkflow.registerWorkflow(true, true);
            System.out.println("Registered: " + SEARCH_WORKFLOW);

            // Demo 1: Generate embeddings for a text
            System.out.println("\n=== Demo 1: Generate Embeddings ===");
            String sampleText = "Conductor is an orchestration platform for microservices";
            
            Map<String, Object> embeddingInput = new HashMap<>();
            embeddingInput.put("text", sampleText);
            embeddingInput.put("llmProvider", LLM_PROVIDER);
            embeddingInput.put("model", EMBEDDING_MODEL);

            System.out.println("Input text: " + sampleText);
            
            var embeddingRun = embeddingWorkflow.execute(embeddingInput);
            Workflow embeddingResult = embeddingRun.get(30, TimeUnit.SECONDS);
            
            System.out.println("Status: " + embeddingResult.getStatus());
            @SuppressWarnings("unchecked")
            List<Double> embeddings = (List<Double>) embeddingResult.getOutput().get("embeddings");
            if (embeddings != null) {
                System.out.println("Embedding dimensions: " + embeddings.size());
                System.out.println("First 5 values: " + embeddings.subList(0, Math.min(5, embeddings.size())));
            }

            // Demo 2: Index documents
            System.out.println("\n=== Demo 2: Index Documents ===");
            List<Map<String, String>> documents = List.of(
                Map.of("id", "1", "text", "The quick brown fox jumps over the lazy dog"),
                Map.of("id", "2", "text", "A fast auburn fox leaps above a sleepy canine"),
                Map.of("id", "3", "text", "The weather today is sunny with clear skies"),
                Map.of("id", "4", "text", "Machine learning is a subset of artificial intelligence"),
                Map.of("id", "5", "text", "Deep learning uses neural networks with many layers")
            );

            for (Map<String, String> doc : documents) {
                Map<String, Object> indexInput = new HashMap<>();
                indexInput.put("documentId", doc.get("id"));
                indexInput.put("text", doc.get("text"));
                indexInput.put("llmProvider", LLM_PROVIDER);
                indexInput.put("embeddingModel", EMBEDDING_MODEL);
                indexInput.put("vectorDb", VECTOR_DB);
                indexInput.put("index", INDEX_NAME);
                indexInput.put("namespace", NAMESPACE);
                indexInput.put("metadata", Map.of("source", "demo"));

                try {
                    var indexRun = indexWorkflow.execute(indexInput);
                    Workflow indexResult = indexRun.get(30, TimeUnit.SECONDS);
                    System.out.println("Indexed doc " + doc.get("id") + ": " + indexResult.getStatus());
                } catch (Exception e) {
                    System.out.println("Failed to index doc " + doc.get("id") + ": " + e.getMessage());
                }
            }

            // Demo 3: Semantic search
            System.out.println("\n=== Demo 3: Semantic Search ===");
            String searchQuery = "animals jumping";
            
            Map<String, Object> searchInput = new HashMap<>();
            searchInput.put("query", searchQuery);
            searchInput.put("llmProvider", LLM_PROVIDER);
            searchInput.put("embeddingModel", EMBEDDING_MODEL);
            searchInput.put("vectorDb", VECTOR_DB);
            searchInput.put("index", INDEX_NAME);
            searchInput.put("namespace", NAMESPACE);
            searchInput.put("topK", 3);

            System.out.println("Search query: " + searchQuery);
            
            var searchRun = searchWorkflow.execute(searchInput);
            Workflow searchResult = searchRun.get(30, TimeUnit.SECONDS);
            
            System.out.println("Status: " + searchResult.getStatus());
            System.out.println("\nSearch Results:");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) searchResult.getOutput().get("results");
            if (results != null) {
                for (int i = 0; i < results.size(); i++) {
                    Map<String, Object> result = results.get(i);
                    System.out.println((i + 1) + ". Score: " + result.get("score") + 
                        " - " + result.get("text"));
                }
            }

            // Demo 4: Another semantic search
            System.out.println("\n=== Demo 4: Another Semantic Search ===");
            searchQuery = "AI and machine learning";
            searchInput.put("query", searchQuery);
            
            System.out.println("Search query: " + searchQuery);
            
            searchRun = searchWorkflow.execute(searchInput);
            searchResult = searchRun.get(30, TimeUnit.SECONDS);
            
            System.out.println("Status: " + searchResult.getStatus());
            System.out.println("\nSearch Results:");
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results2 = (List<Map<String, Object>>) searchResult.getOutput().get("results");
            if (results2 != null) {
                for (int i = 0; i < results2.size(); i++) {
                    Map<String, Object> result = results2.get(i);
                    System.out.println((i + 1) + ". Score: " + result.get("score") + 
                        " - " + result.get("text"));
                }
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Creates a workflow for generating embeddings from text.
     */
    private static ConductorWorkflow<Map<String, Object>> createEmbeddingWorkflow(WorkflowExecutor executor) {
        ConductorWorkflow<Map<String, Object>> workflow = new ConductorWorkflow<>(executor);
        workflow.setName(EMBEDDING_WORKFLOW);
        workflow.setVersion(1);
        workflow.setOwnerEmail("examples@conductor-oss.org");
        workflow.setDescription("Generate embeddings from text");

        LlmGenerateEmbeddings embedTask = new LlmGenerateEmbeddings("generate_embeddings", "embed_ref")
            .llmProvider("${workflow.input.llmProvider}")
            .model("${workflow.input.model}")
            .text("${workflow.input.text}");

        workflow.add(embedTask);

        Map<String, Object> outputParams = new HashMap<>();
        outputParams.put("embeddings", "${embed_ref.output.embeddings}");
        outputParams.put("model", "${workflow.input.model}");
        outputParams.put("inputText", "${workflow.input.text}");
        workflow.setWorkflowOutput(outputParams);

        return workflow;
    }

    /**
     * Creates a workflow for indexing text into a vector database.
     */
    private static ConductorWorkflow<Map<String, Object>> createIndexWorkflow(WorkflowExecutor executor) {
        ConductorWorkflow<Map<String, Object>> workflow = new ConductorWorkflow<>(executor);
        workflow.setName(INDEX_WORKFLOW);
        workflow.setVersion(1);
        workflow.setOwnerEmail("examples@conductor-oss.org");
        workflow.setDescription("Index text into vector database");

        LlmIndexDocument indexTask = new LlmIndexDocument("index_text", "index_ref")
            .vectorDb("${workflow.input.vectorDb}")
            .namespace("${workflow.input.namespace}")
            .index("${workflow.input.index}")
            .embeddingModelProvider("${workflow.input.llmProvider}")
            .embeddingModel("${workflow.input.embeddingModel}")
            .text("${workflow.input.text}")
            .docId("${workflow.input.documentId}")
            .metadata("${workflow.input.metadata}");

        workflow.add(indexTask);

        Map<String, Object> outputParams = new HashMap<>();
        outputParams.put("documentId", "${workflow.input.documentId}");
        outputParams.put("indexed", true);
        workflow.setWorkflowOutput(outputParams);

        return workflow;
    }

    /**
     * Creates a workflow for semantic search in a vector database.
     */
    private static ConductorWorkflow<Map<String, Object>> createSearchWorkflow(WorkflowExecutor executor) {
        ConductorWorkflow<Map<String, Object>> workflow = new ConductorWorkflow<>(executor);
        workflow.setName(SEARCH_WORKFLOW);
        workflow.setVersion(1);
        workflow.setOwnerEmail("examples@conductor-oss.org");
        workflow.setDescription("Semantic search in vector database");

        LlmSearchIndex searchTask = new LlmSearchIndex("search_vectors", "search_ref")
            .vectorDb("${workflow.input.vectorDb}")
            .namespace("${workflow.input.namespace}")
            .index("${workflow.input.index}")
            .embeddingModelProvider("${workflow.input.llmProvider}")
            .embeddingModel("${workflow.input.embeddingModel}")
            .query("${workflow.input.query}")
            .topK(5);

        workflow.add(searchTask);

        Map<String, Object> outputParams = new HashMap<>();
        outputParams.put("results", "${search_ref.output.results}");
        outputParams.put("query", "${workflow.input.query}");
        workflow.setWorkflowOutput(outputParams);

        return workflow;
    }
}
