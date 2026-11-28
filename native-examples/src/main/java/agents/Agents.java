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
package agents;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;

import org.conductoross.conductor.sdk.AgentCall;
import org.conductoross.conductor.sdk.AgentCall.AgentResult;
import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.TaskFailure;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;
import org.conductoross.conductor.sdk.testing.DurableReplay;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.conductoross.conductor.sdk.Conductor.agent;
import static org.conductoross.conductor.sdk.Conductor.async;
import static org.conductoross.conductor.sdk.Conductor.human;
import static org.conductoross.conductor.sdk.Conductor.join;

/**
 * AI agents from workflow code. {@code agent(ref)} starts a conversation with an agent, and each
 * {@code ask(text)} on it is an AGENT task: the server sends the message, waits for the agent and
 * records the reply. Asking again continues the conversation, and answers the agent's question when
 * it asked one. The workflow is suspended while it waits, holding no thread.
 *
 * <p>An agent may work for hours, and the workflow waits as long as it takes: the server polls the
 * agent's task or waits for the agent to call back. As with any call, to keep working meanwhile,
 * ask inside {@code async(...)} and join the Future when the answer is needed, as this workflow
 * does with its two long-running agents.
 *
 * <pre>
 *   agent(ref).remote(cardUrl)                a conversation with an A2A agent, its endpoint read
 *   agent(ref).remote(endpoint)               from its agent card, or given directly
 *   .ask(text)                                a message and its reply; asking again on the same
 *                                             conversation continues it, and when the reply
 *                                             needsInput(), the next ask answers the agent
 *   .header(name, value)                      sent to the agent with each request (auth, say)
 *   .maxDuration(d)                           how long an agent task may run; 24 hours by default
 *   .pollInterval(d)                          how often the server polls a running agent task (5s)
 *   .pushNotifications(backstop)              the agent calls the server back when done instead;
 *                                             the server polls only every backstop, in case a call
 *                                             is lost. Needs conductor.a2a.callback.url on the server
 *   async(() -> agent(..).ask(..))            an agent that works while the workflow goes on; join
 *                                             its Future when its answer is needed
 *   .retries(n, delay)                        retries when the agent fails; 3 by default, and at
 *                                             least 1 (the server makes a count below 1 into 3)
 *   catch (TaskFailure e)                     an agent that failed for good throws at its call
 *   agent(ref).conductor(name, version)       a Conductor agent by name, run by the server's agent
 *                                             runtime, instead of a remote one (not run here)
 * </pre>
 *
 * The reply ({@link AgentResult}) has the text, the state ({@code completed}, {@code input-required},
 * or {@code message} for a direct answer), the artifacts with any structured data, and the ids that
 * continue the conversation.
 *
 * <p>Runs against the stub agent in {@code native-examples/stubs/ai_stub.py}, which answers by
 * markers in the text: {@code [slow N]} (works for N seconds), {@code [ask]}, {@code [fail]},
 * {@code [data]}. Push notifications need the server started with {@code
 * --conductor.a2a.callback.url=http://localhost:8099}.
 *
 * <pre>
 *   python3 native-examples/stubs/ai_stub.py 8765 &amp;
 *   CONDUCTOR_SERVER_URL=http://localhost:8099/api CONDUCTOR_DEV=true \
 *     java -cp "native-examples/build/install/native-examples/lib/*" agents.Agents [worker]
 * </pre>
 */
public class Agents {

    public record Ticket(String id, String agentUrl) {}

    public record Choice(String refundTo) {}

    @Workflow(name = "support_agents")
    public List<String> run(Ticket ticket) {
        String card = ticket.agentUrl() + "/.well-known/agent-card.json";
        List<String> out = new ArrayList<>();

        // two long-running agents, started first: the workflow goes on while they work, and the
        // server waits for them without a worker, one by polling, the other by a callback
        Future<AgentResult> research = async(() -> agent("research").remote(card)
                .pollInterval(Duration.ofSeconds(2)).maxDuration(Duration.ofHours(4))
                .ask("[slow 8] Find tickets like " + ticket.id()));
        // (a callback needs the agent's endpoint, not its card: the server reads the agent's task
        // from the URL given here when the callback comes)
        Future<AgentResult> audit = async(() -> agent("audit").remote(ticket.agentUrl() + "/a2a")
                .pushNotifications(Duration.ofMinutes(5)).maxDuration(Duration.ofHours(8))
                .ask("[slow 10] Audit the account behind " + ticket.id()));

        // a conversation: one message, one reply; asking again is its next turn
        AgentCall support = agent("support").remote(card);
        out.add(support.ask("Summarize ticket " + ticket.id()).text());
        out.add(support.ask("What first?").text());

        // the endpoint directly, with a header
        out.add(agent("whoami").remote(ticket.agentUrl() + "/a2a").header("Authorization", "Bearer support-bot")
                .ask("Who is asking?").text());

        // structured output: a data part in the agent's artifact
        AgentResult triage = agent("triage").remote(card).ask("[data] Triage " + ticket.id());
        out.add("priority " + data(triage).get("priority") + ", team " + data(triage).get("team"));

        // the agent asks for input: a person answers, and asking again gives the agent the answer
        AgentCall refund = agent("refund").remote(card);
        AgentResult reply = refund.ask("[ask] Refund ticket " + ticket.id());
        if (reply.needsInput()) {
            Optional<Choice> choice = human("refund_choice").assignee("support").input(Map.of("question", reply.text()))
                    .await(Choice.class, Duration.ofHours(1));
            reply = refund.ask(choice.map(Choice::refundTo).orElse("store credit"));
        }
        out.add(reply.state() + ": " + reply.text());

        // an agent that fails: retried once, then the failure is caught
        try {
            agent("charge").remote(card).retries(1, Duration.ofSeconds(1)).ask("[fail] Charge the card");
        } catch (TaskFailure e) {
            out.add("charge " + e.getStatus());
        }

        // the long-running agents' answers, now that they are needed
        join(research, audit);
        out.add(research.resultNow().state() + ": " + research.resultNow().text());
        out.add(audit.resultNow().state() + ": " + audit.resultNow().text());
        return out;
    }

