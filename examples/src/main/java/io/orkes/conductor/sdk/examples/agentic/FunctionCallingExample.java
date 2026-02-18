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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.sdk.workflow.def.ConductorWorkflow;
import com.netflix.conductor.sdk.workflow.def.tasks.LlmChatComplete;
import com.netflix.conductor.sdk.workflow.def.tasks.SimpleTask;
import com.netflix.conductor.sdk.workflow.def.tasks.Switch;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;

import io.orkes.conductor.sdk.examples.util.ClientUtil;

/**
 * Example demonstrating LLM function calling with Conductor.
 * <p>
 * This example shows how an LLM can dynamically select which tool/function to call
 * based on user queries. The workflow:
 * <ul>
 *   <li>Receives a user query</li>
 *   <li>LLM analyzes the query and decides which function to call</li>
 *   <li>A SWITCH task routes to the appropriate worker based on LLM's decision</li>
 *   <li>The worker executes and returns results</li>
 *   <li>LLM formats the final response</li>
 * </ul>
 * 
 * <p>This pattern is the foundation for building AI agents that can:
 * <ul>
 *   <li>Call APIs dynamically</li>
 *   <li>Execute code</li>
 *   <li>Query databases</li>
 *   <li>Interact with external systems</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>
 * ./gradlew :examples:run -PmainClass=io.orkes.conductor.sdk.examples.agentic.FunctionCallingExample
 * </pre>
 */
public class FunctionCallingExample {

    private static final String WORKFLOW_NAME = "llm_function_calling";
    private static final String LLM_PROVIDER = "openai";
    private static final String MODEL = "gpt-4";

    public static void main(String[] args) {
        String query = args.length > 0 ? String.join(" ", args) : "What's the weather like in San Francisco?";
        
        System.out.println("Starting LLM Function Calling Example");
        System.out.println("Query: " + query);
        System.out.println();

        // Initialize Conductor client
        ConductorClient client = ClientUtil.getClient();
        WorkflowExecutor executor = new WorkflowExecutor(client, 10);

        try {
            // Initialize workers for the functions
            executor.initWorkers("io.orkes.conductor.sdk.examples.agentic.workers");
            
            // Create and register the workflow
            ConductorWorkflow<Map<String, Object>> workflow = createFunctionCallingWorkflow(executor);
            workflow.registerWorkflow(true, true);
            System.out.println("Workflow registered: " + WORKFLOW_NAME);

            // Execute the workflow
            Map<String, Object> input = new HashMap<>();
            input.put("query", query);
            input.put("llmProvider", LLM_PROVIDER);
            input.put("model", MODEL);

            var workflowRun = workflow.execute(input);
            Workflow result = workflowRun.get(60, TimeUnit.SECONDS);
            
            System.out.println("\n=== Execution Complete ===");
            System.out.println("Status: " + result.getStatus());
            System.out.println("\nFinal Response:");
            System.out.println(result.getOutput().get("response"));

        } catch (Exception e) {
            System.err.println("Error executing workflow: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Creates a workflow that uses LLM to decide which function to call.
     */
    private static ConductorWorkflow<Map<String, Object>> createFunctionCallingWorkflow(WorkflowExecutor executor) {
        ConductorWorkflow<Map<String, Object>> workflow = new ConductorWorkflow<>(executor);
        workflow.setName(WORKFLOW_NAME);
        workflow.setVersion(1);
        workflow.setOwnerEmail("examples@conductor-oss.org");
        workflow.setDescription("LLM function calling with dynamic tool selection");

        // Define available tools/functions
        List<Object> tools = new ArrayList<>();
        tools.add(createToolDefinition("get_weather", 
            "Get the current weather for a location",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "location", Map.of("type", "string", "description", "City and state/country")
                ),
                "required", List.of("location")
            )));
        tools.add(createToolDefinition("search_web",
            "Search the web for information",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "query", Map.of("type", "string", "description", "Search query")
                ),
                "required", List.of("query")
            )));
        tools.add(createToolDefinition("calculate",
            "Perform mathematical calculations",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "expression", Map.of("type", "string", "description", "Math expression to evaluate")
                ),
                "required", List.of("expression")
            )));

        // Step 1: LLM decides which function to call
        LlmChatComplete functionSelector = new LlmChatComplete("select_function", "selector_ref")
            .llmProvider("${workflow.input.llmProvider}")
            .model("${workflow.input.model}")
            .messages(List.of(
                Map.of("role", "system", "content", 
                    "You are a helpful assistant that selects the appropriate function to answer user queries. " +
                    "Analyze the user's request and call the most appropriate function."),
                Map.of("role", "user", "content", "${workflow.input.query}")
            ))
            .tools(tools)
            .toolChoice("auto")
            .temperature(0.0);

        workflow.add(functionSelector);

        // Step 2: Switch based on which function was selected
        Switch functionRouter = new Switch("router_ref", 
            "${selector_ref.output.toolCalls[0].function.name}");

        // Weather function worker
        SimpleTask weatherTask = new SimpleTask("get_weather", "weather_ref");
        weatherTask.input("location", "${selector_ref.output.toolCalls[0].function.arguments.location}");
        functionRouter.switchCase("get_weather", weatherTask);

        // Web search function worker  
        SimpleTask searchTask = new SimpleTask("search_web", "search_ref");
        searchTask.input("query", "${selector_ref.output.toolCalls[0].function.arguments.query}");
        functionRouter.switchCase("search_web", searchTask);

        // Calculator function worker
        SimpleTask calcTask = new SimpleTask("calculate", "calc_ref");
        calcTask.input("expression", "${selector_ref.output.toolCalls[0].function.arguments.expression}");
        functionRouter.switchCase("calculate", calcTask);

        // Default case - direct response without function call
        SimpleTask defaultTask = new SimpleTask("direct_response", "default_ref");
        defaultTask.input("query", "${workflow.input.query}");
        functionRouter.defaultCase(defaultTask);

        workflow.add(functionRouter);

        // Step 3: LLM formats the final response
        LlmChatComplete responseFormatter = new LlmChatComplete("format_response", "formatter_ref")
            .llmProvider("${workflow.input.llmProvider}")
            .model("${workflow.input.model}")
            .messages(List.of(
                Map.of("role", "system", "content", 
                    "Format the function result into a helpful, natural language response for the user."),
                Map.of("role", "user", "content", "Original query: ${workflow.input.query}"),
                Map.of("role", "assistant", "content", "I called a function to help answer your query."),
                Map.of("role", "function", "name", "${selector_ref.output.toolCalls[0].function.name}",
                    "content", "${router_ref.output.result}")
            ))
            .temperature(0.7)
            .maxTokens(300);

        workflow.add(responseFormatter);

        // Set output
        Map<String, Object> outputParams = new HashMap<>();
        outputParams.put("response", "${formatter_ref.output.result}");
        outputParams.put("functionCalled", "${selector_ref.output.toolCalls[0].function.name}");
        outputParams.put("functionArgs", "${selector_ref.output.toolCalls[0].function.arguments}");
        workflow.setWorkflowOutput(outputParams);

        return workflow;
    }

    /**
     * Creates a tool definition in OpenAI function calling format.
     */
    private static Map<String, Object> createToolDefinition(String name, String description, 
            Map<String, Object> parameters) {
        return Map.of(
            "type", "function",
            "function", Map.of(
                "name", name,
                "description", description,
                "parameters", parameters
            )
        );
    }
}
