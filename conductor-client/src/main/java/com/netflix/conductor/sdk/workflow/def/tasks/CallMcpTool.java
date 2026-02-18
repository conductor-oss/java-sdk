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

import java.util.Map;

import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;

/**
 * MCP Call Tool task - executes a specific tool on an MCP (Model Context Protocol) server.
 * <p>
 * This system task calls a named tool on an MCP server with provided arguments
 * and returns the tool's result.
 *
 * <p>Example usage:
 * <pre>
 * // Direct call with known tool and arguments
 * CallMcpTool callTool = new CallMcpTool("get_weather", "get_weather_ref")
 *     .mcpServer("http://localhost:3001/mcp")
 *     .method("get_current_weather")
 *     .arguments(Map.of("city", "${workflow.input.city}"));
 *
 * // Dynamic call driven by LLM planning
 * CallMcpTool executeTool = new CallMcpTool("execute_tool", "execute_tool_ref")
 *     .mcpServer("http://localhost:3001/mcp")
 *     .method("${plan_action_ref.output.result.method}")
 *     .arguments("${plan_action_ref.output.result.arguments}");
 * </pre>
 *
 * @see ListMcpTools
 */
public class CallMcpTool extends Task<CallMcpTool> {

    /** Task type identifier for MCP call tool tasks */
    public static final String TASK_TYPE_CALL_MCP_TOOL = "MCP_CALL_TOOL";

    private String mcpServer;
    private Object method;
    private Object arguments;

    /**
     * Creates a new MCP Call Tool task.
     *
     * @param taskDefName the task definition name
     * @param taskReferenceName the unique reference name for this task in the workflow
     */
    public CallMcpTool(String taskDefName, String taskReferenceName) {
        super(taskReferenceName, TaskType.MCP_CALL_TOOL);
        super.name(taskDefName);
    }

    CallMcpTool(WorkflowTask workflowTask) {
        super(workflowTask);
    }

    /**
     * Sets the MCP server URL.
     *
     * @param mcpServer the URL of the MCP server (e.g., "http://localhost:3001/mcp")
     * @return this task for method chaining
     */
    public CallMcpTool mcpServer(String mcpServer) {
        this.mcpServer = mcpServer;
        return this;
    }

    /**
     * Sets the tool/method name to call.
     * <p>
     * Can be a literal string or a workflow expression (e.g., "${plan_ref.output.result.method}").
     *
     * @param method the tool name to invoke
     * @return this task for method chaining
     */
    public CallMcpTool method(String method) {
        this.method = method;
        return this;
    }

    /**
     * Sets the arguments to pass to the tool as a Map.
     *
     * @param arguments key-value pairs of tool arguments
     * @return this task for method chaining
     */
    public CallMcpTool arguments(Map<String, Object> arguments) {
        this.arguments = arguments;
        return this;
    }

    /**
     * Sets the arguments as a workflow expression string (e.g., from LLM planning output).
     *
     * @param argumentsExpression workflow expression resolving to a map of arguments
     * @return this task for method chaining
     */
    public CallMcpTool arguments(String argumentsExpression) {
        this.arguments = argumentsExpression;
        return this;
    }

    @Override
    protected void updateWorkflowTask(WorkflowTask workflowTask) {
        workflowTask.setType(TASK_TYPE_CALL_MCP_TOOL);

        if (mcpServer != null) {
            workflowTask.getInputParameters().put("mcpServer", mcpServer);
        }
        if (method != null) {
            workflowTask.getInputParameters().put("method", method);
        }
        if (arguments != null) {
            workflowTask.getInputParameters().put("arguments", arguments);
        }
    }

    public String getMcpServer() { return mcpServer; }
    public Object getMethod() { return method; }
    public Object getArguments() { return arguments; }
}
