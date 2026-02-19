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

import org.conductoross.conductor.sdk.ai.LlmChatComplete;
import org.conductoross.conductor.sdk.ai.LlmIndexText;
import org.conductoross.conductor.sdk.ai.LlmSearchIndex;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.sdk.workflow.def.ConductorWorkflow;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;

import io.orkes.conductor.client.http.OrkesPromptClient;
import io.orkes.conductor.sdk.examples.util.ClientUtil;

/**
 * Example demonstrating a Retrieval-Augmented Generation (RAG) pipeline with Conductor.
 * <p>
 * RAG combines document retrieval with LLM generation to provide accurate,
 * grounded responses. This example shows:
 * <ul>
 *   <li>Document indexing - Converting documents to embeddings and storing in vector DB</li>
 *   <li>Semantic search - Finding relevant documents based on user queries</li>
 *   <li>Answer generation - Using retrieved context to generate accurate responses</li>
 * </ul>
 * 
 * <p>Prerequisites:
 * <ul>
 *   <li>Conductor server with AI/LLM integrations configured</li>
 *   <li>Vector database integration (Pinecone, Weaviate, pgvector, etc.)</li>
 *   <li>Embedding model integration (OpenAI, Cohere, etc.)</li>
 *   <li>LLM integration for answer generation</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>
 * # Index documents and answer questions
 * ./gradlew :examples:run -PmainClass=io.orkes.conductor.sdk.examples.agentic.RagWorkflowExample
 * </pre>
 */
public class RagWorkflowExample {

    private static final String INDEX_WORKFLOW = "rag_index_documents";
    private static final String QUERY_WORKFLOW = "rag_query";
    private static final String LLM_PROVIDER = "openai";
    private static final String EMBEDDING_MODEL = "text-embedding-ada-002";
    private static final String CHAT_MODEL = "gpt-4o-mini";
    private static final String VECTOR_DB = "pinecone";
    private static final String INDEX_NAME = "knowledge-base";
    private static final String NAMESPACE = "rag-demo";

