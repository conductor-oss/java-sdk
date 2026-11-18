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
import java.util.List;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;

import agents.SupportTicket.Answer;
import agents.SupportTicket.Resolution;
import agents.SupportTicket.Ticket;

import static org.conductoross.conductor.sdk.Conductor.fanOut;

/**
 * The support desk: an inbox of tickets, all handled at once, each by the same code as {@link
 * SupportTicket} (a model decides the team, a SWITCH hands the ticket to that team's agent, the
 * model drafts the reply).
 *
 * <pre>
 *   tickets    FORK_JOIN   one branch per ticket (fanOut), all at once; in each branch the
 *                          triage, delegate and reply steps of SupportTicket
 * </pre>
 *
 * Run as {@link SupportTicket} says, with {@code agents.SupportDesk}.
 */
public class SupportDesk {

    private final SupportTicket desk = new SupportTicket();

    @Workflow(name = "support_desk")
    public List<Resolution> handle(List<Ticket> tickets) {
        // every ticket at once, each in a branch of its own, handled as one ticket is
        return fanOut("tickets", tickets, desk::handle);
    }

    public static void main(String[] args) {
        ConductorClient client = ConductorClient.fromEnvironment();
        Conductor conductor = Conductor.of(client);
        conductor.workflows(SupportDesk.class).start();

        String agents = System.getenv().getOrDefault("AGENT_URL", "http://localhost:8765");
        List<Ticket> inbox = List.of(
                new Ticket("B-1", "ana", "I was charged twice for order 1042, please refund one.", agents),
                new Ticket("T-2", "raj", "Urgent: the app shows an error every time I sign in.", agents),
                new Ticket("S-3", "lee", "My package has not arrived and the tracking page is empty.", agents),
                new Ticket("H-4", "kim", "Do you have a store in Lisbon?", agents));

        WorkflowHandle<List<Resolution>> run = conductor.start(SupportDesk::handle, inbox);
        System.out.println("execution: " + run.url());
        // the ticket no team fits waits for a person: answered here, as a support lead would in the UI
        run.completeTask("support_lead", new Answer("Mia", "Yes, on Rua Augusta since May."), Duration.ofMinutes(2));
        List<Resolution> resolutions = run.result(Duration.ofMinutes(3));
        resolutions.forEach(r -> System.out.println("  " + r));

        boolean routed = resolutions.stream().map(Resolution::team).toList()
                .equals(List.of("billing", "technical", "shipping", "human"));
        System.out.println(routed ? "SUPPORTDESK PASSED" : "SUPPORTDESK FAILED: unexpected routing");
        conductor.close();
        client.shutdown();
        System.exit(routed ? 0 : 1);
    }
}
