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

import org.conductoross.conductor.sdk.ai.LlmChatComplete;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.sdk.workflow.def.ConductorWorkflow;
import com.netflix.conductor.sdk.workflow.def.tasks.DoWhile;
import com.netflix.conductor.sdk.workflow.def.tasks.SetVariable;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;

import io.orkes.conductor.client.http.OrkesPromptClient;
import io.orkes.conductor.sdk.examples.util.ClientUtil;

/**
 * Example demonstrating automated multi-turn Q&A between two LLMs.
 * <p>
 * This example creates a workflow where:
 * <ul>
 *   <li>An "asker" LLM generates questions on a topic</li>
 *   <li>An "answerer" LLM provides responses</li>
 *   <li>The conversation continues for a specified number of turns</li>
 * </ul>
 * 
 * <p>Prerequisites:
 * <ul>
 *   <li>Conductor server running with AI/LLM integration configured</li>
 *   <li>OpenAI (or other LLM provider) integration set up in Conductor</li>
 *   <li>Prompt templates registered for asker and answerer roles</li>
 * </ul>
 * 
 * <p>Environment variables:
 * <ul>
 *   <li>CONDUCTOR_SERVER_URL - Conductor server URL (default: http://localhost:8080/api)</li>
 *   <li>CONDUCTOR_AUTH_KEY - Authentication key (for Orkes Conductor)</li>
 *   <li>CONDUCTOR_AUTH_SECRET - Authentication secret (for Orkes Conductor)</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>
 * # Run with default topic
 * ./gradlew :examples:run -PmainClass=io.orkes.conductor.sdk.examples.agentic.LlmChatExample
 * 
 * # Run with custom topic
 * ./gradlew :examples:run -PmainClass=io.orkes.conductor.sdk.examples.agentic.LlmChatExample --args="quantum computing"
 * </pre>
 */
public class LlmChatExample {

    private static final String WORKFLOW_NAME = "llm_multi_turn_chat";
    private static final String LLM_PROVIDER = "openai";
    private static final String MODEL = "gpt-4o-mini";
    private static final int MAX_TURNS = 3;