    public static void main(String[] args) {
        System.out.println("Starting RAG Workflow Example");
        System.out.println();

        // Initialize Conductor client
        ConductorClient client = ClientUtil.getClient();
        WorkflowExecutor executor = new WorkflowExecutor(client, 10);

        try {
            // Register prompts
            registerPrompts(client);
            
            // Create and register workflows
            ConductorWorkflow<Map<String, Object>> indexWorkflow = createIndexingWorkflow(executor);
            indexWorkflow.registerWorkflow(true, true);
            System.out.println("Registered: " + INDEX_WORKFLOW);

            ConductorWorkflow<Map<String, Object>> queryWorkflow = createQueryWorkflow(executor);
            queryWorkflow.registerWorkflow(true, true);
            System.out.println("Registered: " + QUERY_WORKFLOW);

            // Index some sample documents
            System.out.println("\n=== Indexing Documents ===");
            indexSampleDocuments(indexWorkflow);

            // Query the knowledge base
            System.out.println("\n=== Querying Knowledge Base ===");
            String question = args.length > 0 ? String.join(" ", args) : 
                "What are the key features of Conductor?";
            
            Map<String, Object> queryInput = new HashMap<>();
            queryInput.put("question", question);
            queryInput.put("llmProvider", LLM_PROVIDER);
            queryInput.put("embeddingModel", EMBEDDING_MODEL);
            queryInput.put("chatModel", CHAT_MODEL);
            queryInput.put("vectorDb", VECTOR_DB);
            queryInput.put("index", INDEX_NAME);
            queryInput.put("namespace", NAMESPACE);
            queryInput.put("topK", 3);

            System.out.println("Question: " + question);
            
            var queryRun = queryWorkflow.execute(queryInput);
            Workflow result = queryRun.get(60, TimeUnit.SECONDS);
            
            System.out.println("\nStatus: " + result.getStatus());
            System.out.println("\n=== Answer ===");
            System.out.println(result.getOutput().get("answer"));
            
            System.out.println("\n=== Sources Used ===");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> sources = (List<Map<String, Object>>) result.getOutput().get("sources");
            if (sources != null) {
                for (int i = 0; i < sources.size(); i++) {
                    Map<String, Object> source = sources.get(i);
                    System.out.println((i + 1) + ". " + source.get("title"));
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
     * Creates a workflow for indexing documents into the vector database.
     */
    private static ConductorWorkflow<Map<String, Object>> createIndexingWorkflow(WorkflowExecutor executor) {
        ConductorWorkflow<Map<String, Object>> workflow = new ConductorWorkflow<>(executor);
        workflow.setName(INDEX_WORKFLOW);
        workflow.setVersion(1);
        workflow.setOwnerEmail("examples@conductor-oss.org");
        workflow.setDescription("Index documents into vector database for RAG");

        // Index document task
        LlmIndexText indexTask = new LlmIndexText("index_document", "index_ref")
            .vectorDb("${workflow.input.vectorDb}")
            .namespace("${workflow.input.namespace}")
            .index("${workflow.input.index}")
            .embeddingModelProvider("${workflow.input.llmProvider}")
            .embeddingModel("${workflow.input.embeddingModel}")
            .text("${workflow.input.documentText}")
            .docId("${workflow.input.documentId}")
            .metadata("${workflow.input.metadata}")
            .chunkSize(500)
            .chunkOverlap(50);

        workflow.add(indexTask);

        // Set output
        Map<String, Object> outputParams = new HashMap<>();
        outputParams.put("documentId", "${workflow.input.documentId}");
        outputParams.put("status", "indexed");
        outputParams.put("chunksCreated", "${index_ref.output.chunksCreated}");
        workflow.setWorkflowOutput(outputParams);

        return workflow;
    }

    /**
     * Creates a workflow for querying the knowledge base and generating answers.
     */
    private static ConductorWorkflow<Map<String, Object>> createQueryWorkflow(WorkflowExecutor executor) {
        ConductorWorkflow<Map<String, Object>> workflow = new ConductorWorkflow<>(executor);
        workflow.setName(QUERY_WORKFLOW);
        workflow.setVersion(1);
        workflow.setOwnerEmail("examples@conductor-oss.org");
        workflow.setDescription("Query knowledge base and generate RAG response");

        // Step 1: Search vector database for relevant documents
        LlmSearchIndex searchTask = new LlmSearchIndex("search_knowledge", "search_ref")
            .vectorDb("${workflow.input.vectorDb}")
            .namespace("${workflow.input.namespace}")
            .index("${workflow.input.index}")
            .embeddingModelProvider("${workflow.input.llmProvider}")
            .embeddingModel("${workflow.input.embeddingModel}")
            .query("${workflow.input.question}")
            .maxResults(5);

        workflow.add(searchTask);

        // Step 2: Generate answer using retrieved context
        LlmChatComplete generateAnswer = new LlmChatComplete("generate_answer", "answer_ref")
            .llmProvider("${workflow.input.llmProvider}")
            .model("${workflow.input.chatModel}")
            .instructions("rag-answer-prompt")
            .promptVariables(Map.of(
                "question", "${workflow.input.question}",
                "context", "${search_ref.output.results}"
            ))
            .temperature(0.3)
            .maxTokens(500);

        workflow.add(generateAnswer);

        // Set output
        Map<String, Object> outputParams = new HashMap<>();
        outputParams.put("answer", "${answer_ref.output.result}");
        outputParams.put("sources", "${search_ref.output.results}");
        outputParams.put("question", "${workflow.input.question}");
        workflow.setWorkflowOutput(outputParams);

        return workflow;
    }

    /**
     * Index sample documents about Conductor.
     */
    private static void indexSampleDocuments(ConductorWorkflow<Map<String, Object>> indexWorkflow) 
            throws Exception {
        List<Map<String, String>> documents = List.of(
            Map.of(
                "id", "doc-1",
                "title", "Conductor Overview",
                "text", "Conductor is a platform for orchestrating microservices and managing workflows. " +
                    "It provides visibility and control over workflows, with features like automatic retries, " +
                    "rate limiting, and comprehensive monitoring. Conductor was originally developed at Netflix " +
                    "and is now an open-source project."
            ),
            Map.of(
                "id", "doc-2",
                "title", "Conductor Workers",
                "text", "Workers in Conductor are responsible for executing tasks. They poll the Conductor " +
                    "server for tasks, execute them, and report results back. Workers can be written in any " +
                    "language and can run anywhere - in containers, VMs, or serverless functions. Workers " +
                    "are stateless and can be scaled horizontally."
            ),
            Map.of(
                "id", "doc-3",
                "title", "Conductor AI Features",
                "text", "Conductor supports AI-native workflows including LLM integration, RAG pipelines, " +
                    "and agentic workflows. You can build AI agents where LLMs dynamically select tools, " +
                    "create multi-turn conversations, and implement retrieval-augmented generation for " +
                    "knowledge-grounded responses."
            )
        );

        for (Map<String, String> doc : documents) {
            Map<String, Object> input = new HashMap<>();
            input.put("documentId", doc.get("id"));
            input.put("documentText", doc.get("text"));
            input.put("metadata", Map.of("title", doc.get("title")));
            input.put("llmProvider", LLM_PROVIDER);
            input.put("embeddingModel", EMBEDDING_MODEL);
            input.put("vectorDb", VECTOR_DB);
            input.put("index", INDEX_NAME);
            input.put("namespace", NAMESPACE);

            try {
                var run = indexWorkflow.execute(input);
                Workflow result = run.get(30, TimeUnit.SECONDS);
                System.out.println("Indexed: " + doc.get("title") + " - " + result.getStatus());
            } catch (Exception e) {
                System.out.println("Failed to index " + doc.get("title") + ": " + e.getMessage());
            }
        }
    }

    /**
     * Registers prompt templates.
     */
    private static void registerPrompts(ConductorClient client) {
        OrkesPromptClient promptClient = new OrkesPromptClient(client);

        String ragPrompt = "You are a helpful assistant that answers questions based on the provided context.\n\n" +
            "Context from knowledge base:\n{{context}}\n\n" +
            "Question: {{question}}\n\n" +
            "Instructions:\n" +
            "- Answer based ONLY on the provided context\n" +
            "- If the context doesn't contain enough information, say so\n" +
            "- Be concise and accurate\n" +
            "- Cite which document(s) you used in your answer\n\n" +
            "Answer:";
        
        try {
            promptClient.savePrompt("rag-answer-prompt", 
                "Prompt for RAG answer generation", ragPrompt);
            System.out.println("Registered prompt: rag-answer-prompt");
        } catch (Exception e) {
            System.out.println("Prompt may already exist: " + e.getMessage());
        }
    }
}
