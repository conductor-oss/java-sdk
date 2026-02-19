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

import java.util.Map;

import com.netflix.conductor.common.metadata.tasks.TaskType;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.sdk.workflow.def.tasks.Task;

import lombok.Getter;

/**
 * MCP List Tools task - discovers available tools from an MCP (Model Context Protocol) server.
 * Corresponds to server-side task type {@code LIST_MCP_TOOLS} (model: {@code MCPListToolsRequest}).
 */
@Getter
public class ListMcpTools extends Task<ListMcpTools> {

    public static final String TASK_TYPE_LIST_MCP_TOOLS = "LIST_MCP_TOOLS";

    private String mcpServer;
    private Map<String, String> headers;

    public ListMcpTools(String taskDefName, String taskReferenceName) {
        super(taskReferenceName, TaskType.LIST_MCP_TOOLS);
        super.name(taskDefName);
    }

    ListMcpTools(WorkflowTask workflowTask) {
        super(workflowTask);
    }

    public ListMcpTools mcpServer(String mcpServer) { this.mcpServer = mcpServer; return this; }
    public ListMcpTools headers(Map<String, String> headers) { this.headers = headers; return this; }

    @Override
    protected void updateWorkflowTask(WorkflowTask workflowTask) {
        workflowTask.setType(TASK_TYPE_LIST_MCP_TOOLS);
        if (mcpServer != null) workflowTask.getInputParameters().put("mcpServer", mcpServer);
        if (headers != null)   workflowTask.getInputParameters().put("headers", headers);
    }

}
