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
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.netflix.conductor.common.metadata.SchemaDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for all AI task builder classes and {@link ToolSpec}.
 * <p>
 * For each task builder we verify:
 * <ol>
 *   <li>Fluent API: every setter stores the value, and the Lombok/manual getter returns it.</li>
 *   <li>WorkflowTask serialisation: {@code getWorkflowDefTasks()} produces a single
 *       {@link WorkflowTask} with the correct {@code type} and every non-null field present
 *       in {@code inputParameters}.</li>
 *   <li>Null-guard: a freshly constructed task (no setters) yields an <em>empty</em>
 *       {@code inputParameters} map.</li>
 * </ol>
 */
public class AiTaskBuilderTests {

    static {
        WorkflowExecutor.initTaskImplementations();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Shorthand: get the single WorkflowTask from a builder. */
    private static WorkflowTask singleTask(
            com.netflix.conductor.sdk.workflow.def.tasks.Task<?> task) {
        List<WorkflowTask> tasks = task.getWorkflowDefTasks();
        assertEquals(1, tasks.size(), "Expected exactly one WorkflowTask");
        return tasks.get(0);
    }

    /** Assert that the inputParameters map contains the expected key/value. */
    private static void assertParam(Map<String, Object> params, String key, Object expected) {
        assertTrue(params.containsKey(key), "Missing key: " + key);
        assertEquals(expected, params.get(key), "Mismatch for key: " + key);
    }

    /** Assert that the inputParameters map does NOT contain the given key. */
    private static void assertNoParam(Map<String, Object> params, String key) {
        assertFalse(params.containsKey(key), "Key should be absent: " + key);
    }

    // Shared test fixtures
    private static final SchemaDef SAMPLE_INPUT_SCHEMA =
            new SchemaDef("input_schema", SchemaDef.Type.JSON, Map.of("type", "object"), null);
    private static final SchemaDef SAMPLE_OUTPUT_SCHEMA =
            new SchemaDef("output_schema", SchemaDef.Type.JSON, Map.of("type", "string"), null);

    private static final ToolSpec SAMPLE_TOOL = new ToolSpec()
            .name("get_weather")
            .type("SIMPLE")
            .description("Returns weather");

    // ═════════════════════════════════════════════════════════════════════════
    //  LlmChatComplete
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    public void testLlmChatCompleteFluentApi() {
        Map<String, String> intNames = Map.of("AI_MODEL", "openai");
        Map<String, Object> promptVars = Map.of("name", "Alice");
        List<String> stops = List.of("END", "STOP");
        List<Map<String, String>> msgs = List.of(Map.of("role", "user", "content", "hi"));
        Map<String, String> participants = Map.of("assistant", "assistant");
        List<ToolSpec> tools = List.of(SAMPLE_TOOL);

        LlmChatComplete task = new LlmChatComplete("def", "ref")
                .llmProvider("openai")
                .integrationName("openai_int")
                .integrationNames(intNames)
                .model("gpt-4")
                .promptVersion(2)
                .promptVariables(promptVars)
                .temperature(0.7)
                .frequencyPenalty(0.5)
                .topP(0.9)
                .topK(40)
                .presencePenalty(0.3)
                .stopWords(stops)
                .maxTokens(1024)
                .maxResults(3)
                .allowRawPrompts(true)
                .instructions("Be concise")
                .messages(msgs)
                .userInput("Hello")
                .participants(participants)
                .jsonOutput(true)
                .outputMimeType("application/json")
                .outputLocation("s3://bucket/out")
                .inputSchema(SAMPLE_INPUT_SCHEMA)
                .outputSchema(SAMPLE_OUTPUT_SCHEMA)
                .tools(tools)
                .googleSearchRetrieval(true)
                .thinkingTokenLimit(500)
                .reasoningEffort("high")
                .voice("alloy");

        assertEquals("openai", task.getLlmProvider());
        assertEquals("openai_int", task.getIntegrationName());
        assertSame(intNames, task.getIntegrationNames());
        assertEquals("gpt-4", task.getModel());
        assertEquals(2, task.getPromptVersion());
        assertSame(promptVars, task.getPromptVariables());
        assertEquals(0.7, task.getTemperature());
        assertEquals(0.5, task.getFrequencyPenalty());
        assertEquals(0.9, task.getTopP());
        assertEquals(40, task.getTopK());
        assertEquals(0.3, task.getPresencePenalty());
        assertSame(stops, task.getStopWords());
        assertEquals(1024, task.getMaxTokens());
        assertEquals(3, task.getMaxResults());
        assertTrue(task.getAllowRawPrompts());
        assertEquals("Be concise", task.getInstructions());
        assertSame(msgs, task.getMessages());
        assertEquals("Hello", task.getUserInput());
        assertSame(participants, task.getParticipants());
        assertTrue(task.getJsonOutput());
        assertEquals("application/json", task.getOutputMimeType());
        assertEquals("s3://bucket/out", task.getOutputLocation());
        assertSame(SAMPLE_INPUT_SCHEMA, task.getInputSchema());
        assertSame(SAMPLE_OUTPUT_SCHEMA, task.getOutputSchema());
        assertSame(tools, task.getTools());
        assertTrue(task.getGoogleSearchRetrieval());
        assertEquals(500, task.getThinkingTokenLimit());
        assertEquals("high", task.getReasoningEffort());
        assertEquals("alloy", task.getVoice());
    }

    @Test
    public void testLlmChatCompleteWorkflowTask() {
        Map<String, String> intNames = Map.of("AI_MODEL", "openai");
        Map<String, Object> promptVars = Map.of("x", 1);
        List<String> stops = List.of(".");
        List<ToolSpec> tools = List.of(SAMPLE_TOOL);
        Map<String, String> participants = Map.of("user", "human");

        LlmChatComplete task = new LlmChatComplete("def", "ref")
                .llmProvider("openai")
                .integrationName("int1")
                .integrationNames(intNames)
                .model("gpt-4")
                .promptVersion(1)
                .promptVariables(promptVars)
                .temperature(0.5)
                .frequencyPenalty(0.1)
                .topP(0.8)
                .topK(50)
                .presencePenalty(0.2)
                .stopWords(stops)
                .maxTokens(512)
                .maxResults(1)
                .allowRawPrompts(false)
                .instructions("inst")
                .messages("${msgs}")
                .userInput("input")
                .participants(participants)
                .jsonOutput(false)
                .outputMimeType("text/plain")
                .outputLocation("loc")
                .inputSchema(SAMPLE_INPUT_SCHEMA)
                .outputSchema(SAMPLE_OUTPUT_SCHEMA)
                .tools(tools)
                .googleSearchRetrieval(false)
                .thinkingTokenLimit(100)
                .reasoningEffort("low")
                .voice("echo");

        WorkflowTask wt = singleTask(task);
        assertEquals("LLM_CHAT_COMPLETE", wt.getType());

        Map<String, Object> p = wt.getInputParameters();
        assertParam(p, "llmProvider", "openai");
        assertParam(p, "integrationName", "int1");
        assertParam(p, "integrationNames", intNames);
        assertParam(p, "model", "gpt-4");
        assertParam(p, "promptVersion", 1);
        assertParam(p, "promptVariables", promptVars);
        assertParam(p, "temperature", 0.5);
        assertParam(p, "frequencyPenalty", 0.1);
        assertParam(p, "topP", 0.8);
        assertParam(p, "topK", 50);
        assertParam(p, "presencePenalty", 0.2);
        assertParam(p, "stopWords", stops);
        assertParam(p, "maxTokens", 512);
        assertParam(p, "maxResults", 1);
        assertParam(p, "allowRawPrompts", false);
        assertParam(p, "instructions", "inst");
        assertParam(p, "messages", "${msgs}");
        assertParam(p, "userInput", "input");
        assertParam(p, "participants", participants);
        assertParam(p, "jsonOutput", false);
        assertParam(p, "outputMimeType", "text/plain");
        assertParam(p, "outputLocation", "loc");
        assertParam(p, "inputSchema", SAMPLE_INPUT_SCHEMA);
        assertParam(p, "outputSchema", SAMPLE_OUTPUT_SCHEMA);
        assertParam(p, "tools", tools);
        assertParam(p, "googleSearchRetrieval", false);
        assertParam(p, "thinkingTokenLimit", 100);
        assertParam(p, "reasoningEffort", "low");
        assertParam(p, "voice", "echo");
    }

    @Test
    public void testLlmChatCompleteNullGuard() {
        WorkflowTask wt = singleTask(new LlmChatComplete("def", "ref"));
        assertEquals("LLM_CHAT_COMPLETE", wt.getType());
        assertTrue(wt.getInputParameters().isEmpty(),
                "Expected empty inputParameters but got: " + wt.getInputParameters());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  LlmTextComplete
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    public void testLlmTextCompleteFluentApi() {
        Map<String, String> intNames = Map.of("AI_MODEL", "anthropic");
        Map<String, Object> promptVars = Map.of("topic", "weather");
        List<String> stops = List.of("\n");

        LlmTextComplete task = new LlmTextComplete("def", "ref")
                .llmProvider("anthropic")
                .integrationName("ant_int")
                .integrationNames(intNames)
                .model("claude-3")
                .promptVariables(promptVars)
                .promptVersion(3)
                .temperature(0.9)
                .frequencyPenalty(0.4)
                .topP(0.95)
                .topK(30)
                .presencePenalty(0.1)
                .stopWords(stops)
                .maxTokens(2048)
                .maxResults(5)
                .allowRawPrompts(true)
                .promptName("weather_prompt")
                .jsonOutput(false);

        assertEquals("anthropic", task.getLlmProvider());
        assertEquals("ant_int", task.getIntegrationName());
        assertSame(intNames, task.getIntegrationNames());
        assertEquals("claude-3", task.getModel());
        assertSame(promptVars, task.getPromptVariables());
        assertEquals(3, task.getPromptVersion());
        assertEquals(0.9, task.getTemperature());
        assertEquals(0.4, task.getFrequencyPenalty());
        assertEquals(0.95, task.getTopP());
        assertEquals(30, task.getTopK());
        assertEquals(0.1, task.getPresencePenalty());
        assertSame(stops, task.getStopWords());
        assertEquals(2048, task.getMaxTokens());
        assertEquals(5, task.getMaxResults());
        assertTrue(task.getAllowRawPrompts());
        assertEquals("weather_prompt", task.getPromptName());
        assertFalse(task.getJsonOutput());
    }

    @Test
    public void testLlmTextCompleteWorkflowTask() {
        Map<String, String> intNames = Map.of("AI_MODEL", "cohere");
        List<String> stops = List.of("END");

        LlmTextComplete task = new LlmTextComplete("def", "ref")
                .llmProvider("cohere")
                .integrationName("co_int")
                .integrationNames(intNames)
                .model("command")
                .promptVariables("${vars}")
                .promptVersion(1)
                .temperature(0.6)
                .frequencyPenalty(0.0)
                .topP(1.0)
                .topK(0)
                .presencePenalty(0.0)
                .stopWords(stops)
                .maxTokens(100)
                .maxResults(1)
                .allowRawPrompts(false)
                .promptName("my_prompt")
                .jsonOutput(true);

        WorkflowTask wt = singleTask(task);
        assertEquals("LLM_TEXT_COMPLETE", wt.getType());

        Map<String, Object> p = wt.getInputParameters();
        assertParam(p, "llmProvider", "cohere");
        assertParam(p, "integrationName", "co_int");
        assertParam(p, "integrationNames", intNames);
        assertParam(p, "model", "command");
        assertParam(p, "promptVariables", "${vars}");
        assertParam(p, "promptVersion", 1);
        assertParam(p, "temperature", 0.6);
        assertParam(p, "frequencyPenalty", 0.0);
        assertParam(p, "topP", 1.0);
        assertParam(p, "topK", 0);
        assertParam(p, "presencePenalty", 0.0);
        assertParam(p, "stopWords", stops);
        assertParam(p, "maxTokens", 100);
        assertParam(p, "maxResults", 1);
        assertParam(p, "allowRawPrompts", false);
        assertParam(p, "promptName", "my_prompt");
        assertParam(p, "jsonOutput", true);
    }

    @Test
    public void testLlmTextCompleteNullGuard() {
        WorkflowTask wt = singleTask(new LlmTextComplete("def", "ref"));
        assertEquals("LLM_TEXT_COMPLETE", wt.getType());
        assertTrue(wt.getInputParameters().isEmpty());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  LlmIndexText
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    public void testLlmIndexTextFluentApi() {
        Map<String, Object> meta = Map.of("source", "web");

        LlmIndexText task = new LlmIndexText("def", "ref")
                .llmProvider("openai")
                .embeddingModelProvider("openai")
                .embeddingModel("text-embedding-3")
                .integrationName("oai_int")
                .vectorDb("pinecone")
                .namespace("ns1")
                .index("idx1")
                .text("Hello world")
                .docId("doc-123")
                .url("https://example.com/doc")
                .mediaType("text/plain")
                .chunkSize(500)
                .chunkOverlap(50)
                .metadata(meta)
                .dimensions(1536);

        assertEquals("openai", task.getLlmProvider());
        assertEquals("openai", task.getEmbeddingModelProvider());
        assertEquals("text-embedding-3", task.getEmbeddingModel());
        assertEquals("oai_int", task.getIntegrationName());
        assertEquals("pinecone", task.getVectorDb());
        assertEquals("ns1", task.getNamespace());
        assertEquals("idx1", task.getIndex());
        assertEquals("Hello world", task.getText());
        assertEquals("doc-123", task.getDocId());
        assertEquals("https://example.com/doc", task.getUrl());
        assertEquals("text/plain", task.getMediaType());
        assertEquals(500, task.getChunkSize());
        assertEquals(50, task.getChunkOverlap());
        assertSame(meta, task.getMetadata());
        assertEquals(1536, task.getDimensions());
    }

    @Test
    public void testLlmIndexTextWorkflowTask() {
        Map<String, Object> meta = Map.of("k", "v");

        LlmIndexText task = new LlmIndexText("def", "ref")
                .llmProvider("openai")
                .embeddingModelProvider("openai")
                .embeddingModel("ada-002")
                .integrationName("int1")
                .vectorDb("weaviate")
                .namespace("default")
                .index("my_index")
                .text("some text")
                .docId(42)
                .url("https://example.com")
                .mediaType("application/pdf")
                .chunkSize(1000)
                .chunkOverlap(100)
                .metadata(meta)
                .dimensions(768);

        WorkflowTask wt = singleTask(task);
        assertEquals("LLM_INDEX_TEXT", wt.getType());

        Map<String, Object> p = wt.getInputParameters();
        assertParam(p, "llmProvider", "openai");
        assertParam(p, "embeddingModelProvider", "openai");
        assertParam(p, "embeddingModel", "ada-002");
        assertParam(p, "integrationName", "int1");
        // vectorDb field maps to "vectorDB" key (capital DB)
        assertParam(p, "vectorDB", "weaviate");
        assertNoParam(p, "vectorDb");
        assertParam(p, "namespace", "default");
        assertParam(p, "index", "my_index");
        assertParam(p, "text", "some text");
        assertParam(p, "docId", 42);
        assertParam(p, "url", "https://example.com");
        assertParam(p, "mediaType", "application/pdf");
        assertParam(p, "chunkSize", 1000);
        assertParam(p, "chunkOverlap", 100);
        assertParam(p, "metadata", meta);
        assertParam(p, "dimensions", 768);
    }

    @Test
    public void testLlmIndexTextNullGuard() {
        WorkflowTask wt = singleTask(new LlmIndexText("def", "ref"));
        assertEquals("LLM_INDEX_TEXT", wt.getType());
        assertTrue(wt.getInputParameters().isEmpty());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  LlmSearchIndex
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    public void testLlmSearchIndexFluentApi() {
        List<Float> emb = List.of(0.1f, 0.2f, 0.3f);
        Map<String, Object> meta = Map.of("filter", "active");

        LlmSearchIndex task = new LlmSearchIndex("def", "ref")
                .llmProvider("openai")
                .embeddingModelProvider("openai")
                .embeddingModel("ada-002")
                .vectorDb("pinecone")
                .namespace("ns1")
                .index("idx1")
                .query("search term")
                .embeddings(emb)
                .maxResults(10)
                .metadata(meta)
                .dimensions(1536);

        assertEquals("openai", task.getLlmProvider());
        assertEquals("openai", task.getEmbeddingModelProvider());
        assertEquals("ada-002", task.getEmbeddingModel());
        assertEquals("pinecone", task.getVectorDb());
        assertEquals("ns1", task.getNamespace());
        assertEquals("idx1", task.getIndex());
        assertEquals("search term", task.getQuery());
        assertSame(emb, task.getEmbeddings());
        assertEquals(10, task.getMaxResults());
        assertSame(meta, task.getMetadata());
        assertEquals(1536, task.getDimensions());
    }

    @Test
    public void testLlmSearchIndexWorkflowTask() {
        List<Float> emb = List.of(1.0f, 2.0f);

        LlmSearchIndex task = new LlmSearchIndex("def", "ref")
                .llmProvider("cohere")
                .embeddingModelProvider("cohere")
                .embeddingModel("embed-v3")
                .vectorDb("qdrant")
                .namespace("prod")
                .index("search_idx")
                .query("what is AI?")
                .embeddings(emb)
                .maxResults(5)
                .metadata(Map.of("type", "faq"))
                .dimensions(384);

        WorkflowTask wt = singleTask(task);
        assertEquals("LLM_SEARCH_INDEX", wt.getType());

        Map<String, Object> p = wt.getInputParameters();
        assertParam(p, "llmProvider", "cohere");
        assertParam(p, "embeddingModelProvider", "cohere");
        assertParam(p, "embeddingModel", "embed-v3");
        // vectorDb -> "vectorDB"
        assertParam(p, "vectorDB", "qdrant");
        assertNoParam(p, "vectorDb");
        assertParam(p, "namespace", "prod");
        assertParam(p, "index", "search_idx");
        assertParam(p, "query", "what is AI?");
        assertParam(p, "embeddings", emb);
        assertParam(p, "maxResults", 5);
        assertParam(p, "metadata", Map.of("type", "faq"));
        assertParam(p, "dimensions", 384);
    }

    @Test
    public void testLlmSearchIndexNullGuard() {
        WorkflowTask wt = singleTask(new LlmSearchIndex("def", "ref"));
        assertEquals("LLM_SEARCH_INDEX", wt.getType());
        assertTrue(wt.getInputParameters().isEmpty());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  LlmGenerateEmbeddings
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    public void testLlmGenerateEmbeddingsFluentApi() {
        LlmGenerateEmbeddings task = new LlmGenerateEmbeddings("def", "ref")
                .llmProvider("openai")
                .model("text-embedding-3-small")
                .text("embed this")
                .dimensions(256);

        assertEquals("openai", task.getLlmProvider());
        assertEquals("text-embedding-3-small", task.getModel());
        assertEquals("embed this", task.getText());
        assertEquals(256, task.getDimensions());
    }

    @Test
    public void testLlmGenerateEmbeddingsWorkflowTask() {
        LlmGenerateEmbeddings task = new LlmGenerateEmbeddings("def", "ref")
                .llmProvider("openai")
                .model("ada-002")
                .text(List.of("text1", "text2"))
                .dimensions(1536);

        WorkflowTask wt = singleTask(task);
        assertEquals("LLM_GENERATE_EMBEDDINGS", wt.getType());

        Map<String, Object> p = wt.getInputParameters();
        assertParam(p, "llmProvider", "openai");
        assertParam(p, "model", "ada-002");
        assertParam(p, "text", List.of("text1", "text2"));
        assertParam(p, "dimensions", 1536);
    }

    @Test
    public void testLlmGenerateEmbeddingsNullGuard() {
        WorkflowTask wt = singleTask(new LlmGenerateEmbeddings("def", "ref"));
        assertEquals("LLM_GENERATE_EMBEDDINGS", wt.getType());
        assertTrue(wt.getInputParameters().isEmpty());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  LlmStoreEmbeddings
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    public void testLlmStoreEmbeddingsFluentApi() {
        List<Float> emb = List.of(0.5f, 0.6f);
        Map<String, Object> meta = Map.of("tag", "test");

        LlmStoreEmbeddings task = new LlmStoreEmbeddings("def", "ref")
                .embeddingModelProvider("openai")
                .embeddingModel("ada-002")
                .vectorDb("pinecone")
                .namespace("ns1")
                .index("store_idx")
                .embeddings(emb)
                .id("vec-001")
                .metadata(meta);

        assertEquals("openai", task.getEmbeddingModelProvider());
        assertEquals("ada-002", task.getEmbeddingModel());
        assertEquals("pinecone", task.getVectorDb());
        assertEquals("ns1", task.getNamespace());
        assertEquals("store_idx", task.getIndex());
        assertSame(emb, task.getEmbeddings());
        assertEquals("vec-001", task.getId());
        assertSame(meta, task.getMetadata());
    }

    @Test
    public void testLlmStoreEmbeddingsWorkflowTask() {
        List<Float> emb = List.of(1.1f, 2.2f, 3.3f);

        LlmStoreEmbeddings task = new LlmStoreEmbeddings("def", "ref")
                .embeddingModelProvider("cohere")
                .embeddingModel("embed-v3")
                .vectorDb("weaviate")
                .namespace("default")
                .index("emb_idx")
                .embeddings(emb)
                .id(99)
                .metadata(Map.of("source", "api"));

        WorkflowTask wt = singleTask(task);
        assertEquals("LLM_STORE_EMBEDDINGS", wt.getType());

        Map<String, Object> p = wt.getInputParameters();
        assertParam(p, "embeddingModelProvider", "cohere");
        assertParam(p, "embeddingModel", "embed-v3");
        // vectorDb -> "vectorDB"
        assertParam(p, "vectorDB", "weaviate");
        assertNoParam(p, "vectorDb");
        assertParam(p, "namespace", "default");
        assertParam(p, "index", "emb_idx");
        assertParam(p, "embeddings", emb);
        assertParam(p, "id", 99);
        assertParam(p, "metadata", Map.of("source", "api"));
    }

    @Test
    public void testLlmStoreEmbeddingsNullGuard() {
        WorkflowTask wt = singleTask(new LlmStoreEmbeddings("def", "ref"));
        assertEquals("LLM_STORE_EMBEDDINGS", wt.getType());
        assertTrue(wt.getInputParameters().isEmpty());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  LlmGetEmbeddings
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    public void testLlmGetEmbeddingsFluentApi() {
        List<Float> emb = List.of(0.1f, 0.2f);
        Map<String, Object> meta = Map.of("category", "test");

        LlmGetEmbeddings task = new LlmGetEmbeddings("def", "ref")
                .llmProvider("openai")
                .embeddingModelProvider("openai")
                .embeddingModel("ada-002")
                .vectorDb("pinecone")
                .namespace("ns1")
                .index("get_idx")
                .query("lookup query")
                .embeddings(emb)
                .maxResults(20)
                .metadata(meta)
                .dimensions(1536);

        assertEquals("openai", task.getLlmProvider());
        assertEquals("openai", task.getEmbeddingModelProvider());
        assertEquals("ada-002", task.getEmbeddingModel());
        assertEquals("pinecone", task.getVectorDb());
        assertEquals("ns1", task.getNamespace());
        assertEquals("get_idx", task.getIndex());
        assertEquals("lookup query", task.getQuery());
        assertSame(emb, task.getEmbeddings());
        assertEquals(20, task.getMaxResults());
        assertSame(meta, task.getMetadata());
        assertEquals(1536, task.getDimensions());
    }

    @Test
    public void testLlmGetEmbeddingsWorkflowTask() {
        List<Float> emb = List.of(9.9f);

        LlmGetEmbeddings task = new LlmGetEmbeddings("def", "ref")
                .llmProvider("anthropic")
                .embeddingModelProvider("anthropic")
                .embeddingModel("voyage-2")
                .vectorDb("milvus")
                .namespace("staging")
                .index("emb_get")
                .query(Map.of("text", "find similar"))
                .embeddings(emb)
                .maxResults(3)
                .metadata(Map.of("env", "staging"))
                .dimensions(512);

        WorkflowTask wt = singleTask(task);
        assertEquals("LLM_GET_EMBEDDINGS", wt.getType());

        Map<String, Object> p = wt.getInputParameters();
        assertParam(p, "llmProvider", "anthropic");
        assertParam(p, "embeddingModelProvider", "anthropic");
        assertParam(p, "embeddingModel", "voyage-2");
        // vectorDb -> "vectorDB"
        assertParam(p, "vectorDB", "milvus");
        assertNoParam(p, "vectorDb");
        assertParam(p, "namespace", "staging");
        assertParam(p, "index", "emb_get");
        assertParam(p, "query", Map.of("text", "find similar"));
        assertParam(p, "embeddings", emb);
        assertParam(p, "maxResults", 3);
        assertParam(p, "metadata", Map.of("env", "staging"));
        assertParam(p, "dimensions", 512);
    }

    @Test
    public void testLlmGetEmbeddingsNullGuard() {
        WorkflowTask wt = singleTask(new LlmGetEmbeddings("def", "ref"));
        assertEquals("LLM_GET_EMBEDDINGS", wt.getType());
        assertTrue(wt.getInputParameters().isEmpty());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ListMcpTools
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    public void testListMcpToolsFluentApi() {
        Map<String, String> headers = Map.of("Authorization", "Bearer token123");

        ListMcpTools task = new ListMcpTools("def", "ref")
                .mcpServer("https://mcp.example.com")
                .headers(headers);

        assertEquals("https://mcp.example.com", task.getMcpServer());
        assertSame(headers, task.getHeaders());
    }

    @Test
    public void testListMcpToolsWorkflowTask() {
        Map<String, String> headers = Map.of("X-API-Key", "key123");

        ListMcpTools task = new ListMcpTools("def", "ref")
                .mcpServer("https://mcp.server.io")
                .headers(headers);

        WorkflowTask wt = singleTask(task);
        assertEquals("LIST_MCP_TOOLS", wt.getType());

        Map<String, Object> p = wt.getInputParameters();
        assertParam(p, "mcpServer", "https://mcp.server.io");
        assertParam(p, "headers", headers);
    }

    @Test
    public void testListMcpToolsNullGuard() {
        WorkflowTask wt = singleTask(new ListMcpTools("def", "ref"));
        assertEquals("LIST_MCP_TOOLS", wt.getType());
        assertTrue(wt.getInputParameters().isEmpty());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CallMcpTool
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    public void testCallMcpToolFluentApi() {
        Map<String, Object> args = Map.of("city", "London", "units", "metric");
        Map<String, String> headers = Map.of("Authorization", "Bearer xyz");

        CallMcpTool task = new CallMcpTool("def", "ref")
                .mcpServer("https://mcp.example.com")
                .method("get_weather")
                .arguments(args)
                .headers(headers);

        assertEquals("https://mcp.example.com", task.getMcpServer());
        assertEquals("get_weather", task.getMethod());
        assertSame(args, task.getArguments());
        assertSame(headers, task.getHeaders());
    }

    @Test
    public void testCallMcpToolWorkflowTask() {
        Map<String, Object> args = Map.of("query", "test");
        Map<String, String> headers = Map.of("X-Key", "val");

        CallMcpTool task = new CallMcpTool("def", "ref")
                .mcpServer("https://mcp.io")
                .method("search")
                .arguments(args)
                .headers(headers);

        WorkflowTask wt = singleTask(task);
        assertEquals("CALL_MCP_TOOL", wt.getType());

        Map<String, Object> p = wt.getInputParameters();
        assertParam(p, "mcpServer", "https://mcp.io");
        assertParam(p, "method", "search");
        assertParam(p, "arguments", args);
        assertParam(p, "headers", headers);
    }

    @Test
    public void testCallMcpToolNullGuard() {
        WorkflowTask wt = singleTask(new CallMcpTool("def", "ref"));
        assertEquals("CALL_MCP_TOOL", wt.getType());
        assertTrue(wt.getInputParameters().isEmpty());
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  ToolSpec (not a Task — pure POJO)
    // ═════════════════════════════════════════════════════════════════════════

    @Test
    public void testToolSpecFluentApi() {
        Map<String, Object> configParams = Map.of("timeout", 30, "retries", 3);
        Map<String, String> intNames = Map.of("AI_MODEL", "openai");
        Map<String, Object> inSchema = Map.of(
                "type", "object",
                "properties", Map.of("city", Map.of("type", "string")),
                "required", List.of("city"));
        Map<String, Object> outSchema = Map.of(
                "type", "object",
                "properties", Map.of("temp", Map.of("type", "number")));

        ToolSpec spec = new ToolSpec()
                .name("get_weather")
                .type("SIMPLE")
                .description("Returns current weather for a city")
                .configParams(configParams)
                .integrationNames(intNames)
                .inputSchema(inSchema)
                .outputSchema(outSchema);

        assertEquals("get_weather", spec.getName());
        assertEquals("SIMPLE", spec.getType());
        assertEquals("Returns current weather for a city", spec.getDescription());
        assertSame(configParams, spec.getConfigParams());
        assertSame(intNames, spec.getIntegrationNames());
        assertSame(inSchema, spec.getInputSchema());
        assertSame(outSchema, spec.getOutputSchema());
    }

    @Test
    public void testToolSpecDefaultsAreNull() {
        ToolSpec spec = new ToolSpec();
        assertNull(spec.getName());
        assertNull(spec.getType());
        assertNull(spec.getDescription());
        assertNull(spec.getConfigParams());
        assertNull(spec.getIntegrationNames());
        assertNull(spec.getInputSchema());
        assertNull(spec.getOutputSchema());
    }
}
