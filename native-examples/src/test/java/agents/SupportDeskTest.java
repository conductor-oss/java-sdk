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

import java.util.List;

import org.conductoross.conductor.sdk.AgentCall.AgentResult;
import org.conductoross.conductor.sdk.testing.DurableTestEnvironment;
import org.junit.jupiter.api.Test;

import agents.SupportTicket.Resolution;
import agents.SupportTicket.Ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The desk handles each ticket of its inbox as SupportTicket handles one. */
class SupportDeskTest {

    @Test
    void everyTicketOfTheInboxIsRoutedAndAnswered() {
        DurableTestEnvironment env = DurableTestEnvironment.inMemory().workflows(SupportDesk.class);
        env.llm("triage").respond("{\"team\": \"billing\", \"reason\": \"a charge\", \"urgent\": false}");
        env.llm("triage_2").respond("{\"team\": \"shipping\", \"reason\": \"a package\", \"urgent\": false}");
        env.agent("billing_agent").respond(new AgentResult("refunded", "completed", List.of(), null, "c-1"));
        env.agent("shipping_agent").respond(new AgentResult("resent", "completed", List.of(), null, "c-2"));
        env.llm("reply").respond("Dear customer, done.");

        List<Resolution> resolutions = env.start(SupportDesk::handle, List.of(
                new Ticket("B-1", "ana", "charged twice", "http://agents"),
                new Ticket("S-2", "lee", "package missing", "http://agents"))).result();

        assertEquals(List.of("billing", "shipping"), resolutions.stream().map(Resolution::team).toList());
        assertEquals(List.of("refunded", "resent"), resolutions.stream().map(Resolution::work).toList());
    }
}
