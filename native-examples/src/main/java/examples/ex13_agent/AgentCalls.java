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
package examples.ex13_agent;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Future;

import org.conductoross.conductor.sdk.AgentCall;
import org.conductoross.conductor.sdk.AgentCall.AgentResult;
import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;

import static org.conductoross.conductor.sdk.Conductor.agent;
import static org.conductoross.conductor.sdk.Conductor.async;
import static org.conductoross.conductor.sdk.Conductor.join;

/**
 * 13 Agent: asking an AI agent, over A2A.
 *
 * <p>{@code agent(ref)} is a conversation with an agent, and each {@code ask(text)} on it is an
 * AGENT task: the server sends the message, waits for the agent and records the reply. Asking again
 * on the same conversation continues it.
 *
 * <p>Agents can work for minutes or hours. The workflow waits on the server, holding no thread: the
 * server polls the agent's task, or the agent calls the server back. To keep working while an agent
 * works, ask inside {@code async(...)} and join when the answer is needed.
 *
 * <pre>
 *   agent(ref).remote(url)              an A2A agent, by its agent card's URL or its endpoint
 *   .ask(text)                          a message and its reply: .text(), .state(), .artifacts()
 *   .pollInterval(d)                    how often the server checks on a running agent task
 *   .maxDuration(d)                     how long the agent may take; 24 hours by default
 *   .pushNotifications(backstop)        the agent calls the server back when done instead of
 *                                       being polled (needs conductor.a2a.callback.url set)
 *   .header(name, value)                sent with each request (auth, say)
 *   reply.needsInput()                  the agent asked a question: the next ask answers it
 * </pre>
 *
 * <p>Runs against the stub agent in {@code native-examples/stubs/ai_stub.py}; {@code [slow 5]} in a
 * message makes it work for five seconds.
 *
 * <pre>
 *   python3 native-examples/stubs/ai_stub.py 8765 &amp;
 *   CONDUCTOR_SERVER_URL=http://localhost:8080/api ./gradlew :native-examples:ex13_agent
 * </pre>
 */
public class AgentCalls {

    public record Request(String agentUrl, String question) {}

    @Workflow(name = "ask_an_agent")
    public List<String> ask(Request request) {
        // a long piece of work, started first: the workflow goes on while the agent works
        Future<AgentResult> research = async(() -> agent("research").remote(request.agentUrl())
                .pollInterval(Duration.ofSeconds(2))
                .maxDuration(Duration.ofHours(4))
                .ask("[slow 5] Find orders like this one: " + request.question()));

        // a conversation: one ask, one reply; asking again is the next turn
        AgentCall support = agent("support").remote(request.agentUrl());
        String answer = support.ask(request.question()).text();
        String followUp = support.ask("What should I do first?").text();

        // the long piece of work, now that it is needed
        join(research);
        return List.of(answer, followUp, research.resultNow().text());
    }

    public static void main(String[] args) {
        ConductorClient client = ConductorClient.fromEnvironment();
        String stub = System.getenv().getOrDefault("STUB_URL", "http://localhost:8765");
        try (Conductor conductor = Conductor.of(client)) {
            conductor.workflows(AgentCalls.class).start();

            Request request = new Request(stub + "/.well-known/agent-card.json", "Why was order 1042 charged twice?");
            WorkflowHandle<List<String>> run = conductor.start(AgentCalls::ask, request);
            System.out.println("execution: " + run.workflowId());
            run.result(Duration.ofMinutes(2)).forEach(line -> System.out.println("  " + line));
        }
        client.shutdown();
    }
}
