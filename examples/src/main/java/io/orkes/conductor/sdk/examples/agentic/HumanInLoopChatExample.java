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
import java.util.Scanner;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.sdk.workflow.def.ConductorWorkflow;
import com.netflix.conductor.sdk.workflow.def.tasks.DoWhile;
import com.netflix.conductor.sdk.workflow.def.tasks.LlmChatComplete;
import com.netflix.conductor.sdk.workflow.def.tasks.SetVariable;
import com.netflix.conductor.sdk.workflow.def.tasks.Wait;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;

import io.orkes.conductor.client.http.OrkesPromptClient;
import io.orkes.conductor.sdk.examples.util.ClientUtil;

/**
 * Example demonstrating interactive human-in-the-loop chat with an LLM.
 * <p>
 * This example creates a workflow where:
 * <ul>
 *   <li>A WAIT task pauses for human input</li>
 *   <li>The LLM processes the human's message and responds</li>
 *   <li>The conversation continues until the user types "quit"</li>
 * </ul>
 * 
 * <p>The WAIT task allows external signals to provide input, making this
 * perfect for interactive chatbot applications where human input is needed.
 * 
 * <p>Prerequisites:
 * <ul>
 *   <li>Conductor server running with AI/LLM integration configured</li>
 *   <li>OpenAI (or other LLM provider) integration set up in Conductor</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>
 * ./gradlew :examples:run -PmainClass=io.orkes.conductor.sdk.examples.agentic.HumanInLoopChatExample
 * </pre>
 */
public class HumanInLoopChatExample {

    private static final String WORKFLOW_NAME = "human_in_loop_chat";
    private static final String LLM_PROVIDER = "openai";
    private static final String MODEL = "gpt-4";
    private static final int MAX_TURNS = 10;

