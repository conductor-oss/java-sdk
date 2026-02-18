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
 * MCP List Tools task - discovers available tools from an MCP (Model Context Protocol) server.
 * <p>
 * This system task connects to an MCP server and returns the list of tools available,
 * which can then be used by an LLM to select which tool to call.
 *
 * <p>Example usage:
 * <pre>
 * ListMcpTools listTools = new ListMcpTools("discover_tools", "discover_tools_ref")
 *     .mcpServer("http://localhost:3001/mcp");
 *
 * workflow.add(listTools);
 * // Output: ${discover_tools_ref.output.tools} - list of available tools
 * </pre>
 *
 * @see CallMcpTool
 */
public class ListMcpTools extends Task<ListMcpTools> {

    /** Task type identifier for MCP list tools tasks */
    public static final String TASK_TYPE_LIST_MCP_TOOLS = "MCP_LIST_TOOLS";

    private String mcpServer;

    /**
     * Creates a new MCP List Tools task.
     *
     * @param taskDefName the task definition name
     * @param taskReferenceName the unique reference name for this task in the workflow
     */
    public ListMcpTools(String taskDefName, String taskReferenceName) {
        super(taskReferenceName, TaskType.MCP_LIST_TOOLS);
        super.name(taskDefName);
    }

    ListMcpTools(WorkflowTask workflowTask) {
        super(workflowTask);
    }

    /**
     * Sets the MCP server URL.
     *
     * @param mcpServer the URL of the MCP server (e.g., "http://localhost:3001/mcp")
     * @return this task for method chaining
     */
    public ListMcpTools mcpServer(String mcpServer) {
        this.mcpServer = mcpServer;
        return this;
    }

    @Override
    protected void updateWorkflowTask(WorkflowTask workflowTask) {
        workflowTask.setType(TASK_TYPE_LIST_MCP_TOOLS);

        if (mcpServer != null) {
            workflowTask.getInputParameters().put("mcpServer", mcpServer);
        }
    }

    public String getMcpServer() { return mcpServer; }
}
