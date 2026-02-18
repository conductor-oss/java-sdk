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

import java.util.List;

import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;

/**
 * LLM Chat Completion task for multi-turn conversations with large language models.
 * <p>
 * This task sends a conversation history (list of messages) to an LLM provider and returns 
 * the assistant's response. Supports various LLM providers configured through Conductor's 
 * integration framework.
 * 
 * <p>Example usage:
 * <pre>
 * LlmChatComplete chatTask = new LlmChatComplete("chat_assistant", "chat_ref")
 *     .llmProvider("openai")
 *     .model("gpt-4")
 *     .messages("${workflow.input.conversation_history}")
 *     .temperature(0.7)
 *     .maxTokens(1000);
 * 
 * workflow.add(chatTask);
 * </pre>
 * 
 * <p>Message format expected:
 * <pre>
 * [
 *   {"role": "system", "content": "You are a helpful assistant."},
 *   {"role": "user", "content": "Hello!"},
 *   {"role": "assistant", "content": "Hi there! How can I help you?"},
 *   {"role": "user", "content": "What's the weather like?"}
 * ]
 * </pre>
 */
public class LlmChatComplete extends Task<LlmChatComplete> {

    /** Task type identifier for LLM chat completion tasks */
    public static final String TASK_TYPE_LLM_CHAT_COMPLETE = "LLM_CHAT_COMPLETE";

    private String llmProvider;
    private String model;
    private Object messages;
    private String promptName;
    private Object promptVariables;
    private Double temperature;
    private Double topP;
    private Integer maxTokens;
    private List<String> stopWords;
    private List<Object> tools;
    private String toolChoice;
    private Boolean jsonOutput;

    /**
     * Creates a new LLM Chat Completion task.
     *
     * @param taskDefName the task definition name
     * @param taskReferenceName the unique reference name for this task in the workflow
     */
    public LlmChatComplete(String taskDefName, String taskReferenceName) {
        super(taskReferenceName, TaskType.LLM_CHAT_COMPLETE);
        super.name(taskDefName);
    }

    LlmChatComplete(WorkflowTask workflowTask) {
        super(workflowTask);
    }

    /**
     * Sets the LLM provider integration name.
     *
     * @param llmProvider the integration name (e.g., "openai", "azure_openai", "anthropic")
     * @return this task for method chaining
     */
    public LlmChatComplete llmProvider(String llmProvider) {
        this.llmProvider = llmProvider;
        return this;
    }

    /**
     * Sets the model to use for chat completion.
     *
     * @param model the model identifier (e.g., "gpt-4", "gpt-3.5-turbo", "claude-3-opus")
     * @return this task for method chaining
     */
    public LlmChatComplete model(String model) {
        this.model = model;
        return this;
    }

    /**
     * Sets the conversation messages.
     * <p>
     * Can be a list of message objects or a workflow expression that resolves to a list.
     * Each message should have "role" (system/user/assistant) and "content" fields.
     *
     * @param messages the conversation history
     * @return this task for method chaining
     */
    public LlmChatComplete messages(Object messages) {
        this.messages = messages;
        return this;
    }

    /**
     * Sets the prompt template name to use.
     * <p>
     * When set, the prompt template content will be used as the system message.
     *
     * @param promptName the name of the registered prompt template
     * @return this task for method chaining
     */
    public LlmChatComplete promptName(String promptName) {
        this.promptName = promptName;
        return this;
    }

    /**
     * Sets variables to substitute in the prompt template.
     *
     * @param promptVariables a map of variable names to values
     * @return this task for method chaining
     */
    public LlmChatComplete promptVariables(Object promptVariables) {
        this.promptVariables = promptVariables;
        return this;
    }

    /**
     * Sets the temperature for controlling randomness.
     *
     * @param temperature value between 0.0 and 2.0
     * @return this task for method chaining
     */
    public LlmChatComplete temperature(Double temperature) {
        this.temperature = temperature;
        return this;
    }

