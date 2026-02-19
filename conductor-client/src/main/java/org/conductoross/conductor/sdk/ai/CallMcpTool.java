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


/**
 * MCP Call Tool task - executes a specific tool on an MCP (Model Context Protocol) server.
 * Corresponds to server-side task type {@code CALL_MCP_TOOL} (model: {@code MCPToolCallRequest}).
 */
public class CallMcpTool extends Task<CallMcpTool> {

    public static final String TASK_TYPE_CALL_MCP_TOOL = "CALL_MCP_TOOL";

    private String mcpServer;
    private String method;
    private Map<String, Object> arguments;
    private Map<String, String> headers;

    public CallMcpTool(String taskDefName, String taskReferenceName) {
        super(taskReferenceName, TaskType.CALL_MCP_TOOL);
        super.name(taskDefName);
    }

    CallMcpTool(WorkflowTask workflowTask) {
        super(workflowTask);
    }

    public CallMcpTool mcpServer(String mcpServer) { this.mcpServer = mcpServer; return this; }
    public CallMcpTool method(String method) { this.method = method; return this; }
    public CallMcpTool arguments(Map<String, Object> arguments) { this.arguments = arguments; return this; }
    public CallMcpTool headers(Map<String, String> headers) { this.headers = headers; return this; }

    @Override
    protected void updateWorkflowTask(WorkflowTask workflowTask) {
        workflowTask.setType(TASK_TYPE_CALL_MCP_TOOL);
        if (mcpServer != null)  workflowTask.getInputParameters().put("mcpServer", mcpServer);
        if (method != null)     workflowTask.getInputParameters().put("method", method);
        if (arguments != null)  workflowTask.getInputParameters().put("arguments", arguments);
        if (headers != null)    workflowTask.getInputParameters().put("headers", headers);
    }

    public String getMcpServer() { return mcpServer; }
    public String getMethod() { return method; }
    public Map<String, Object> getArguments() { return arguments; }
    public Map<String, String> getHeaders() { return headers; }
}
