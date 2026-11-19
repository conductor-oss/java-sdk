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
package examples.ex14_agent_harness;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.conductoross.conductor.sdk.AgentCall.AgentResult;
import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.McpCall.McpTool;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;

import static org.conductoross.conductor.sdk.Conductor.agent;
import static org.conductoross.conductor.sdk.Conductor.async;
import static org.conductoross.conductor.sdk.Conductor.human;
import static org.conductoross.conductor.sdk.Conductor.join;
import static org.conductoross.conductor.sdk.Conductor.llm;
import static org.conductoross.conductor.sdk.Conductor.mcp;

/**
 * 14 Agent harness: a durable agent loop, with tools, helper agents and a person.
 *
 * <p>A model plans one step at a time; the workflow carries the step out and gives the model what it
 * observed, until the model gives its answer or the budget of steps runs out:
 *
 * <pre>
 *   list_tools   LIST_MCP_TOOLS      the MCP server's tools, listed once and given to the model
 *   plan         LLM_CHAT_COMPLETE   the model reads the goal and the observations so far and answers
 *                                    the next step as JSON, read into a Step record
 *   call_tool    CALL_MCP_TOOL       "tool": call the tool the model chose, with its arguments
 *   helper       AGENT               "delegate": hand longer work to another agent. It runs in a
 *                                    branch of its own while the loop goes on, for hours if need be
 *   ask_user     HUMAN               "ask_human": ask a person, before spending money say
 *                                    "finish": the answer. If helpers are still working, the loop
 *                                    waits for them and gives the model their findings first
 * </pre>
 *
 * <p>The loop is a plain Java {@code while}, and the choice of step a plain {@code switch}. Each
 * step is a task on the server: the agent survives restarts of the server and of this process, a
 * helper agent or a person may take hours while nothing is held, and the execution in the UI shows
 * every step the agent took, with the model's input and output.
 *
 * <p>Runs against the stub in {@code native-examples/stubs/ai_stub.py}: a stand-in planner model
 * that follows a script, an MCP server and an A2A agent. With a real provider, set {@link #PLANNER}
 * to one of its models.
 *
 * <pre>
 *   python3 native-examples/stubs/ai_stub.py 8765 &amp;
 *   # the server started with --conductor.ai.litellm.baseURL=http://localhost:8765
 *   CONDUCTOR_SERVER_URL=http://localhost:8080/api ./gradlew :native-examples:ex14_agent_harness
 * </pre>
 *
 * This main answers the person's question itself.
 */
public class AgentHarness {

    static final String PLANNER = "litellm/planner";

    static final String INSTRUCTIONS = """
            You reach the user's goal one step at a time. Answer only with JSON, one of:
              {"action": "tool", "tool": "<name>", "args": {...}}   call one of the tools below
              {"action": "delegate", "task": "<what to find out>"}  hand longer research to a helper agent
              {"action": "ask_human", "question": "<question>"}     ask the user, before spending money say
              {"action": "finish", "answer": "<the answer>"}        when the goal is reached
            The tools:
            """;

    public record Goal(String text, String mcpUrl, String agentUrl, int maxSteps) {}

    /** The model's next step; only the fields of its action are set. */
    public record Step(String action, String tool, Map<String, Object> args, String task, String question,
                       String answer) {}

    /** What the person answers. */
    public record Approval(boolean approved, String note) {}

    public record Outcome(String answer, int steps, List<String> observations) {}

    @Workflow(name = "agent_harness")
    public Outcome pursue(Goal goal) {
        List<McpTool> tools = mcp("list_tools").server(goal.mcpUrl()).listTools();
        String instructions = INSTRUCTIONS + describe(tools);

        List<String> observations = new ArrayList<>();
        List<Future<AgentResult>> helpers = new ArrayList<>();
        String answer = null;
        int steps = 0;
        while (answer == null && steps < goal.maxSteps()) {
            steps++;
            Step step = llm("plan").model(PLANNER)
                    .system(instructions)
                    .prompt(goal.text() + "\nObservations:\n" + String.join("\n", observations))
                    .as(Step.class);

            switch (step.action()) {
                case "tool" -> {
                    String result = mcp("call_tool").server(goal.mcpUrl()).call(step.tool(), step.args()).text();
                    observations.add("tool " + step.tool() + ": " + result);
                }
                case "delegate" -> {
                    helpers.add(async(() -> agent("helper").remote(goal.agentUrl())
                            .maxDuration(Duration.ofHours(4))
                            .ask(step.task())));
                    observations.add("delegated: " + step.task());
                }
                case "ask_human" -> {
                    Approval approval = human("ask_user").input(Map.of("question", step.question()))
                            .await(Approval.class, Duration.ofDays(1))
                            .orElse(new Approval(false, "no answer within a day"));
                    observations.add("person answered: " + (approval.approved() ? "yes" : "no") + ", " + approval.note());
                }
                case "finish" -> {
                    if (helpers.isEmpty()) {
                        answer = step.answer();
                    } else {
                        // the helpers' findings go to the model before it finishes
                        join(helpers.toArray(Future[]::new));
                        for (Future<AgentResult> helper : helpers) {
                            observations.add("helper found: " + helper.resultNow().text());
                        }
                        helpers.clear();
                    }
                }
                default -> observations.add("unknown action: " + step.action());
            }
        }
        if (!helpers.isEmpty()) {
            join(helpers.toArray(Future[]::new)); // out of steps: leave no helper running
        }
        return new Outcome(answer != null ? answer : "no answer within " + steps + " steps", steps, observations);
    }

    private static String describe(List<McpTool> tools) {
        return tools.stream()
                .map(tool -> "  " + tool.name() + ": " + tool.description() + ", arguments " + tool.inputSchema())
                .collect(Collectors.joining("\n"));
    }

    public static void main(String[] args) {
        ConductorClient client = ConductorClient.fromEnvironment();
        String stub = System.getenv().getOrDefault("STUB_URL", "http://localhost:8765");
        try (Conductor conductor = Conductor.of(client)) {
            conductor.workflows(AgentHarness.class).start();

            Goal goal = new Goal("Plan a day out in Lisbon tomorrow, museum tickets included.", stub + "/mcp",
                    stub + "/.well-known/agent-card.json", 10);
            WorkflowHandle<Outcome> run = conductor.start(AgentHarness::pursue, goal);
            System.out.println("execution: " + run.workflowId());

            // the person: approves when the agent asks
            run.completeTask("ask_user", new Approval(true, "go ahead"), Duration.ofMinutes(2));

            Outcome outcome = run.result(Duration.ofMinutes(3));
            System.out.println("answer:    " + outcome.answer());
            System.out.println("steps:     " + outcome.steps());
            outcome.observations().forEach(line -> System.out.println("  " + line));
        }
        client.shutdown();
    }
}
