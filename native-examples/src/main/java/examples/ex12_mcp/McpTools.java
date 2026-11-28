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
package examples.ex12_mcp;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.McpCall.McpTool;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;

import static org.conductoross.conductor.sdk.Conductor.mcp;

/**
 * 12 MCP: using the tools of an MCP server.
 *
 * <pre>
 *   mcp(ref).server(url).listTools()        LIST_MCP_TOOLS: the server's tools, each with its name,
 *                                           description and input schema
 *   mcp(ref).server(url).call(tool, args)   CALL_MCP_TOOL: call one; .text() is the text of the
 *                                           result, .content() all of it, .isError() whether the
 *                                           tool reported an error
 *   .header(name, value)                    sent with each request (auth, say)
 *   .retries(count, delay)                  retries when the call fails
 * </pre>
 *
 * The Conductor server makes the calls; your process only runs the workflow code. Example 14 lets a
 * model choose which tool to call.
 *
 * <pre>
 *   python3 native-examples/stubs/ai_stub.py 8765 &amp;
 *   CONDUCTOR_SERVER_URL=http://localhost:8080/api ./gradlew :native-examples:ex12_mcp
 * </pre>
 */
public class McpTools {

    public record Request(String mcpUrl, String city) {}

    public record Used(List<String> tools, String weather, String sum) {}

    @Workflow(name = "mcp_tools")
    public Used use(Request request) {
        List<McpTool> tools = mcp("list_tools").server(request.mcpUrl()).listTools();

        String weather = mcp("weather").server(request.mcpUrl())
                .call("get_weather", Map.of("city", request.city()))
                .text();
        String sum = mcp("add").server(request.mcpUrl())
                .call("add", Map.of("a", 12, "b", 30))
                .text();

        return new Used(tools.stream().map(McpTool::name).toList(), weather, sum);
    }

    public static void main(String[] args) {
        ConductorClient client = ConductorClient.fromEnvironment();
        String stub = System.getenv().getOrDefault("STUB_URL", "http://localhost:8765");
        try (Conductor conductor = Conductor.of(client)) {
            conductor.workflows(McpTools.class).start();

            WorkflowHandle<Used> run = conductor.start(McpTools::use, new Request(stub + "/mcp", "Lisbon"));
            System.out.println("execution: " + run.workflowId());
            System.out.println("result:    " + run.result(Duration.ofSeconds(30)));
        }
        client.shutdown();
    }
}
