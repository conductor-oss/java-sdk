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

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.conductoross.conductor.sdk.ai.LlmChatComplete;
import org.conductoross.conductor.sdk.ai.LlmGenerateEmbeddings;
import org.conductoross.conductor.sdk.ai.LlmIndexText;
import org.conductoross.conductor.sdk.ai.LlmSearchIndex;
import org.conductoross.conductor.sdk.ai.LlmTextComplete;

import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.MetadataClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.sdk.workflow.def.ConductorWorkflow;
import com.netflix.conductor.sdk.workflow.def.tasks.DoWhile;
import com.netflix.conductor.sdk.workflow.def.tasks.ForkJoin;
import com.netflix.conductor.sdk.workflow.def.tasks.Http;
import com.netflix.conductor.sdk.workflow.def.tasks.SetVariable;
import com.netflix.conductor.sdk.workflow.def.tasks.SimpleTask;
import com.netflix.conductor.sdk.workflow.def.tasks.Switch;
import com.netflix.conductor.sdk.workflow.def.tasks.Wait;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;

import io.orkes.conductor.sdk.examples.util.ClientUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Unified runner for all Agentic/AI examples in the Conductor Java SDK.
 * <p>
 * This class provides a single entry point to:
 * <ul>
 *   <li>Run all examples end-to-end</li>
 *   <li>Run individual examples interactively</li>
 *   <li>Validate SDK functionality with workers</li>
 *   <li>Execute LLM workflows using worker-based OpenAI/Claude calls</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>
 * # Run all examples
 * ./gradlew :examples:run -PmainClass=io.orkes.conductor.sdk.examples.agentic.AgenticExamplesRunner
 * 
 * # Run with specific mode
 * ./gradlew :examples:run --args="--all"
 * ./gradlew :examples:run --args="--menu"
 * ./gradlew :examples:run --args="--validate"
 * </pre>
 * 
 * <p>Environment variables:
 * <ul>
 *   <li>CONDUCTOR_SERVER_URL - Conductor server URL (default: http://localhost:8080/api)</li>
 *   <li>OPENAI_API_KEY - OpenAI API key for LLM workers</li>
 *   <li>ANTHROPIC_API_KEY - Anthropic API key for Claude (optional)</li>
 * </ul>
 */
public class AgenticExamplesRunner {

    private static final String BANNER = """
        ╔══════════════════════════════════════════════════════════════════╗
        ║         Conductor Java SDK - Agentic Examples Runner             ║
        ╚══════════════════════════════════════════════════════════════════╝
        """;

    // Configuration - prefer Anthropic if available, fall back to OpenAI
    private static final String OPENAI_API_KEY = System.getenv().getOrDefault("OPENAI_API_KEY", "");
    private static final String ANTHROPIC_API_KEY = System.getenv().getOrDefault("ANTHROPIC_API_KEY", "");
    private static final boolean USE_ANTHROPIC = !ANTHROPIC_API_KEY.isEmpty();
    private static final boolean HAS_LLM_KEY = USE_ANTHROPIC || !OPENAI_API_KEY.isEmpty();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    // Clients
    private ConductorClient client;
    private WorkflowExecutor executor;
    private WorkflowClient workflowClient;
    private TaskClient taskClient;
    private MetadataClient metadataClient;
    private TaskRunnerConfigurer taskRunner;

    // Test tracking
    private int passedTests = 0;
    private int failedTests = 0;
    private List<String> failedTestNames = new ArrayList<>();
    private List<WorkflowExecution> executedWorkflows = new ArrayList<>();

    // Record of an executed workflow
    private record WorkflowExecution(String workflowName, String workflowId, String status) {}

    public static void main(String[] args) {
        System.out.println(BANNER);
        
        AgenticExamplesRunner runner = new AgenticExamplesRunner();
        
        try {
            runner.initialize();
            
            if (args.length == 0 || "--all".equals(args[0])) {
                runner.runAllExamples();
            } else if ("--menu".equals(args[0])) {
                runner.runInteractiveMenu();
            } else if ("--validate".equals(args[0])) {
                runner.runValidation();
            } else if ("--help".equals(args[0])) {
                printHelp();
            } else {
                System.out.println("Unknown option: " + args[0]);
                printHelp();
            }
        } catch (Exception e) {
            System.err.println("\nFatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            runner.shutdown();
        }
    }

    private static void printHelp() {
        System.out.println("""
            Usage: AgenticExamplesRunner [option]
            
            Options:
              --all       Run all examples end-to-end (default)
              --menu      Interactive menu to select examples
              --validate  Run SDK validation tests only
              --help      Show this help message
            
            Environment variables:
              CONDUCTOR_SERVER_URL  - Conductor server URL
              OPENAI_API_KEY        - OpenAI API key for LLM examples
              ANTHROPIC_API_KEY     - Anthropic API key (optional)
            """);
    }

    /**
     * Initialize all clients and workers.
     */
    public void initialize() {
        String serverUrl = System.getenv().getOrDefault("CONDUCTOR_SERVER_URL", "http://localhost:8080/api");
        System.out.println("Connecting to: " + serverUrl);
        
        client = ClientUtil.getClient();
        executor = new WorkflowExecutor(client, 100);
        workflowClient = new WorkflowClient(client);
        taskClient = new TaskClient(client);
        metadataClient = new MetadataClient(client);
        
        // Verify connectivity
        try {
            metadataClient.getAllWorkflowsWithLatestVersions();
            System.out.println("Connected to Conductor server successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to Conductor server: " + e.getMessage(), e);
        }
        
        // Check for API keys
        if (USE_ANTHROPIC) {
            System.out.println("LLM Provider: Anthropic Claude (API key configured)");
        } else if (!OPENAI_API_KEY.isEmpty()) {
            System.out.println("LLM Provider: OpenAI (API key configured)");
        } else {
            System.out.println("LLM Provider: not configured (set OPENAI_API_KEY or ANTHROPIC_API_KEY)");
        }
        System.out.println();
        
        // Start workers for validation
        startWorkers();
    }

    /**
     * Start workers needed for examples.
     */
    private void startWorkers() {
        List<Worker> workers = new ArrayList<>();
        
        // Basic validation workers
        workers.add(createWorker("get_weather", this::executeWeatherTask));
        workers.add(createWorker("search_web", this::executeSearchTask));
        workers.add(createWorker("calculate", this::executeCalculateTask));
        workers.add(createWorker("direct_response", this::executeDirectResponseTask));
        workers.add(createWorker("validation_task_1", this::executeValidationTask));
        workers.add(createWorker("validation_task_2", this::executeValidationTask));
        workers.add(createWorker("validation_task_3", this::executeValidationTask));
        
        // Dispatch function worker (for function calling workflow - parses LLM JSON and calls function)
        workers.add(createWorker("dispatch_function", this::executeDispatchFunctionTask));
        
        // Workers for multi-agent and multi-turn chat workflows
        workers.add(createWorker("chat_collect_history", this::executeChatCollectHistoryTask));
        workers.add(createWorker("build_moderator_messages", this::executeBuildModeratorMessagesTask));
        workers.add(createWorker("update_multiagent_history", this::executeUpdateMultiagentHistoryTask));
        
        // MCP workers (bridge to MCP server via HTTP when Conductor doesn't have native MCP support)
        workers.add(createWorker("mcp_list_tools_worker", this::executeMcpListToolsTask));
        workers.add(createWorker("mcp_call_tool_worker", this::executeMcpCallToolTask));
        
        // LLM workers (only if API key is configured)
        if (HAS_LLM_KEY) {
            workers.add(createWorker("llm_chat", this::executeLlmChatTask));
            workers.add(createWorker("llm_text_complete", this::executeLlmTextCompleteTask));
            workers.add(createWorker("llm_function_selector", this::executeLlmFunctionSelectorTask));
            workers.add(createWorker("llm_response_formatter", this::executeLlmResponseFormatterTask));
        }
        
        // Register task definitions
        for (Worker worker : workers) {
            try {
                TaskDef taskDef = new TaskDef(worker.getTaskDefName());
                taskDef.setOwnerEmail("examples@conductor-oss.org");
                taskDef.setRetryCount(0);
                taskDef.setTimeoutSeconds(120);
                metadataClient.registerTaskDefs(List.of(taskDef));
            } catch (Exception e) {
                // Task may already exist
            }
        }
        
        taskRunner = new TaskRunnerConfigurer.Builder(taskClient, workers)
            .withThreadCount(10)
            .build();
        taskRunner.init();
        
        System.out.println("Workers started: " + workers.size() + " task types\n");
    }

    /**
     * Shutdown all resources.
     */
    public void shutdown() {
        if (taskRunner != null) {
            taskRunner.shutdown();
        }
        if (executor != null) {
            executor.shutdown();
        }
    }

    /**
     * Run all examples end-to-end.
     */
    public void runAllExamples() {
        System.out.println("═══════════════════════════════════════════════════════════════════");
        System.out.println("                    Running All Examples                            ");
        System.out.println("═══════════════════════════════════════════════════════════════════\n");

        // 1. SDK Validation
        runTest("SDK Validation", this::runValidation);

        // 2. LLM Task Classes
        runTest("LLM Task Classes Instantiation", this::testLlmTaskClasses);

        // 3. Workflow Task Types
        runTest("Complex Task Types", this::testComplexTaskTypes);

        // 4. Simple Workflow Execution
        runTest("Simple Workflow Execution", this::runSimpleWorkflow);

        // 5. LLM Examples (if API key is configured)
        if (HAS_LLM_KEY) {
            runTest("LLM Chat Workflow", this::runLlmChatWorkflow);
            runTest("Function Calling Workflow", this::runFunctionCallingWorkflow);
            runTest("Multi-turn Conversation Workflow", this::runMultiTurnConversationWorkflow);
            runTest("Multi-Agent Chat Workflow", this::runMultiAgentChatWorkflow);
            runTest("Human-in-the-Loop Chat", this::runHumanInLoopChatWorkflow);
            runTest("MCP Weather Agent", this::runMcpWeatherAgentWorkflow);
        } else {
            System.out.println("\n─────────────────────────────────────────────────────────────────");
            System.out.println("  LLM Examples (SKIPPED - Set OPENAI_API_KEY or ANTHROPIC_API_KEY)");
            System.out.println("─────────────────────────────────────────────────────────────────");
        }

        // Print summary
        printSummary();
    }

    /**
     * Run a test with error handling.
     */
    private void runTest(String testName, Runnable test) {
        System.out.println("\n─────────────────────────────────────────────────────────────────");
        System.out.println("  " + testName);
        System.out.println("─────────────────────────────────────────────────────────────────");
        
        try {
            test.run();
            pass(testName + " completed");
        } catch (Exception e) {
            fail(testName + " failed: " + e.getMessage());
            failedTestNames.add(testName);
            e.printStackTrace();
        }
    }

    /**
     * SDK validation tests.
     */
    public void runValidation() {
        // Test workflow definition
        ConductorWorkflow<Map<String, Object>> workflow = new ConductorWorkflow<>(executor);
        workflow.setName("sdk_validation_workflow");
        workflow.setVersion(1);
        workflow.setOwnerEmail("examples@conductor-oss.org");

        SimpleTask task1 = new SimpleTask("validation_task_1", "task1_ref");
        task1.input("input_name", "${workflow.input.name}");
        workflow.add(task1);

        SimpleTask task2 = new SimpleTask("validation_task_2", "task2_ref");
        task2.input("previous_output", "${task1_ref.output.result}");
        workflow.add(task2);

        SimpleTask task3 = new SimpleTask("validation_task_3", "task3_ref");
        task3.input("all_results", Map.of("task1", "${task1_ref.output.result}", "task2", "${task2_ref.output.result}"));
        workflow.add(task3);

        Map<String, Object> output = new HashMap<>();
        output.put("finalResult", "${task3_ref.output.result}");
        workflow.setWorkflowOutput(output);

        // Register
        workflow.registerWorkflow(true, true);
        pass("Workflow registered: sdk_validation_workflow");

        // Execute
        Map<String, Object> input = new HashMap<>();
        input.put("name", "Validation Test");

        try {
            var workflowRun = workflow.execute(input);
            Workflow result = workflowRun.get(30, TimeUnit.SECONDS);
            
            String workflowId = result.getWorkflowId();
            String status = result.getStatus().toString();
            executedWorkflows.add(new WorkflowExecution("sdk_validation_workflow", workflowId, status));
            
            if (result.getStatus() == Workflow.WorkflowStatus.COMPLETED) {
                pass("Workflow executed: " + workflowId + " [" + status + "]");
            } else {
                fail("Workflow failed: " + workflowId + " [" + status + "]");
            }
        } catch (Exception e) {
            fail("Workflow execution error: " + e.getMessage());
        }
    }

    /**
     * Test all LLM task classes can be instantiated.
     */
    private void testLlmTaskClasses() {
        // LlmTextComplete
        LlmTextComplete textTask = new LlmTextComplete("test_text", "text_ref")
            .llmProvider("openai")
            .model("gpt-4o-mini")
            .promptName("test-prompt")
            .temperature(0.7);
        pass("LlmTextComplete instantiated");

        // LlmChatComplete
        LlmChatComplete chatTask = new LlmChatComplete("test_chat", "chat_ref")
            .llmProvider("openai")
            .model("gpt-4o-mini")
            .messages(List.of(Map.of("role", "user", "content", "test")))
            .temperature(0.7);
        pass("LlmChatComplete instantiated");

        // LlmIndexText
        LlmIndexText indexTask = new LlmIndexText("test_index", "index_ref")
            .vectorDb("pinecone")
            .namespace("test")
            .index("test-index")
            .embeddingModel("text-embedding-ada-002")
            .text("test document");
        pass("LlmIndexText instantiated");

        // LlmSearchIndex
        LlmSearchIndex searchTask = new LlmSearchIndex("test_search", "search_ref")
            .vectorDb("pinecone")
            .namespace("test")
            .index("test-index")
            .query("test query")
            .maxResults(5);
        pass("LlmSearchIndex instantiated");

        // LlmGenerateEmbeddings
        LlmGenerateEmbeddings embedTask = new LlmGenerateEmbeddings("test_embed", "embed_ref")
            .llmProvider("openai")
            .model("text-embedding-ada-002")
            .text("test text");
        pass("LlmGenerateEmbeddings instantiated");
    }

    /**
     * Test complex task types.
     */
    private void testComplexTaskTypes() {
        // Switch task
        Switch switchTask = new Switch("switch_ref", "${workflow.input.condition}");
        switchTask.switchCase("case1", new SimpleTask("task1", "task1_ref"));
        switchTask.switchCase("case2", new SimpleTask("task2", "task2_ref"));
        switchTask.defaultCase(new SimpleTask("default", "default_ref"));
        pass("Switch task created with 2 cases + default");

        // DoWhile task
        DoWhile doWhile = new DoWhile("loop_ref", 3, 
            new SimpleTask("loop_task", "loop_task_ref"));
        pass("DoWhile task created with 3 iterations");

        // ForkJoin task
        SimpleTask fork1 = new SimpleTask("fork1", "fork1_ref");
        SimpleTask fork2 = new SimpleTask("fork2", "fork2_ref");
        @SuppressWarnings("unchecked")
        ForkJoin forkJoin = new ForkJoin("fork_ref", 
            new com.netflix.conductor.sdk.workflow.def.tasks.Task<?>[]{fork1}, 
            new com.netflix.conductor.sdk.workflow.def.tasks.Task<?>[]{fork2});
        pass("ForkJoin task created with 2 parallel branches");

        // Http task
        Http httpTask = new Http("http_ref");
        httpTask.url("https://httpbin.org/get")
            .method(Http.Input.HttpMethod.GET);
        pass("Http task created");

        // SetVariable task
        SetVariable setVar = new SetVariable("setvar_ref");
        setVar.input("myVar", "myValue");
        pass("SetVariable task created");
    }

    /**
     * Run a simple workflow end-to-end.
     */
    private void runSimpleWorkflow() {
        ConductorWorkflow<Map<String, Object>> workflow = new ConductorWorkflow<>(executor);
        workflow.setName("simple_agentic_test");
        workflow.setVersion(1);
        workflow.setOwnerEmail("examples@conductor-oss.org");

        SimpleTask task = new SimpleTask("validation_task_1", "test_ref");
        task.input("message", "${workflow.input.message}");
        workflow.add(task);

        workflow.setWorkflowOutput(Map.of("result", "${test_ref.output.result}"));
        workflow.registerWorkflow(true, true);

        try {
            var run = workflow.execute(Map.of("message", "Hello from Agentic Runner!"));
            Workflow result = run.get(30, TimeUnit.SECONDS);
            
            String workflowId = result.getWorkflowId();
            String status = result.getStatus().toString();
            executedWorkflows.add(new WorkflowExecution("simple_agentic_test", workflowId, status));
            
            if (result.getStatus() == Workflow.WorkflowStatus.COMPLETED) {
                pass("Workflow executed: " + workflowId + " [" + status + "]");
            } else {
                fail("Workflow failed: " + workflowId + " [" + status + "]");
            }
        } catch (Exception e) {
            fail("Simple workflow error: " + e.getMessage());
        }
    }

    /**
     * Run LLM Chat workflow using LLM_CHAT_COMPLETE system task.
     */
    private void runLlmChatWorkflow() {
        ConductorWorkflow<Map<String, Object>> workflow = new ConductorWorkflow<>(executor);
        workflow.setName("llm_chat_workflow");
        workflow.setVersion(1);
        workflow.setOwnerEmail("examples@conductor-oss.org");
        workflow.setDescription("LLM chat using LLM_CHAT_COMPLETE system task");

        // LLM Chat Complete system task
        LlmChatComplete chatTask = new LlmChatComplete("llm_chat_complete", "chat_ref")
            .llmProvider("${workflow.input.llmProvider}")
            .model("${workflow.input.model}")
            .messages("${workflow.input.messages}")
            .temperature(0.7)
            .maxTokens(500);
        workflow.add(chatTask);

        workflow.setWorkflowOutput(Map.of(
            "response", "${chat_ref.output.result}",
            "model", "${workflow.input.model}"
        ));
        workflow.registerWorkflow(true, true);
        pass("LLM Chat workflow registered (using LLM_CHAT_COMPLETE)");

        // Execute the workflow
        Map<String, Object> input = new HashMap<>();
        input.put("llmProvider", "openai");
        input.put("model", "gpt-4o-mini");
        input.put("messages", List.of(
            Map.of("role", "system", "message", "You are a helpful assistant that gives concise answers."),
            Map.of("role", "user", "message", "What is Conductor workflow orchestration? Answer in 2-3 sentences.")
        ));

        try {
            var run = workflow.execute(input);
            Workflow result = run.get(90, TimeUnit.SECONDS);
            
            String workflowId = result.getWorkflowId();
            String status = result.getStatus().toString();
            executedWorkflows.add(new WorkflowExecution("llm_chat_workflow", workflowId, status));
            
            if (result.getStatus() == Workflow.WorkflowStatus.COMPLETED) {
                pass("Workflow executed: " + workflowId + " [" + status + "]");
                String response = (String) result.getOutput().get("response");
                if (response != null && response.length() > 100) {
                    response = response.substring(0, 100) + "...";
                }
                pass("LLM Response: " + response);
            } else {
                fail("Workflow failed: " + workflowId + " [" + status + "] - " + result.getReasonForIncompletion());
            }
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            fail("LLM Chat workflow error: " + errorMsg);
            e.printStackTrace();
        }
    }

    /**
     * Run Function Calling workflow using LLM_CHAT_COMPLETE with JSON output.
     * Follows Python SDK pattern: LLM returns JSON with function name and parameters,
     * then a dispatch worker calls the appropriate function.
     */
    private void runFunctionCallingWorkflow() {
        ConductorWorkflow<Map<String, Object>> workflow = new ConductorWorkflow<>(executor);
        workflow.setName("function_calling_workflow");
        workflow.setVersion(1);
        workflow.setOwnerEmail("examples@conductor-oss.org");
        workflow.setDescription("LLM function calling with JSON output and dispatch worker");

        // System prompt describing available tools (same pattern as Python SDK)
        String systemPrompt = """
            You are a helpful assistant with access to the following tools (functions):
            
            1. get_weather(city: string) - Get current weather for a city
            2. get_price(product: string) - Look up the price of a product  
            3. calculate(expression: string) - Evaluate a math expression
            
            When you need to use a tool, respond with ONLY this JSON (no other text):
            {"function": "FUNCTION_NAME", "function_parameters": {"param1": "value1"}}
            
            If you don't need a tool, respond with:
            {"function": "direct_response", "function_parameters": {"answer": "your answer here"}}
            """;

        // Step 1: LLM decides which function to call (returns JSON)
        LlmChatComplete selectorTask = new LlmChatComplete("function_selector", "selector_ref")
            .llmProvider("${workflow.input.llmProvider}")
            .model("${workflow.input.model}")
            .messages("${workflow.input.messages}")
            .temperature(0.0)
            .maxTokens(200);
        workflow.add(selectorTask);

        // Step 2: Dispatch worker parses JSON and calls the function
        SimpleTask dispatchTask = new SimpleTask("dispatch_function", "dispatch_ref");
        dispatchTask.input("llm_response", "${selector_ref.output.result}");
        workflow.add(dispatchTask);

        workflow.setWorkflowOutput(Map.of(
            "llm_decision", "${selector_ref.output.result}",
            "function_result", "${dispatch_ref.output.result}",
            "function_called", "${dispatch_ref.output.function}"
        ));
        workflow.registerWorkflow(true, true);
        pass("Function Calling workflow registered (using LLM_CHAT_COMPLETE with JSON)");

        // Execute the workflow
        Map<String, Object> input = new HashMap<>();
        input.put("llmProvider", "openai");
        input.put("model", "gpt-4o-mini");
        input.put("messages", List.of(
            Map.of("role", "system", "message", systemPrompt),
            Map.of("role", "user", "message", "What's the weather like in San Francisco?")
        ));

        try {
            var run = workflow.execute(input);
            Workflow result = run.get(90, TimeUnit.SECONDS);
            
            String workflowId = result.getWorkflowId();
            String status = result.getStatus().toString();
            executedWorkflows.add(new WorkflowExecution("function_calling_workflow", workflowId, status));
            
            if (result.getStatus() == Workflow.WorkflowStatus.COMPLETED) {
                pass("Workflow executed: " + workflowId + " [" + status + "]");
                pass("Function called: " + result.getOutput().get("function_called"));
                Object functionResult = result.getOutput().get("function_result");
                String resultStr = functionResult != null ? functionResult.toString() : "null";
                if (resultStr.length() > 100) {
                    resultStr = resultStr.substring(0, 100) + "...";
                }
                pass("Result: " + resultStr);
            } else {
                fail("Workflow failed: " + workflowId + " [" + status + "] - " + result.getReasonForIncompletion());
            }
        } catch (Exception e) {
            fail("Function Calling workflow error: " + e.getMessage());
        }
    }

    /**
     * Run Multi-turn Conversation workflow using LLM_CHAT_COMPLETE system tasks.
     */
    private void runMultiTurnConversationWorkflow() {
        ConductorWorkflow<Map<String, Object>> workflow = new ConductorWorkflow<>(executor);
        workflow.setName("multi_turn_conversation");
        workflow.setVersion(1);
        workflow.setOwnerEmail("examples@conductor-oss.org");
        workflow.setDescription("Multi-turn conversation using LLM_CHAT_COMPLETE");

        // Turn 1: Initial question
        LlmChatComplete turn1 = new LlmChatComplete("turn1_chat", "turn1_ref")
            .llmProvider("${workflow.input.llmProvider}")
            .model("${workflow.input.model}")
            .messages(List.of(
                Map.of("role", "system", "message", "You are a helpful assistant. Be concise."),
                Map.of("role", "user", "message", "${workflow.input.question1}")
            ))
            .temperature(0.7)
            .maxTokens(200);
        workflow.add(turn1);

        // Turn 2: Follow-up question with context from turn 1
        LlmChatComplete turn2 = new LlmChatComplete("turn2_chat", "turn2_ref")
            .llmProvider("${workflow.input.llmProvider}")
            .model("${workflow.input.model}")
            .messages(List.of(
                Map.of("role", "system", "message", "You are a helpful assistant. Consider the previous context."),
                Map.of("role", "user", "message", "${workflow.input.question1}"),
                Map.of("role", "assistant", "message", "${turn1_ref.output.result}"),
                Map.of("role", "user", "message", "${workflow.input.question2}")
            ))
            .temperature(0.7)
            .maxTokens(200);
        workflow.add(turn2);

        workflow.setWorkflowOutput(Map.of(
            "turn1_response", "${turn1_ref.output.result}",
            "turn2_response", "${turn2_ref.output.result}"
        ));
        workflow.registerWorkflow(true, true);
        pass("Multi-turn Conversation workflow registered (using LLM_CHAT_COMPLETE)");

        // Execute the workflow
        Map<String, Object> input = new HashMap<>();
        input.put("question1", "What is the capital of France?");
        input.put("question2", "What is its population?");
        input.put("llmProvider", "openai");
        input.put("model", "gpt-4o-mini");

        try {
            var run = workflow.execute(input);
            Workflow result = run.get(90, TimeUnit.SECONDS);
            
            String workflowId = result.getWorkflowId();
            String status = result.getStatus().toString();
            executedWorkflows.add(new WorkflowExecution("multi_turn_conversation", workflowId, status));
            
            if (result.getStatus() == Workflow.WorkflowStatus.COMPLETED) {
                pass("Workflow executed: " + workflowId + " [" + status + "]");
                String turn1Response = (String) result.getOutput().get("turn1_response");
                String turn2Response = (String) result.getOutput().get("turn2_response");
                if (turn1Response != null && turn1Response.length() > 80) {
                    turn1Response = turn1Response.substring(0, 80) + "...";
                }
                if (turn2Response != null && turn2Response.length() > 80) {
                    turn2Response = turn2Response.substring(0, 80) + "...";
                }
                pass("Turn 1: " + turn1Response);
                pass("Turn 2: " + turn2Response);
            } else {
                fail("Workflow failed: " + workflowId + " [" + status + "] - " + result.getReasonForIncompletion());
            }
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            fail("Multi-turn workflow error: " + errorMsg);
            e.printStackTrace();
        }
    }

    /**
     * Interactive menu for selecting examples.
     */
    public void runInteractiveMenu() {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            System.out.println("\n═══════════════════════════════════════════════════════════════════");
            System.out.println("                    Agentic Examples Menu                           ");
            System.out.println("═══════════════════════════════════════════════════════════════════");
            System.out.println("  1. Run SDK Validation");
            System.out.println("  2. Test LLM Task Classes");
            System.out.println("  3. Test Complex Task Types");
            System.out.println("  4. Run Simple Workflow");
            if (HAS_LLM_KEY) {
                System.out.println("  5. Run LLM Chat Workflow");
                System.out.println("  6. Run Function Calling Workflow");
                System.out.println("  7. Run Multi-turn Conversation");
            }
            System.out.println("  8. Run All Examples");
            System.out.println("  0. Exit");
            System.out.println("═══════════════════════════════════════════════════════════════════");
            System.out.print("\nSelect option: ");
            
            String input = scanner.nextLine().trim();
            
            switch (input) {
                case "1" -> runTest("SDK Validation", this::runValidation);
                case "2" -> runTest("LLM Task Classes", this::testLlmTaskClasses);
                case "3" -> runTest("Complex Task Types", this::testComplexTaskTypes);
                case "4" -> runTest("Simple Workflow", this::runSimpleWorkflow);
                case "5" -> {
                    if (HAS_LLM_KEY) {
                        runTest("LLM Chat Workflow", this::runLlmChatWorkflow);
                    } else {
                        System.out.println("No LLM API key configured");
                    }
                }
                case "6" -> {
                    if (HAS_LLM_KEY) {
                        runTest("Function Calling Workflow", this::runFunctionCallingWorkflow);
                    } else {
                        System.out.println("No LLM API key configured");
                    }
                }
                case "7" -> {
                    if (HAS_LLM_KEY) {
                        runTest("Multi-turn Conversation", this::runMultiTurnConversationWorkflow);
                    } else {
                        System.out.println("No LLM API key configured");
                    }
                }
                case "8" -> runAllExamples();
                case "0" -> {
                    System.out.println("\nGoodbye!");
                    return;
                }
                default -> System.out.println("Invalid option. Please try again.");
            }
        }
    }

    /**
     * Print test summary.
     */
    private void printSummary() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║                        TEST SUMMARY                               ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Passed: %-5d                                                    ║%n", passedTests);
        System.out.printf("║  Failed: %-5d                                                    ║%n", failedTests);
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        if (failedTests == 0) {
            System.out.println("║  Result: ALL TESTS PASSED                                        ║");
        } else {
            System.out.println("║  Result: SOME TESTS FAILED                                       ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            System.out.println("║  Failed tests:                                                   ║");
            for (String name : failedTestNames) {
                System.out.printf("║    - %-58s ║%n", name);
            }
        }
        System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        
        // Print executed workflows
        if (!executedWorkflows.isEmpty()) {
            System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
            System.out.println("║                    EXECUTED WORKFLOWS                             ║");
            System.out.println("╠══════════════════════════════════════════════════════════════════╣");
            for (WorkflowExecution wf : executedWorkflows) {
                System.out.printf("║  %-30s                                  ║%n", wf.workflowName());
                System.out.printf("║    ID: %-56s ║%n", wf.workflowId());
                System.out.printf("║    Status: %-52s ║%n", wf.status());
            }
            System.out.println("╚══════════════════════════════════════════════════════════════════╝");
        }
        
        System.exit(failedTests > 0 ? 1 : 0);
    }

    private void pass(String message) {
        System.out.println("  ✓ " + message);
        passedTests++;
    }

    private void fail(String message) {
        System.out.println("  ✗ " + message);
        failedTests++;
    }

    // ═══════════════════════════════════════════════════════════════════
    //                        WORKER IMPLEMENTATIONS
    // ═══════════════════════════════════════════════════════════════════

    private Worker createWorker(String taskDefName, java.util.function.Function<Task, TaskResult> executor) {
        return new Worker() {
            @Override
            public String getTaskDefName() {
                return taskDefName;
            }

            @Override
            public TaskResult execute(Task task) {
                return executor.apply(task);
            }
        };
    }

    private TaskResult executeWeatherTask(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        String location = (String) task.getInputData().get("location");
        if (location == null) location = "Unknown";
        result.addOutputData("result", "Weather in " + location + ": Sunny, 72°F (22°C), clear skies");
        result.addOutputData("temperature", 72);
        result.addOutputData("condition", "sunny");
        result.addOutputData("location", location);
        return result;
    }

    private TaskResult executeSearchTask(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        String query = (String) task.getInputData().get("query");
        if (query == null) query = "Unknown";
        result.addOutputData("result", "Search results for: " + query);
        result.addOutputData("results", List.of(
            Map.of("title", "Result 1 for " + query, "url", "https://example.com/1"),
            Map.of("title", "Result 2 for " + query, "url", "https://example.com/2")
        ));
        return result;
    }

    private TaskResult executeCalculateTask(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        String expression = (String) task.getInputData().get("expression");
        if (expression == null) expression = "0";
        result.addOutputData("result", "Calculated: " + expression + " = 42");
        result.addOutputData("value", 42);
        return result;
    }

    private TaskResult executeDirectResponseTask(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        String query = (String) task.getInputData().get("query");
        result.addOutputData("result", "I can help answer: " + query);
        return result;
    }

    private TaskResult executeValidationTask(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("result", "Processed by " + task.getTaskDefName());
        result.addOutputData("inputReceived", task.getInputData());
        result.addOutputData("timestamp", System.currentTimeMillis());
        return result;
    }

    /**
     * Dispatch function worker - parses LLM JSON response and calls the appropriate function.
     * This follows the Python SDK pattern where the LLM returns JSON like:
     * {"function": "function_name", "function_parameters": {"param1": "value1"}}
     */
    @SuppressWarnings("unchecked")
    private TaskResult executeDispatchFunctionTask(Task task) {
        TaskResult result = new TaskResult(task);
        
        Object llmResponseObj = task.getInputData().get("llm_response");
        if (llmResponseObj == null) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("No LLM response provided");
            return result;
        }
        
        try {
            String functionName;
            Map<String, Object> functionParams;
            String rawResponse;
            
            // Handle both String and Map (already parsed JSON) responses
            if (llmResponseObj instanceof Map) {
                // Already parsed by Conductor - the LLM_CHAT_COMPLETE task may return parsed JSON
                Map<String, Object> responseMap = (Map<String, Object>) llmResponseObj;
                functionName = (String) responseMap.get("function");
                Object paramsObj = responseMap.get("function_parameters");
                functionParams = paramsObj instanceof Map ? (Map<String, Object>) paramsObj : Map.of();
                rawResponse = objectMapper.writeValueAsString(responseMap);
            } else {
                // String response - parse it
                String llmResponse = llmResponseObj.toString();
                rawResponse = llmResponse;
                
                // Clean up the response - sometimes LLM wraps JSON in markdown code blocks
                String cleanedResponse = llmResponse.trim();
                if (cleanedResponse.startsWith("```json")) {
                    cleanedResponse = cleanedResponse.substring(7);
                } else if (cleanedResponse.startsWith("```")) {
                    cleanedResponse = cleanedResponse.substring(3);
                }
                if (cleanedResponse.endsWith("```")) {
                    cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
                }
                cleanedResponse = cleanedResponse.trim();
                
                // Parse the JSON
                JsonNode json = objectMapper.readTree(cleanedResponse);
                functionName = json.path("function").asText();
                JsonNode paramsNode = json.path("function_parameters");
                functionParams = paramsNode.isMissingNode() ? Map.of() : objectMapper.convertValue(paramsNode, Map.class);
            }
            
            if (functionName == null || functionName.isEmpty()) {
                result.setStatus(TaskResult.Status.FAILED);
                result.setReasonForIncompletion("No function name in LLM response: " + rawResponse);
                return result;
            }
            
            // Dispatch to the appropriate function
            String functionResult;
            switch (functionName) {
                case "get_weather" -> {
                    String city = (String) functionParams.getOrDefault("city", 
                                  functionParams.getOrDefault("location", "")); // Alternative param name
                    functionResult = getWeather(city);
                }
                case "get_price" -> {
                    String product = (String) functionParams.getOrDefault("product", "");
                    functionResult = getPrice(product);
                }
                case "calculate" -> {
                    String expression = (String) functionParams.getOrDefault("expression", "");
                    functionResult = calculate(expression);
                }
                case "direct_response" -> {
                    String answer = (String) functionParams.getOrDefault("answer", "");
                    functionResult = answer.isEmpty() ? "No direct answer provided" : answer;
                }
                default -> {
                    functionResult = "Unknown function: " + functionName;
                }
            }
            
            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("function", functionName);
            result.addOutputData("parameters", functionParams);
            result.addOutputData("result", functionResult);
            result.addOutputData("llm_raw_response", rawResponse);
            
        } catch (Exception e) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("Failed to parse LLM response: " + e.getMessage() + " - Response was: " + llmResponseObj);
        }
        
        return result;
    }
    
    // Function implementations for dispatch_function worker
    private String getWeather(String city) {
        if (city == null || city.isEmpty()) {
            city = "Unknown";
        }
        // Simulated weather data
        return String.format("Weather in %s: Sunny, 72°F (22°C), humidity 45%%, winds 10 mph from the west.", city);
    }
    
    private String getPrice(String product) {
        if (product == null || product.isEmpty()) {
            return "No product specified";
        }
        // Simulated price data
        Map<String, String> prices = Map.of(
            "laptop", "$999.99",
            "phone", "$699.99",
            "tablet", "$449.99",
            "headphones", "$199.99"
        );
        String price = prices.get(product.toLowerCase());
        return price != null ? String.format("The price of %s is %s", product, price) 
                            : String.format("Price for '%s' not found in catalog", product);
    }
    
    private String calculate(String expression) {
        if (expression == null || expression.isEmpty()) {
            return "No expression provided";
        }
        // Simple evaluation for basic expressions (for demo purposes)
        try {
            // Handle simple arithmetic for demo
            expression = expression.replaceAll("\\s+", "");
            if (expression.matches("\\d+\\+\\d+")) {
                String[] parts = expression.split("\\+");
                int result = Integer.parseInt(parts[0]) + Integer.parseInt(parts[1]);
                return expression + " = " + result;
            } else if (expression.matches("\\d+\\-\\d+")) {
                String[] parts = expression.split("\\-");
                int result = Integer.parseInt(parts[0]) - Integer.parseInt(parts[1]);
                return expression + " = " + result;
            } else if (expression.matches("\\d+\\*\\d+")) {
                String[] parts = expression.split("\\*");
                int result = Integer.parseInt(parts[0]) * Integer.parseInt(parts[1]);
                return expression + " = " + result;
            } else if (expression.matches("\\d+/\\d+")) {
                String[] parts = expression.split("/");
                double result = Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
                return expression + " = " + result;
            }
            return "Calculated: " + expression + " (complex expression - result: 42)";
        } catch (Exception e) {
            return "Error calculating: " + expression;
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //                        LLM WORKER IMPLEMENTATIONS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * LLM Chat worker - calls OpenAI or Anthropic API based on configuration.
     */
    private TaskResult executeLlmChatTask(Task task) {
        TaskResult result = new TaskResult(task);
        
        String prompt = (String) task.getInputData().get("prompt");
        // Always use the appropriate model for the configured provider
        String model = USE_ANTHROPIC ? "claude-haiku-4-5" : "gpt-4o-mini";
        String systemPrompt = (String) task.getInputData().getOrDefault("systemPrompt", "You are a helpful assistant.");
        
        try {
            String response = USE_ANTHROPIC ? callClaudeChat(prompt, model, systemPrompt) : callOpenAiChat(prompt, model, systemPrompt);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("response", response);
            result.addOutputData("model", model);
            result.addOutputData("provider", USE_ANTHROPIC ? "anthropic" : "openai");
            result.addOutputData("usage", Map.of("model", model, "prompt_length", prompt.length()));
        } catch (Exception e) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("LLM API error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }

    /**
     * LLM Text Complete worker.
     */
    private TaskResult executeLlmTextCompleteTask(Task task) {
        return executeLlmChatTask(task); // Same implementation for simplicity
    }

    /**
     * LLM Function Selector worker - uses tool/function calling.
     */
    private TaskResult executeLlmFunctionSelectorTask(Task task) {
        TaskResult result = new TaskResult(task);
        
        String query = (String) task.getInputData().get("query");
        
        try {
            Map<String, Object> functionCall = USE_ANTHROPIC ? callClaudeFunctionSelection(query) : callOpenAiFunctionSelection(query);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("selectedFunction", functionCall.get("name"));
            result.addOutputData("functionArgs", functionCall.get("arguments"));
            result.addOutputData("reasoning", functionCall.get("reasoning"));
        } catch (Exception e) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("LLM API error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * LLM Response Formatter worker.
     */
    private TaskResult executeLlmResponseFormatterTask(Task task) {
        TaskResult result = new TaskResult(task);
        
        String query = (String) task.getInputData().get("query");
        String functionCalled = (String) task.getInputData().get("functionCalled");
        Object functionResult = task.getInputData().get("functionResult");
        
        try {
            String prompt = String.format(
                "The user asked: %s\n\nI called the '%s' function and got this result: %s\n\nPlease provide a natural, helpful response to the user based on this information.",
                query, functionCalled, functionResult
            );
            String model = USE_ANTHROPIC ? "claude-haiku-4-5" : "gpt-4o-mini";
            String response = USE_ANTHROPIC ? callClaudeChat(prompt, model, "You are a helpful assistant that formats function results into natural responses.") 
                                            : callOpenAiChat(prompt, model, "You are a helpful assistant that formats function results into natural responses.");
            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("response", response);
        } catch (Exception e) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("LLM API error: " + e.getMessage());
        }
        
        return result;
    }

    /**
     * Call OpenAI Chat Completion API.
     */
    private String callOpenAiChat(String prompt, String model, String systemPrompt) throws IOException, InterruptedException {
        Map<String, Object> requestBody = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", prompt)
            ),
            "max_tokens", 500,
            "temperature", 0.7
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + OPENAI_API_KEY)
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
            .timeout(Duration.ofSeconds(60))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("OpenAI API error: " + response.statusCode() + " - " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        return json.path("choices").path(0).path("message").path("content").asText();
    }

    /**
     * Call OpenAI with function calling to select appropriate tool.
     */
    private Map<String, Object> callOpenAiFunctionSelection(String query) throws IOException, InterruptedException {
        List<Map<String, Object>> tools = List.of(
            createToolDefinition("get_weather", "Get the current weather for a location", 
                Map.of("type", "object", "properties", Map.of(
                    "location", Map.of("type", "string", "description", "City name")
                ), "required", List.of("location"))),
            createToolDefinition("search_web", "Search the web for information",
                Map.of("type", "object", "properties", Map.of(
                    "query", Map.of("type", "string", "description", "Search query")
                ), "required", List.of("query"))),
            createToolDefinition("calculate", "Perform mathematical calculations",
                Map.of("type", "object", "properties", Map.of(
                    "expression", Map.of("type", "string", "description", "Math expression")
                ), "required", List.of("expression")))
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.put("messages", List.of(
            Map.of("role", "system", "content", "You are a helpful assistant. Use the available tools to answer the user's question."),
            Map.of("role", "user", "content", query)
        ));
        requestBody.put("tools", tools);
        requestBody.put("tool_choice", "auto");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + OPENAI_API_KEY)
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
            .timeout(Duration.ofSeconds(60))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("OpenAI API error: " + response.statusCode() + " - " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        JsonNode message = json.path("choices").path(0).path("message");
        
        // Check if a tool was called
        JsonNode toolCalls = message.path("tool_calls");
        if (toolCalls.isArray() && toolCalls.size() > 0) {
            JsonNode toolCall = toolCalls.get(0);
            String functionName = toolCall.path("function").path("name").asText();
            String argsJson = toolCall.path("function").path("arguments").asText();
            Map<String, Object> args = objectMapper.readValue(argsJson, Map.class);
            
            return Map.of(
                "name", functionName,
                "arguments", args,
                "reasoning", "LLM selected " + functionName + " based on query analysis"
            );
        } else {
            // No tool called, return direct response
            return Map.of(
                "name", "direct_response",
                "arguments", Map.of("query", query),
                "reasoning", "LLM chose to respond directly without tools"
            );
        }
    }

    /**
     * Call Anthropic Claude API for chat completion.
     */
    private String callClaudeChat(String prompt, String model, String systemPrompt) throws IOException, InterruptedException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", 500);
        requestBody.put("system", systemPrompt);
        requestBody.put("messages", List.of(
            Map.of("role", "user", "content", prompt)
        ));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.anthropic.com/v1/messages"))
            .header("Content-Type", "application/json")
            .header("x-api-key", ANTHROPIC_API_KEY)
            .header("anthropic-version", "2023-06-01")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
            .timeout(Duration.ofSeconds(60))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Claude API error: " + response.statusCode() + " - " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        return json.path("content").path(0).path("text").asText();
    }

    /**
     * Call Claude with tool use for function selection.
     */
    private Map<String, Object> callClaudeFunctionSelection(String query) throws IOException, InterruptedException {
        List<Map<String, Object>> tools = List.of(
            Map.of(
                "name", "get_weather",
                "description", "Get the current weather for a location",
                "input_schema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "location", Map.of("type", "string", "description", "City name")
                    ),
                    "required", List.of("location")
                )
            ),
            Map.of(
                "name", "search_web",
                "description", "Search the web for information",
                "input_schema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "query", Map.of("type", "string", "description", "Search query")
                    ),
                    "required", List.of("query")
                )
            ),
            Map.of(
                "name", "calculate",
                "description", "Perform mathematical calculations",
                "input_schema", Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "expression", Map.of("type", "string", "description", "Math expression")
                    ),
                    "required", List.of("expression")
                )
            )
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "claude-haiku-4-5");
        requestBody.put("max_tokens", 500);
        requestBody.put("system", "You are a helpful assistant. Use the available tools to answer the user's question.");
        requestBody.put("messages", List.of(Map.of("role", "user", "content", query)));
        requestBody.put("tools", tools);
        requestBody.put("tool_choice", Map.of("type", "auto"));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.anthropic.com/v1/messages"))
            .header("Content-Type", "application/json")
            .header("x-api-key", ANTHROPIC_API_KEY)
            .header("anthropic-version", "2023-06-01")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
            .timeout(Duration.ofSeconds(60))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new IOException("Claude API error: " + response.statusCode() + " - " + response.body());
        }

        JsonNode json = objectMapper.readTree(response.body());
        JsonNode content = json.path("content");
        
        // Check if a tool was called
        for (JsonNode block : content) {
            if ("tool_use".equals(block.path("type").asText())) {
                String functionName = block.path("name").asText();
                JsonNode inputNode = block.path("input");
                Map<String, Object> args = objectMapper.convertValue(inputNode, Map.class);
                
                return Map.of(
                    "name", functionName,
                    "arguments", args,
                    "reasoning", "Claude selected " + functionName + " based on query analysis"
                );
            }
        }
        
        // No tool called, return direct response
        return Map.of(
            "name", "direct_response",
            "arguments", Map.of("query", query),
            "reasoning", "Claude chose to respond directly without tools"
        );
    }

    private Map<String, Object> createToolDefinition(String name, String description, Map<String, Object> parameters) {
        return Map.of(
            "type", "function",
            "function", Map.of(
                "name", name,
                "description", description,
                "parameters", parameters
            )
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    //                  MULTI-AGENT CHAT WORKFLOW
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Multi-agent debate workflow: a moderator LLM routes turns between two LLM panelists.
     * Mirrors the Python SDK multiagent_chat.py example.
     */
    private void runMultiAgentChatWorkflow() {
        ConductorWorkflow<Map<String, Object>> workflow = new ConductorWorkflow<>(executor);
        workflow.setName("multiagent_chat_demo");
        workflow.setVersion(1);
        workflow.setOwnerEmail("examples@conductor-oss.org");
        workflow.setDescription("Multi-agent debate: moderator routes between two LLM panelists");

        // Init: set history variable
        SetVariable init = new SetVariable("init_ref");
        init.input("history", List.of(
            Map.of("role", "user", "message", "Discuss the following topic: ${workflow.input.topic}")
        ));
        init.input("last_speaker", "");

        // Build moderator messages (worker prepends system prompt to history)
        SimpleTask buildMessages = new SimpleTask("build_moderator_messages", "build_mod_msgs_ref");
        buildMessages.input("system_prompt",
            "You are a discussion moderator. Two panelists are debating: "
            + "${workflow.input.agent1_name} and ${workflow.input.agent2_name}.\n"
            + "Summarize the latest exchange, then ask a follow-up question to "
            + "one of them. Alternate fairly. The last speaker was: ${workflow.variables.last_speaker}.\n\n"
            + "Respond ONLY with valid JSON:\n"
            + "{\"result\": \"your moderator message\", \"user\": \"name_of_next_speaker\"}");
        buildMessages.input("history", "${workflow.variables.history}");

        // Moderator LLM (produces JSON: {result, user})
        LlmChatComplete moderatorTask = new LlmChatComplete("moderator_llm", "moderator_ref")
            .llmProvider("${workflow.input.llmProvider}")
            .model("${workflow.input.model}")
            .messages("${build_mod_msgs_ref.output.result}")
            .temperature(0.7)
            .maxTokens(500)
            .jsonOutput(true);

        // Agent 1 LLM
        LlmChatComplete agent1Task = new LlmChatComplete("agent1_llm", "agent1_ref")
            .llmProvider("${workflow.input.llmProvider}")
            .model("${workflow.input.model}")
            .messages(List.of(
                Map.of("role", "system", "message",
                    "You are ${workflow.input.agent1_name}. You reason and speak like this persona. "
                    + "You are in a panel discussion. Provide insightful analysis. "
                    + "Do not mention that you are an AI. Keep responses to 2-3 sentences.\n\n"
                    + "Topic: ${workflow.input.topic}"),
                Map.of("role", "user", "message", "${moderator_ref.output.result.result}")
            ))
            .temperature(0.8)
            .maxTokens(300);

        SimpleTask updateHistory1 = new SimpleTask("update_multiagent_history", "update_hist1_ref");
        updateHistory1.input("history", "${workflow.variables.history}");
        updateHistory1.input("moderator_message", "${moderator_ref.output.result.result}");
        updateHistory1.input("agent_name", "${workflow.input.agent1_name}");
        updateHistory1.input("agent_response", "${agent1_ref.output.result}");

        SetVariable saveVar1 = new SetVariable("save_var1_ref");
        saveVar1.input("history", "${update_hist1_ref.output.result}");
        saveVar1.input("last_speaker", "${workflow.input.agent1_name}");

        // Agent 2 LLM
        LlmChatComplete agent2Task = new LlmChatComplete("agent2_llm", "agent2_ref")
            .llmProvider("${workflow.input.llmProvider}")
            .model("${workflow.input.model}")
            .messages(List.of(
                Map.of("role", "system", "message",
                    "You are ${workflow.input.agent2_name}. You bring contrarian views and challenge assumptions. "
                    + "You are in a panel discussion. Be provocative but civil. "
                    + "Do not mention that you are an AI. Keep responses to 2-3 sentences.\n\n"
                    + "Topic: ${workflow.input.topic}"),
                Map.of("role", "user", "message", "${moderator_ref.output.result.result}")
            ))
            .temperature(0.8)
            .maxTokens(300);

        SimpleTask updateHistory2 = new SimpleTask("update_multiagent_history", "update_hist2_ref");
        updateHistory2.input("history", "${workflow.variables.history}");
        updateHistory2.input("moderator_message", "${moderator_ref.output.result.result}");
        updateHistory2.input("agent_name", "${workflow.input.agent2_name}");
        updateHistory2.input("agent_response", "${agent2_ref.output.result}");

        SetVariable saveVar2 = new SetVariable("save_var2_ref");
        saveVar2.input("history", "${update_hist2_ref.output.result}");
        saveVar2.input("last_speaker", "${workflow.input.agent2_name}");

        // Route to agent1 or agent2 based on moderator's pick
        // Use a simplified routing: if moderator picks agent2, route there, else agent1
        Switch router = new Switch("route_ref", "${moderator_ref.output.result.user}");
        router.switchCase("${workflow.input.agent2_name}", agent2Task, updateHistory2, saveVar2);
        router.defaultCase(agent1Task, updateHistory1, saveVar1);

        // Loop: build messages -> moderate -> route to agent
        DoWhile loop = new DoWhile("loop_ref", 2, buildMessages, moderatorTask, router);

        workflow.add(init);
        workflow.add(loop);
        workflow.registerWorkflow(true, true);
        pass("Multi-Agent Chat workflow registered");

        Map<String, Object> input = new HashMap<>();
        input.put("topic", "The impact of artificial intelligence on employment");
        input.put("agent1_name", "optimistic technologist");
        input.put("agent2_name", "cautious economist");
        input.put("llmProvider", "openai");
        input.put("model", "gpt-4o-mini");

        try {
            var run = workflow.execute(input);
            Workflow result = run.get(120, TimeUnit.SECONDS);

            String workflowId = result.getWorkflowId();
            String status = result.getStatus().toString();
            executedWorkflows.add(new WorkflowExecution("multiagent_chat_demo", workflowId, status));

            if (result.getStatus() == Workflow.WorkflowStatus.COMPLETED) {
                pass("Workflow executed: " + workflowId + " [" + status + "]");
            } else {
                fail("Workflow failed: " + workflowId + " [" + status + "] - " + result.getReasonForIncompletion());
            }
        } catch (Exception e) {
            fail("Multi-agent chat error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //                  HUMAN-IN-THE-LOOP CHAT WORKFLOW
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Human-in-the-loop interactive chat: workflow pauses at a WAIT task for user input.
     * Mirrors the Python SDK llm_chat_human_in_loop.py example.
     * In this automated test we simulate the user by updating the wait task externally.
     */
    private void runHumanInLoopChatWorkflow() {
        ConductorWorkflow<Map<String, Object>> workflow = new ConductorWorkflow<>(executor);
        workflow.setName("llm_chat_human_in_loop");
        workflow.setVersion(1);
        workflow.setOwnerEmail("examples@conductor-oss.org");
        workflow.setDescription("Human-in-the-loop chat using WAIT task for user input");

        // Wait for user input
        Wait userInput = new Wait("user_input_ref");

        // Collect conversation history
        SimpleTask collectHistoryTask = new SimpleTask("chat_collect_history", "collect_history_ref");
        collectHistoryTask.input("user_input", "${user_input_ref.output.question}");
        collectHistoryTask.input("history", "${chat_complete_ref.input.messages}");
        collectHistoryTask.input("assistant_response", "${chat_complete_ref.output.result}");
        collectHistoryTask.input("system_prompt",
            "You are a helpful assistant that knows about science. "
            + "Answer questions clearly and concisely.");

        // Chat completion
        LlmChatComplete chatComplete = new LlmChatComplete("chat_llm", "chat_complete_ref")
            .llmProvider("${workflow.input.llmProvider}")
            .model("${workflow.input.model}")
            .messages("${collect_history_ref.output.result}")
            .temperature(0.7)
            .maxTokens(300);

        // Loop: wait -> collect -> respond (2 turns)
        DoWhile chatLoop = new DoWhile("loop_ref", 2, userInput, collectHistoryTask, chatComplete);

        workflow.add(chatLoop);
        workflow.registerWorkflow(true, true);
        pass("Human-in-the-loop Chat workflow registered");

        Map<String, Object> input = new HashMap<>();
        input.put("llmProvider", "openai");
        input.put("model", "gpt-4o-mini");

        try {
            // Start the workflow - it will pause at the first WAIT task
            StartWorkflowRequest swReq = new StartWorkflowRequest();
            swReq.setName("llm_chat_human_in_loop");
            swReq.setVersion(1);
            swReq.setInput(input);
            String workflowId = workflowClient.startWorkflow(swReq);
            pass("Workflow started: " + workflowId);

            // Simulate user responding to turn 1
            for (int turn = 1; turn <= 2; turn++) {
                // Wait for the WAIT task to appear
                // Inside a DoWhile loop the task ref gets suffixed with __1, __2, etc.
                String taskRef = "user_input_ref__" + turn;
                String question = (turn == 1) ? "What is photosynthesis?" : "How does it produce oxygen?";
                boolean resolved = waitAndUpdateWaitTask(workflowId, taskRef, question, 30);
                if (!resolved) {
                    fail("Timed out waiting for WAIT task in turn " + turn);
                    break;
                }
                pass("Turn " + turn + ": submitted question: " + question);
                Thread.sleep(3000); // give LLM time to respond
            }

            // Wait for completion
            long deadline = System.currentTimeMillis() + 90_000;
            Workflow result = null;
            while (System.currentTimeMillis() < deadline) {
                result = workflowClient.getWorkflow(workflowId, false);
                if (result.getStatus() != Workflow.WorkflowStatus.RUNNING) break;
                Thread.sleep(2000);
            }

            if (result != null) {
                String status = result.getStatus().toString();
                executedWorkflows.add(new WorkflowExecution("llm_chat_human_in_loop", workflowId, status));
                if (result.getStatus() == Workflow.WorkflowStatus.COMPLETED) {
                    pass("Workflow completed: " + workflowId + " [" + status + "]");
                } else {
                    fail("Workflow did not complete: " + workflowId + " [" + status + "]");
                }
            }
        } catch (Exception e) {
            fail("Human-in-loop chat error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            e.printStackTrace();
        }
    }

    /**
     * Wait for a WAIT task to be scheduled on a workflow, then complete it with user input.
     */
    private boolean waitAndUpdateWaitTask(String workflowId, String taskRefName, String question, int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        while (System.currentTimeMillis() < deadline) {
            try {
                Workflow wf = workflowClient.getWorkflow(workflowId, true);
                for (var task : wf.getTasks()) {
                    if (taskRefName.equals(task.getReferenceTaskName())
                            && task.getStatus() == com.netflix.conductor.common.metadata.tasks.Task.Status.IN_PROGRESS) {
                        // Complete the WAIT task
                        com.netflix.conductor.common.metadata.tasks.TaskResult taskResult =
                            new com.netflix.conductor.common.metadata.tasks.TaskResult(task);
                        taskResult.setStatus(com.netflix.conductor.common.metadata.tasks.TaskResult.Status.COMPLETED);
                        taskResult.addOutputData("question", question);
                        taskClient.updateTask(taskResult);
                        return true;
                    }
                }
                Thread.sleep(1000);
            } catch (Exception e) {
                // retry
            }
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════════════
    //                  MCP WEATHER AGENT WORKFLOW
    // ═══════════════════════════════════════════════════════════════════

    /**
     * MCP Weather Agent workflow: uses ListMcpTools + LLM planning + CallMcpTool.
     * Mirrors the Python SDK mcp_weather_agent.py example.
     */
    private void runMcpWeatherAgentWorkflow() {
        String mcpServer = System.getenv().getOrDefault("MCP_SERVER_URL", "http://localhost:3001/mcp");

        ConductorWorkflow<Map<String, Object>> workflow = new ConductorWorkflow<>(executor);
        workflow.setName("mcp_ai_agent");
        workflow.setVersion(1);
        workflow.setOwnerEmail("examples@conductor-oss.org");
        workflow.setDescription("AI agent using MCP tools for weather queries");

        // Step 1: Discover available MCP tools (worker bridges to MCP server)
        SimpleTask listTools = new SimpleTask("mcp_list_tools_worker", "discover_tools_ref");
        listTools.input("mcpServer", mcpServer);

        // Step 2: Ask LLM to plan which tool to use (returns JSON)
        LlmChatComplete planTask = new LlmChatComplete("plan_action_llm", "plan_action_ref")
            .llmProvider("${workflow.input.llmProvider}")
            .model("${workflow.input.model}")
            .messages(List.of(
                Map.of("role", "system", "message",
                    "You are an AI agent that can use tools to help users.\n\n"
                    + "Available tools:\n${discover_tools_ref.output.tools}\n\n"
                    + "User's request:\n${workflow.input.request}\n\n"
                    + "Decide which tool to use. Respond ONLY with this JSON:\n"
                    + "{\"method\": \"tool_name\", \"arguments\": {\"param\": \"value\"}, "
                    + "\"reasoning\": \"why you chose this tool\"}"),
                Map.of("role", "user", "message", "What tool should I use?")
            ))
            .temperature(0.1)
            .maxTokens(500)
            .jsonOutput(true);

        // Step 3: Execute the selected tool via MCP (worker bridges to MCP server)
        SimpleTask executeTool = new SimpleTask("mcp_call_tool_worker", "execute_tool_ref");
        executeTool.input("mcpServer", mcpServer);
        executeTool.input("method", "${plan_action_ref.output.result.method}");
        executeTool.input("arguments", "${plan_action_ref.output.result.arguments}");

        // Step 4: Summarize the result
        LlmChatComplete summarizeTask = new LlmChatComplete("summarize_result_llm", "summarize_result_ref")
            .llmProvider("${workflow.input.llmProvider}")
            .model("${workflow.input.model}")
            .messages(List.of(
                Map.of("role", "system", "message",
                    "You are a helpful assistant. Summarize the tool result.\n\n"
                    + "Original request: ${workflow.input.request}\n"
                    + "Tool used: ${plan_action_ref.output.result.method}\n"
                    + "Tool result: ${execute_tool_ref.output.content}\n\n"
                    + "Provide a natural, conversational response."),
                Map.of("role", "user", "message", "Please summarize the result")
            ))
            .temperature(0.3)
            .maxTokens(300);

        workflow.add(listTools);
        workflow.add(planTask);
        workflow.add(executeTool);
        workflow.add(summarizeTask);

        workflow.setWorkflowOutput(Map.of(
            "plan", "${plan_action_ref.output.result}",
            "tool_result", "${execute_tool_ref.output.content}",
            "summary", "${summarize_result_ref.output.result}"
        ));
        workflow.registerWorkflow(true, true);
        pass("MCP Weather Agent workflow registered");

        Map<String, Object> input = new HashMap<>();
        input.put("request", "What's the weather like in San Francisco?");
        input.put("llmProvider", "openai");
        input.put("model", "gpt-4o-mini");

        try {
            var run = workflow.execute(input);
            Workflow result = run.get(120, TimeUnit.SECONDS);

            String workflowId = result.getWorkflowId();
            String status = result.getStatus().toString();
            executedWorkflows.add(new WorkflowExecution("mcp_ai_agent", workflowId, status));

            if (result.getStatus() == Workflow.WorkflowStatus.COMPLETED) {
                pass("Workflow executed: " + workflowId + " [" + status + "]");
                Object summary = result.getOutput().get("summary");
                if (summary != null) {
                    String summaryStr = summary.toString();
                    if (summaryStr.length() > 120) summaryStr = summaryStr.substring(0, 120) + "...";
                    pass("Agent response: " + summaryStr);
                }
            } else {
                fail("Workflow failed: " + workflowId + " [" + status + "] - " + result.getReasonForIncompletion());
            }
        } catch (Exception e) {
            fail("MCP Weather Agent error: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            e.printStackTrace();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //         WORKER IMPLEMENTATIONS FOR NEW WORKFLOWS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Collects conversation history for multi-turn chat.
     * Appends assistant response and new user input to history list.
     */
    @SuppressWarnings("unchecked")
    private TaskResult executeChatCollectHistoryTask(Task task) {
        TaskResult result = new TaskResult(task);

        String userInput = getStringOrNull(task.getInputData().get("user_input"));
        String seedQuestion = getStringOrNull(task.getInputData().get("seed_question"));
        String assistantResponse = getStringOrNull(task.getInputData().get("assistant_response"));
        String systemPrompt = getStringOrNull(task.getInputData().get("system_prompt"));
        Object historyObj = task.getInputData().get("history");

        List<Map<String, Object>> history = new ArrayList<>();

        // Add system prompt as first message if provided and history is empty
        if (historyObj instanceof List) {
            for (Object item : (List<?>) historyObj) {
                if (item instanceof Map) {
                    Map<String, Object> msg = (Map<String, Object>) item;
                    if (msg.containsKey("role") && msg.containsKey("message")) {
                        history.add(Map.of("role", msg.get("role").toString(), "message", msg.get("message").toString()));
                    }
                }
            }
        }

        // If history is empty and we have a system prompt, add it first
        if (history.isEmpty() && systemPrompt != null && !systemPrompt.startsWith("$")) {
            history.add(Map.of("role", "system", "message", systemPrompt));
        }

        if (assistantResponse != null && !assistantResponse.startsWith("$")) {
            history.add(Map.of("role", "assistant", "message", assistantResponse));
        }

        if (userInput != null && !userInput.startsWith("$")) {
            history.add(Map.of("role", "user", "message", userInput));
        } else if (seedQuestion != null && !seedQuestion.startsWith("$")) {
            history.add(Map.of("role", "user", "message", seedQuestion));
        }

        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("result", history);
        return result;
    }

    /**
     * Prepends a system prompt to conversation history for the moderator LLM.
     */
    @SuppressWarnings("unchecked")
    private TaskResult executeBuildModeratorMessagesTask(Task task) {
        TaskResult result = new TaskResult(task);

        String systemPrompt = getStringOrNull(task.getInputData().get("system_prompt"));
        Object historyObj = task.getInputData().get("history");

        List<Map<String, Object>> messages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.startsWith("$")) {
            messages.add(Map.of("role", "system", "message", systemPrompt));
        }

        if (historyObj instanceof List) {
            for (Object item : (List<?>) historyObj) {
                if (item instanceof Map) {
                    Map<String, Object> msg = (Map<String, Object>) item;
                    if (msg.containsKey("role") && msg.containsKey("message")) {
                        String msgText = msg.get("message").toString();
                        if (!msgText.startsWith("$")) {
                            messages.add(Map.of("role", msg.get("role").toString(), "message", msgText));
                        }
                    }
                }
            }
        }

        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("result", messages);
        return result;
    }

    /**
     * Appends moderator summary and agent response to the conversation history.
     */
    @SuppressWarnings("unchecked")
    private TaskResult executeUpdateMultiagentHistoryTask(Task task) {
        TaskResult result = new TaskResult(task);

        Object historyObj = task.getInputData().get("history");
        String moderatorMessage = getStringOrNull(task.getInputData().get("moderator_message"));
        String agentName = getStringOrNull(task.getInputData().get("agent_name"));
        String agentResponse = getStringOrNull(task.getInputData().get("agent_response"));

        List<Map<String, Object>> history = new ArrayList<>();

        if (historyObj instanceof List) {
            for (Object item : (List<?>) historyObj) {
                if (item instanceof Map) {
                    Map<String, Object> msg = (Map<String, Object>) item;
                    if (msg.containsKey("role") && msg.containsKey("message")) {
                        history.add(Map.of("role", msg.get("role").toString(), "message", msg.get("message").toString()));
                    }
                }
            }
        }

        if (moderatorMessage != null && !moderatorMessage.startsWith("$")) {
            history.add(Map.of("role", "assistant", "message", moderatorMessage));
        }

        if (agentResponse != null && !agentResponse.startsWith("$")) {
            String prefix = (agentName != null && !agentName.startsWith("$")) ? "[" + agentName + "] " : "";
            history.add(Map.of("role", "user", "message", prefix + agentResponse));
        }

        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("result", history);
        return result;
    }

    /** Helper to safely get a String from a task input, returning null if not a String. */
    private String getStringOrNull(Object value) {
        if (value instanceof String) return (String) value;
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════
    //                  MCP WORKER IMPLEMENTATIONS
    // ═══════════════════════════════════════════════════════════════════

    /**
     * MCP List Tools worker - calls the MCP server to discover available tools.
     * Bridges to MCP server via HTTP for OSS Conductor (which lacks native MCP_LIST_TOOLS).
     */
    private TaskResult executeMcpListToolsTask(Task task) {
        TaskResult result = new TaskResult(task);
        String mcpServer = (String) task.getInputData().getOrDefault("mcpServer", "http://localhost:3001/mcp");

        try {
            String responseBody = callMcpServer(mcpServer, "tools/list", Map.of());
            JsonNode json = parseSseResponse(responseBody);
            JsonNode tools = json.path("result").path("tools");
            
            // Convert tools to a list of simple maps for LLM consumption
            List<Map<String, Object>> toolList = new ArrayList<>();
            for (JsonNode tool : tools) {
                Map<String, Object> t = new HashMap<>();
                t.put("name", tool.path("name").asText());
                t.put("description", tool.path("description").asText());
                JsonNode schema = tool.path("inputSchema");
                if (!schema.isMissingNode()) {
                    t.put("parameters", objectMapper.convertValue(schema, Map.class));
                }
                toolList.add(t);
            }
            
            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("tools", toolList);
            result.addOutputData("toolCount", toolList.size());
        } catch (Exception e) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("MCP list tools failed: " + e.getMessage());
        }
        return result;
    }

    /**
     * MCP Call Tool worker - calls a specific tool on the MCP server.
     * Bridges to MCP server via HTTP for OSS Conductor (which lacks native MCP_CALL_TOOL).
     */
    @SuppressWarnings("unchecked")
    private TaskResult executeMcpCallToolTask(Task task) {
        TaskResult result = new TaskResult(task);
        String mcpServer = (String) task.getInputData().getOrDefault("mcpServer", "http://localhost:3001/mcp");
        Object methodObj = task.getInputData().get("method");
        Object argsObj = task.getInputData().get("arguments");

        String method = methodObj != null ? methodObj.toString() : "";
        Map<String, Object> arguments = Map.of();
        if (argsObj instanceof Map) {
            arguments = (Map<String, Object>) argsObj;
        } else if (argsObj instanceof String) {
            try {
                arguments = objectMapper.readValue((String) argsObj, Map.class);
            } catch (Exception e) {
                // use empty args
            }
        }

        if (method.isEmpty() || method.startsWith("$")) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("No method specified for MCP call");
            return result;
        }

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("name", method);
            params.put("arguments", arguments);
            
            String responseBody = callMcpServer(mcpServer, "tools/call", params);
            JsonNode json = parseSseResponse(responseBody);
            JsonNode toolResult = json.path("result");
            
            result.setStatus(TaskResult.Status.COMPLETED);
            // Extract text content from the MCP response
            JsonNode content = toolResult.path("content");
            if (content.isArray() && content.size() > 0) {
                StringBuilder textContent = new StringBuilder();
                for (JsonNode item : content) {
                    if ("text".equals(item.path("type").asText())) {
                        textContent.append(item.path("text").asText());
                    }
                }
                result.addOutputData("content", textContent.toString());
            } else {
                result.addOutputData("content", objectMapper.writeValueAsString(toolResult));
            }
            result.addOutputData("isError", toolResult.path("isError").asBoolean(false));
            result.addOutputData("raw", objectMapper.convertValue(toolResult, Map.class));
        } catch (Exception e) {
            result.setStatus(TaskResult.Status.FAILED);
            result.setReasonForIncompletion("MCP call tool failed: " + e.getMessage());
        }
        return result;
    }

    /**
     * Make an HTTP call to the MCP server using JSON-RPC over SSE transport.
     */
    private String callMcpServer(String mcpServer, String method, Map<String, Object> params) 
            throws IOException, InterruptedException {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("jsonrpc", "2.0");
        requestBody.put("id", System.currentTimeMillis());
        requestBody.put("method", method);
        requestBody.put("params", params);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(mcpServer))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
            .timeout(Duration.ofSeconds(30))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new IOException("MCP server error " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    /**
     * Parse SSE (Server-Sent Events) response from MCP server.
     * MCP responses are formatted as: "event: message\ndata: {json}\n\n"
     */
    private JsonNode parseSseResponse(String sseBody) throws IOException {
        // Parse SSE format: lines starting with "data: " contain the JSON
        for (String line : sseBody.split("\n")) {
            if (line.startsWith("data: ")) {
                String jsonData = line.substring(6).trim();
                return objectMapper.readTree(jsonData);
            }
        }
        // Try to parse directly as JSON (non-SSE response)
        return objectMapper.readTree(sseBody);
    }
}
