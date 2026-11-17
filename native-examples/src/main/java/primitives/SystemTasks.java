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
package primitives;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.conductoross.conductor.sdk.AgentCall;
import org.conductoross.conductor.sdk.McpCall;
import org.conductoross.conductor.sdk.TaskFailure;
import org.conductoross.conductor.sdk.Workflow;

import com.fasterxml.jackson.databind.JsonNode;

import static org.conductoross.conductor.sdk.Conductor.agent;
import static org.conductoross.conductor.sdk.Conductor.awaitExternal;
import static org.conductoross.conductor.sdk.Conductor.event;
import static org.conductoross.conductor.sdk.Conductor.http;
import static org.conductoross.conductor.sdk.Conductor.human;
import static org.conductoross.conductor.sdk.Conductor.llm;
import static org.conductoross.conductor.sdk.Conductor.mcp;

/**
 * Work the server does itself, with no worker of yours: each call is one of Conductor's system
 * tasks, named by the reference you give it.
 *
 * <pre>
 *   http(ref).get(url).asJson()                 HTTP: GET, POST, PUT, DELETE, headers, body,
 *                                               retries; the response as JSON or a type
 *   event(ref).to(sink).send(payload)           EVENT: publish to conductor, sqs:..., kafka:...
 *   human(ref).assignee(..).await(type, t)      HUMAN: a person answers through the UI or the API;
 *                                               empty when nobody does in time
 *   awaitExternal(ref, type).await(t)           WAIT for any system to complete it:
 *                                               POST /api/tasks/{workflowId}/{ref}/COMPLETED
 *   llm(ref).model(..).prompt(..).as(type)      LLM_CHAT_COMPLETE, through the server's providers
 *   mcp(ref).server(url).listTools() / call()   LIST_MCP_TOOLS / CALL_MCP_TOOL
 *   agent(ref).remote(cardUrl).ask(text)        an A2A agent; or conductor(name, version)
 * </pre>
 *
 * A system task that fails for good throws TaskFailure at the call, as a task does.
 */
public class SystemTasks {

    public record Ai(String url) {}

    public record Answer(String by, boolean ok) {}

    public record Delivery(String status) {}

    @Workflow(name = "p_system_tasks")
    public List<String> run(Ai ai) {
        List<String> out = new ArrayList<>();

        JsonNode card = http("agent_card").get(ai.url() + "/.well-known/agent-card.json").asJson();
        out.add("http: " + card.path("name").asText());

        event("announce").to("conductor").send(Map.of("hello", "world"));

        Optional<Answer> answer = human("review").assignee("ops").input(Map.of("what", "approve"))
                .await(Answer.class, Duration.ofHours(1));
        out.add("human: " + answer.map(Answer::by).orElse("nobody"));

        Optional<Delivery> delivery = awaitExternal("delivered", Delivery.class).await(Duration.ofHours(1));
        out.add("external: " + delivery.map(Delivery::status).orElse("none"));

        try {
            out.add("llm: " + llm("summary").model("openai/gpt-4o-mini").prompt("Say hi").retries(0, Duration.ofSeconds(1))
                    .as(String.class));
        } catch (TaskFailure e) {
            out.add("llm: unavailable");          // no model provider configured on this server
        }

        List<McpCall.McpTool> tools = mcp("tools").server(ai.url() + "/mcp").listTools();
        out.add("mcp tools: " + tools.size());
        out.add("mcp call: " + mcp("weather").server(ai.url() + "/mcp").call("get_weather", Map.of("city", "Paris")).text());

        AgentCall.AgentResult reply = agent("helper").remote(ai.url() + "/.well-known/agent-card.json").ask("Check order 7");
        out.add("agent: " + reply.text());
        return out;
    }
}