    /**
     * Sets the top-p (nucleus sampling) parameter.
     *
     * @param topP value between 0.0 and 1.0
     * @return this task for method chaining
     */
    public LlmChatComplete topP(Double topP) {
        this.topP = topP;
        return this;
    }

    /**
     * Sets the maximum number of tokens to generate.
     *
     * @param maxTokens maximum tokens in the response
     * @return this task for method chaining
     */
    public LlmChatComplete maxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
        return this;
    }

    /**
     * Sets stop words/sequences where the model should stop generating.
     *
     * @param stopWords list of stop sequences
     * @return this task for method chaining
     */
    public LlmChatComplete stopWords(List<String> stopWords) {
        this.stopWords = stopWords;
        return this;
    }

    /**
     * Sets the available tools/functions for the model to call.
     * <p>
     * Used for function calling capabilities where the LLM can decide
     * which tool to invoke based on the conversation.
     *
     * @param tools list of tool definitions
     * @return this task for method chaining
     */
    public LlmChatComplete tools(List<Object> tools) {
        this.tools = tools;
        return this;
    }

    /**
     * Sets the tool choice behavior.
     * <p>
     * Options: "auto" (default), "none", or a specific tool name.
     *
     * @param toolChoice the tool choice setting
     * @return this task for method chaining
     */
    public LlmChatComplete toolChoice(String toolChoice) {
        this.toolChoice = toolChoice;
        return this;
    }

    /**
     * Enables structured JSON output mode.
     * <p>
     * When true, instructs the LLM to respond in JSON format, which is useful for
     * function calling patterns and structured data extraction.
     *
     * @param jsonOutput true to enable JSON output mode
     * @return this task for method chaining
     */
    public LlmChatComplete jsonOutput(Boolean jsonOutput) {
        this.jsonOutput = jsonOutput;
        return this;
    }

    @Override
    protected void updateWorkflowTask(WorkflowTask workflowTask) {
        workflowTask.setType(TASK_TYPE_LLM_CHAT_COMPLETE);
        
        if (llmProvider != null) {
            workflowTask.getInputParameters().put("llmProvider", llmProvider);
        }
        if (model != null) {
            workflowTask.getInputParameters().put("model", model);
        }
        if (messages != null) {
            workflowTask.getInputParameters().put("messages", messages);
        }
        if (promptName != null) {
            workflowTask.getInputParameters().put("promptName", promptName);
        }
        if (promptVariables != null) {
            workflowTask.getInputParameters().put("promptVariables", promptVariables);
        }
        if (temperature != null) {
            workflowTask.getInputParameters().put("temperature", temperature);
        }
        if (topP != null) {
            workflowTask.getInputParameters().put("topP", topP);
        }
        if (maxTokens != null) {
            workflowTask.getInputParameters().put("maxTokens", maxTokens);
        }
        if (stopWords != null) {
            workflowTask.getInputParameters().put("stopWords", stopWords);
        }
        if (tools != null) {
            workflowTask.getInputParameters().put("tools", tools);
        }
        if (toolChoice != null) {
            workflowTask.getInputParameters().put("toolChoice", toolChoice);
        }
        if (jsonOutput != null) {
            workflowTask.getInputParameters().put("jsonOutput", jsonOutput);
        }
    }

    // Getters
    public String getLlmProvider() { return llmProvider; }
    public String getModel() { return model; }
    public Object getMessages() { return messages; }
    public String getPromptName() { return promptName; }
    public Object getPromptVariables() { return promptVariables; }
    public Double getTemperature() { return temperature; }
    public Double getTopP() { return topP; }
    public Integer getMaxTokens() { return maxTokens; }
    public List<String> getStopWords() { return stopWords; }
    public List<Object> getTools() { return tools; }
    public String getToolChoice() { return toolChoice; }
    public Boolean getJsonOutput() { return jsonOutput; }
}