    public static void main(String[] args) {
        String topic = args.length > 0 ? String.join(" ", args) : "artificial intelligence";
        
        System.out.println("Starting LLM Multi-Turn Chat Example");
        System.out.println("Topic: " + topic);
        System.out.println("Max turns: " + MAX_TURNS);
        System.out.println();

        // Initialize Conductor client
        ConductorClient client = ClientUtil.getClient();
        WorkflowExecutor executor = new WorkflowExecutor(client, 10);

        try {
            // Register prompts for the conversation
            registerPrompts(client);
            
            // Create and register the workflow
            ConductorWorkflow<Map<String, Object>> workflow = createChatWorkflow(executor);
            workflow.registerWorkflow(true, true);
            System.out.println("Workflow registered: " + WORKFLOW_NAME);

            // Execute the workflow
            Map<String, Object> input = new HashMap<>();
            input.put("topic", topic);
            input.put("maxTurns", MAX_TURNS);
            input.put("llmProvider", LLM_PROVIDER);
            input.put("model", MODEL);

            var workflowRun = workflow.execute(input);
            Workflow result = workflowRun.get(120, TimeUnit.SECONDS);
            
            System.out.println("\n=== Conversation Complete ===");
            System.out.println("Status: " + result.getStatus());
            
            // Print the conversation history from output
            Object historyObj = result.getOutput().get("conversationHistory");
            if (historyObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, String>> history = (List<Map<String, String>>) historyObj;
                System.out.println("\n=== Conversation History ===");
                for (Map<String, String> message : history) {
                    System.out.println("\n[" + message.get("role").toUpperCase() + "]");
                    System.out.println(message.get("content"));
                }
            }

        } catch (Exception e) {
            System.err.println("Error executing workflow: " + e.getMessage());
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Creates a multi-turn chat workflow using LLM tasks.
     * <p>
     * The workflow uses a DO_WHILE loop to continue the conversation
     * for the specified number of turns.
     */
    private static ConductorWorkflow<Map<String, Object>> createChatWorkflow(WorkflowExecutor executor) {
        ConductorWorkflow<Map<String, Object>> workflow = new ConductorWorkflow<>(executor);
        workflow.setName(WORKFLOW_NAME);
        workflow.setVersion(1);
        workflow.setOwnerEmail("examples@conductor-oss.org");
        workflow.setDescription("Multi-turn chat between two LLMs demonstrating agentic conversation patterns");

        // Initialize conversation state using workflow variables
        Map<String, Object> initialVariables = new HashMap<>();
        initialVariables.put("conversationHistory", new ArrayList<>());
        initialVariables.put("turnCount", 0);
        workflow.setVariables(initialVariables);

        // Initialize state task
        SetVariable initState = new SetVariable("init_state_ref");
        initState.input("topic", "${workflow.input.topic}");
        workflow.add(initState);

        // Create the asker LLM task - generates questions
        LlmChatComplete askerTask = new LlmChatComplete("asker_llm", "asker_ref")
            .llmProvider("${workflow.input.llmProvider}")
            .model("${workflow.input.model}")
            .instructions("asker-prompt")
            .promptVariables(Map.of(
                "topic", "${workflow.variables.topic}",
                "conversationHistory", "${workflow.variables.conversationHistory}"
            ))
            .temperature(0.8)
            .maxTokens(200);

        // Update state with asker's question
        SetVariable updateAfterAsker = new SetVariable("update_asker_ref");
        updateAfterAsker.input("lastQuestion", "${asker_ref.output.result}");

        // Create the answerer LLM task - provides responses
        LlmChatComplete answererTask = new LlmChatComplete("answerer_llm", "answerer_ref")
            .llmProvider("${workflow.input.llmProvider}")
            .model("${workflow.input.model}")
            .instructions("answerer-prompt")
            .promptVariables(Map.of(
                "question", "${workflow.variables.lastQuestion}",
                "conversationHistory", "${workflow.variables.conversationHistory}"
            ))
            .temperature(0.7)
            .maxTokens(500);

        // Update state with answerer's response and increment turn count
        SetVariable updateAfterAnswerer = new SetVariable("update_answerer_ref");
        updateAfterAnswerer.input("turnCount", "${workflow.variables.turnCount + 1}");

        // Create DO_WHILE loop for multi-turn conversation
        DoWhile conversationLoop = new DoWhile(
            "loop_ref",
            "if ( $.loop_ref['iteration'] < " + MAX_TURNS + ") { true; } else { false; }",
            askerTask, updateAfterAsker, answererTask, updateAfterAnswerer
        );
        
        workflow.add(conversationLoop);

        // Set final output
        Map<String, Object> outputParams = new HashMap<>();
        outputParams.put("conversationHistory", "${workflow.variables.conversationHistory}");
        outputParams.put("totalTurns", "${workflow.variables.turnCount}");
        outputParams.put("topic", "${workflow.input.topic}");
        workflow.setWorkflowOutput(outputParams);

        return workflow;
    }

    /**
     * Registers prompt templates required for the chat workflow.
     * <p>
     * In a production environment, these would typically be managed
     * separately and not registered during workflow execution.
     */
    private static void registerPrompts(ConductorClient client) {
        OrkesPromptClient promptClient = new OrkesPromptClient(client);

        // Asker prompt - generates questions on the topic
        String askerPrompt = "You are a curious learner having a conversation about {{topic}}.\n" +
            "Based on the conversation history so far, ask an insightful follow-up question.\n" +
            "If this is the start of the conversation, ask an introductory question about the topic.\n\n" +
            "Conversation history:\n{{conversationHistory}}\n\n" +
            "Ask your next question (be concise, 1-2 sentences):";
        
        try {
            promptClient.savePrompt("asker-prompt", "Prompt for generating questions in multi-turn chat", askerPrompt);
            System.out.println("Registered prompt: asker-prompt");
        } catch (Exception e) {
            System.out.println("Prompt 'asker-prompt' may already exist: " + e.getMessage());
        }

        // Answerer prompt - provides knowledgeable responses
        String answererPrompt = "You are a knowledgeable expert having a conversation.\n" +
            "Answer the following question thoughtfully and informatively.\n" +
            "Keep your response focused and under 3 paragraphs.\n\n" +
            "Conversation history:\n{{conversationHistory}}\n\n" +
            "Question: {{question}}\n\n" +
            "Your response:";
        
        try {
            promptClient.savePrompt("answerer-prompt", "Prompt for generating answers in multi-turn chat", answererPrompt);
            System.out.println("Registered prompt: answerer-prompt");
        } catch (Exception e) {
            System.out.println("Prompt 'answerer-prompt' may already exist: " + e.getMessage());
        }
    }
}