    /** The first data part of the agent's artifacts. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> data(AgentResult reply) {
        for (Map<String, Object> artifact : reply.artifacts()) {
            for (Map<String, Object> part : (List<Map<String, Object>>) artifact.get("parts")) {
                if ("data".equals(part.get("kind"))) {
                    return (Map<String, Object>) part.get("data");
                }
            }
        }
        return Map.of();
    }

    static List<String> expected(String id) {
        return List.of(
                "Agent reply: Summarize ticket " + id + " has been reviewed.",
                "Agent reply: What first? (turn 2 of this conversation).",
                "Agent reply: Who is asking? has been reviewed. Authorized as support-bot.",
                "priority high, team billing",
                "completed: Done, as you said: card",
                "charge FAILED",
                "completed: Done: Find tickets like " + id,
                "completed: Done: Audit the account behind " + id);
    }

    /** {@code worker}: host the code only, for runs started on the server. */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        ConductorClient client = ConductorClient.fromEnvironment();
        Conductor conductor = Conductor.of(client);
        conductor.workflows(Agents.class).start();
        if (args.length > 0 && args[0].equals("worker")) {
            System.out.println("support_agents worker started");
            Thread.currentThread().join();
        }
        String agentUrl = System.getenv().getOrDefault("AGENT_URL", "http://localhost:8765");
        String id = "T-" + System.currentTimeMillis() % 100000;
        WorkflowHandle<List<String>> run = conductor.start(Agents::run, new Ticket(id, agentUrl));
        System.out.println("execution: " + run.workflowId());
        List<String> result;
        try {
            run.completeTask("refund_choice", new Choice("card"), Duration.ofMinutes(2));
            result = run.result(Duration.ofMinutes(3));
        } catch (RuntimeException e) {
            System.out.println("AGENTS FAILED: " + e.getMessage());
            System.exit(1);
            return;
        }
        result.forEach(line -> System.out.println("  " + line));

        Map<String, Object> wf = client.execute(ConductorClientRequest.builder().method(ConductorClientRequest.Method.GET)
                .path("/workflow/" + run.workflowId()).addQueryParam("includeTasks", true).build(),
                new TypeReference<Map<String, Object>>() {}).getData();
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) wf.get("tasks");
        List<String> failed = new ArrayList<>();
        if (!expected(id).equals(result)) {
            failed.add("result: expected " + expected(id));
        }
        long agentCalls = tasks.stream().filter(t -> "AGENT".equals(t.get("taskType"))).map(t -> t.get("referenceTaskName"))
                .distinct().count();
        if (agentCalls != 9) {
            failed.add("9 AGENT calls, got " + agentCalls);
        }
        Map<String, Object> research = task(tasks, "research");
        if (((Number) research.get("pollCount")).intValue() < 3) {
            failed.add("the server polled the long-running agent: pollCount " + research.get("pollCount"));
        }
        Map<String, Object> audit = task(tasks, "audit");
        if (!"a2a-callback".equals(audit.get("workerId"))
                || ((Map<String, Object>) audit.get("outputData")).get("pushToken") == null) {
            failed.add("the agent's callback completed the audit: workerId " + audit.get("workerId"));
        }
        if (endTime(task(tasks, "refund_2")) >= endTime(research)) {
            failed.add("the workflow went on while the long-running agents worked");
        }
        Object asked = ((Map<String, Object>) task(tasks, "refund").get("outputData")).get("taskId");
        Object answered = ((Map<String, Object>) task(tasks, "refund_2").get("inputData")).get("taskId");
        if (asked == null || !asked.equals(answered)) {
            failed.add("the answer continues the agent task that asked: " + asked + " / " + answered);
        }
        long charges = tasks.stream().filter(t -> "charge".equals(t.get("referenceTaskName"))).count();
        if (charges != 2) {
            failed.add("the failing agent was tried twice (one retry), got " + charges);
        }
        Path file = Files.createTempDirectory("agents").resolve(run.workflowId() + ".json");
        Files.writeString(file, new ObjectMapper().writeValueAsString(wf));
        try {
            DurableReplay.assertCompatible(Agents.class, file);
        } catch (Throwable e) {
            failed.add("replay: " + e.getMessage());
        }

        conductor.close();
        client.shutdown();
        System.out.println(failed.isEmpty() ? "AGENTS PASSED" : "AGENTS FAILED: " + failed);
        System.exit(failed.isEmpty() ? 0 : 1);
    }

    private static long endTime(Map<String, Object> task) {
        return ((Number) task.get("endTime")).longValue();
    }

    private static Map<String, Object> task(List<Map<String, Object>> tasks, String ref) {
        return tasks.stream().filter(t -> ref.equals(t.get("referenceTaskName"))).reduce((a, b) -> b).orElseThrow();
    }
}