    public static void main(String[] args) {
        System.out.println("Starting Human-in-the-Loop Chat Example");
        System.out.println("Type your messages and press Enter. Type 'quit' to exit.\n");

        // Initialize Conductor client
        ConductorClient client = ClientUtil.getClient();
        WorkflowExecutor executor = new WorkflowExecutor(client, 10);
        WorkflowClient workflowClient = new WorkflowClient(client);
        TaskClient taskClient = new TaskClient(client);

        try {
            // Register prompt
            registerPrompt(client);
            
            // Create and register the workflow
            ConductorWorkflow<Map<String, Object>> workflow = createChatWorkflow(executor);
            workflow.registerWorkflow(true, true);
            System.out.println("Workflow registered: " + WORKFLOW_NAME);

            // Start the workflow
            Map<String, Object> input = new HashMap<>();
            input.put("llmProvider", LLM_PROVIDER);
            input.put("model", MODEL);
            input.put("systemPrompt", "You are a helpful, friendly AI assistant. Be concise in your responses.");
            
            String workflowId = workflow.startDynamic(input);
            System.out.println("Started workflow: " + workflowId);
            System.out.println("\n=== Chat Started ===\n");

            // Interactive chat loop
            Scanner scanner = new Scanner(System.in);
            List<Map<String, String>> conversationHistory = new ArrayList<>();
            
            for (int turn = 0; turn < MAX_TURNS; turn++) {
                // Wait for the workflow to reach the WAIT task
                Workflow wf = waitForWaitTask(workflowClient, workflowId);
                
                if (wf.getStatus() != Workflow.WorkflowStatus.RUNNING) {
                    System.out.println("Workflow ended with status: " + wf.getStatus());
                    break;
                }

                // Get user input
                System.out.print("\nYou: ");
                String userInput = scanner.nextLine().trim();
                
                if ("quit".equalsIgnoreCase(userInput)) {
                    System.out.println("\nEnding conversation...");
                    workflowClient.terminateWorkflow(workflowId, "User requested quit");
                    break;
                }

                // Add user message to history
                Map<String, String> userMessage = new HashMap<>();
                userMessage.put("role", "user");
                userMessage.put("content", userInput);
                conversationHistory.add(userMessage);

                // Find the WAIT task and complete it with user input
                completeWaitTask(taskClient, wf, userInput, conversationHistory);
                
                // Wait for LLM response
                Thread.sleep(1000); // Give time for the LLM task to process
                
                wf = workflowClient.getWorkflow(workflowId, true);
                
                // Get the assistant's response from workflow variables or task output
                String assistantResponse = getAssistantResponse(wf);
                if (assistantResponse != null) {
                    System.out.println("\nAssistant: " + assistantResponse);
                    
                    Map<String, String> assistantMessage = new HashMap<>();
                    assistantMessage.put("role", "assistant");
                    assistantMessage.put("content", assistantResponse);
                    conversationHistory.add(assistantMessage);
                }
            }

            System.out.println("\n=== Chat Ended ===");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Creates an interactive chat workflow with WAIT tasks for human input.
     */
    private static ConductorWorkflow<Map<String, Object>> createChatWorkflow(WorkflowExecutor executor) {
        ConductorWorkflow<Map<String, Object>> workflow = new ConductorWorkflow<>(executor);
        workflow.setName(WORKFLOW_NAME);
        workflow.setVersion(1);
        workflow.setOwnerEmail("examples@conductor-oss.org");
        workflow.setDescription("Interactive chat with human-in-the-loop using WAIT tasks");

        // Initialize conversation state
        Map<String, Object> initialVariables = new HashMap<>();
        initialVariables.put("conversationHistory", new ArrayList<>());
        initialVariables.put("turnCount", 0);
        initialVariables.put("shouldContinue", true);
        workflow.setVariables(initialVariables);

        // WAIT task for human input
        Wait waitForInput = new Wait("wait_for_input_ref");
        waitForInput.input("waitType", "signal");

        // Process user input and update state
        SetVariable processInput = new SetVariable("process_input_ref");
        processInput.input("userMessage", "${wait_for_input_ref.output.userMessage}");
        processInput.input("turnCount", "${workflow.variables.turnCount + 1}");

        // LLM chat task
        LlmChatComplete chatTask = new LlmChatComplete("chat_llm", "chat_ref")
            .llmProvider("${workflow.input.llmProvider}")
            .model("${workflow.input.model}")
            .messages("${workflow.variables.conversationHistory}")
            .temperature(0.7)
            .maxTokens(500);

        // Update conversation history with response
        SetVariable updateHistory = new SetVariable("update_history_ref");
        updateHistory.input("lastResponse", "${chat_ref.output.result}");

        // DO_WHILE loop for conversation turns
        DoWhile chatLoop = new DoWhile(
            "chat_loop_ref",
            MAX_TURNS,
            waitForInput, processInput, chatTask, updateHistory
        );
        
        workflow.add(chatLoop);

        // Set output
        Map<String, Object> outputParams = new HashMap<>();
        outputParams.put("conversationHistory", "${workflow.variables.conversationHistory}");
        outputParams.put("totalTurns", "${workflow.variables.turnCount}");
        workflow.setWorkflowOutput(outputParams);

        return workflow;
    }

    /**
     * Waits for the workflow to reach a WAIT task.
     */
    private static Workflow waitForWaitTask(WorkflowClient workflowClient, String workflowId) throws InterruptedException {
        while (true) {
            Workflow wf = workflowClient.getWorkflow(workflowId, true);
            
            if (wf.getStatus() != Workflow.WorkflowStatus.RUNNING) {
                return wf;
            }
            
            // Check if there's a WAIT task in progress
            boolean hasWaitTask = wf.getTasks().stream()
                .anyMatch(t -> "WAIT".equals(t.getTaskType()) && 
                         t.getStatus() == com.netflix.conductor.common.metadata.tasks.Task.Status.IN_PROGRESS);
            
            if (hasWaitTask) {
                return wf;
            }
            
            Thread.sleep(500);
        }
    }

    /**
     * Completes the WAIT task with user input.
     */
    private static void completeWaitTask(TaskClient taskClient, Workflow wf, 
            String userInput, List<Map<String, String>> history) {
        wf.getTasks().stream()
            .filter(t -> "WAIT".equals(t.getTaskType()) && 
                    t.getStatus() == com.netflix.conductor.common.metadata.tasks.Task.Status.IN_PROGRESS)
            .findFirst()
            .ifPresent(task -> {
                TaskResult result = new TaskResult(task);
                result.setStatus(TaskResult.Status.COMPLETED);
                result.addOutputData("userMessage", userInput);
                result.addOutputData("conversationHistory", history);
                taskClient.updateTask(result);
            });
    }

    /**
     * Extracts the assistant's response from the workflow.
     */
    private static String getAssistantResponse(Workflow wf) {
        // Try to get from the last completed LLM task
        return wf.getTasks().stream()
            .filter(t -> "LLM_CHAT_COMPLETE".equals(t.getTaskType()) && 
                    t.getStatus() == com.netflix.conductor.common.metadata.tasks.Task.Status.COMPLETED)
            .reduce((first, second) -> second) // Get last one
            .map(t -> {
                Object result = t.getOutputData().get("result");
                return result != null ? result.toString() : null;
            })
            .orElse(null);
    }

    /**
     * Registers the prompt template.
     */
    private static void registerPrompt(ConductorClient client) {
        OrkesPromptClient promptClient = new OrkesPromptClient(client);

        String chatPrompt = "You are a helpful AI assistant.\n\n" +
            "System: {{systemPrompt}}\n\n" +
            "Conversation so far:\n{{conversationHistory}}\n\n" +
            "Respond to the user's latest message thoughtfully and helpfully.";
        
        try {
            promptClient.savePrompt("chat-assistant-prompt", 
                "Prompt for interactive chat assistant", chatPrompt);
            System.out.println("Registered prompt: chat-assistant-prompt");
        } catch (Exception e) {
            System.out.println("Prompt may already exist: " + e.getMessage());
        }
    }
}
