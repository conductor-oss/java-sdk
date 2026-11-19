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

import java.time.Duration;
import java.util.Map;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;

import static org.conductoross.conductor.sdk.Conductor.agent;
import static org.conductoross.conductor.sdk.Conductor.human;
import static org.conductoross.conductor.sdk.Conductor.llm;
import static org.conductoross.conductor.sdk.Conductor.switchOn;

/**
 * Customer service, agentic, one ticket: a model decides who should handle the ticket, and a SWITCH
 * hands it to that team's agent.
 *
 * <pre>
 *   triage     LLM_CHAT_COMPLETE   the model reads the ticket and answers, as JSON, which team should
 *                                  take it, why, and whether it is urgent: a Routing record
 *   delegate   SWITCH on the team  billing, technical and shipping each have an agent (an AGENT
 *                                  task over A2A); a ticket no team fits goes to a person (HUMAN)
 *   reply      LLM_CHAT_COMPLETE   the model drafts the customer's reply from what was done
 * </pre>
 *
 * The decision is the model's, the delegation is plain code: a {@code switchOn} on the team it
 * chose. The diagram shows the SWITCH with every team, the taken one holding what ran. Each step
 * waits on the server, so an agent or a person may take hours and nothing is held meanwhile.
 * {@link SupportDesk} handles a whole inbox of tickets at once with this same code.
 *
 * <p>Runs against the stub in {@code native-examples/stubs/ai_stub.py}: a stand-in model that routes
 * by keywords, and an A2A agent per team. The server needs its LiteLLM provider pointed at the stub:
 *
 * <pre>
 *   python3 native-examples/stubs/ai_stub.py 8765 &amp;
 *   # the server started with --conductor.ai.litellm.baseURL=http://localhost:8765
 *   CONDUCTOR_SERVER_URL=http://localhost:8099/api CONDUCTOR_DEV=true \
 *     java -cp "native-examples/build/install/native-examples/lib/*" agents.SupportTicket ["ticket text"]
 * </pre>
 *
 * With a real model, set {@code ROUTER} and {@code WRITER} to models of a provider the server has.
 */
public class SupportTicket {

    /** A customer's ticket, and where the teams' agents are. */
    public record Ticket(String id, String customer, String message, String agentUrl) {}

    /** The model's decision. */
    public record Routing(String team, String reason, boolean urgent) {}

    /** What a person answered, for a ticket no team fits. */
    public record Answer(String by, String text) {}

    public record Resolution(String ticket, String team, String reason, String work, String reply) {}

    static final String ROUTER = "litellm/support-router";
    static final String WRITER = "litellm/support-writer";

    static final String DISPATCHER = """
            You are the dispatcher of a customer support desk. The teams are:
              billing    refunds, double charges, invoices, payments
              technical  errors, crashes, logins, passwords, bugs in the app
              shipping   deliveries, packages, tracking
            Read the customer's ticket and answer only with JSON:
              {"team": "billing" | "technical" | "shipping" | "human", "reason": "<why>", "urgent": true | false}
            Answer "human" when no team fits.""";

    @Workflow(name = "support_ticket")
    public Resolution handle(Ticket ticket) {
        // 1. the model reads the ticket and decides who should take it
        Routing routing = llm("triage").model(ROUTER).system(DISPATCHER).prompt(ticket.message()).as(Routing.class);

        // 2. a SWITCH hands the ticket to that team's agent; a ticket no team fits goes to a person
        String brief = "Ticket " + ticket.id() + " from " + ticket.customer() + ": " + ticket.message()
                + (routing.urgent() ? " (urgent)" : "");
        String agents = ticket.agentUrl() + "/a2a/";
        String work = switchOn("delegate", routing.team())
                .when("billing", () -> agent("billing_agent").remote(agents + "billing").ask(brief).text())
                .when("technical", () -> agent("technical_agent").remote(agents + "technical").ask(brief).text())
                .when("shipping", () -> agent("shipping_agent").remote(agents + "shipping").ask(brief).text())
                .otherwise(() -> human("support_lead").assignee("support-leads")
                        .input(Map.of("ticket", brief, "why", routing.reason()))
                        .await(Answer.class, Duration.ofDays(1))
                        .map(answer -> answer.by() + ": " + answer.text())
                        .orElse("nobody answered within a day"))
                .get();

        // 3. the model drafts the reply to the customer from what was done
        String reply = llm("reply").model(WRITER)
                .system("Write a short, friendly reply to the customer about how their ticket was handled.")
                .prompt(work)
                .as(String.class);
        return new Resolution(ticket.id(), routing.team(), routing.reason(), work, reply);
    }

    /** Handles one ticket: the text given, or a billing one. */
    public static void main(String[] args) {
        ConductorClient client = ConductorClient.fromEnvironment();
        Conductor conductor = Conductor.of(client);
        conductor.workflows(SupportTicket.class).start();

        String message = args.length > 0 ? args[0] : "I was charged twice for order 1042, please refund one.";
        Ticket ticket = new Ticket("T-1", "ana", message, System.getenv().getOrDefault("AGENT_URL", "http://localhost:8765"));
        WorkflowHandle<Resolution> run = conductor.start(SupportTicket::handle, ticket);
        System.out.println("execution: " + run.workflowId());
        System.out.println("result:    " + run.result(Duration.ofMinutes(2)));
        conductor.close();
        client.shutdown();
        System.exit(0);
    }
}
