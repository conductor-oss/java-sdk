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
import org.conductoross.conductor.sdk.testing.TestRun;
import org.junit.jupiter.api.Test;

import agents.SupportTicket.Answer;
import agents.SupportTicket.Resolution;
import agents.SupportTicket.Ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The model's decision picks the agent: each answer is stubbed, no server and no model. */
class SupportTicketTest {

    private static final Ticket TICKET = new Ticket("B-1", "ana", "I was charged twice.", "http://agents");

    @Test
    void theModelsDecisionHandsTheTicketToThatTeamsAgent() {
        DurableTestEnvironment env = DurableTestEnvironment.inMemory().workflows(SupportTicket.class);
        env.llm("triage").respond("{\"team\": \"billing\", \"reason\": \"a double charge\", \"urgent\": false}");
        env.agent("billing_agent").respond(new AgentResult("refunded one charge", "completed", List.of(), "t-1", "c-1"));
        env.llm("reply").respond("Dear Ana, we refunded the second charge.");

        TestRun<Resolution> run = env.start(SupportTicket::handle, TICKET);

        assertEquals(new Resolution("B-1", "billing", "a double charge", "refunded one charge",
                "Dear Ana, we refunded the second charge."), run.result());
        assertTrue(run.ran("billing_agent"));
        assertFalse(run.ran("technical_agent"));
        assertFalse(run.ran("support_lead"));
    }

    @Test
    void aTicketNoTeamFitsGoesToAPerson() {
        DurableTestEnvironment env = DurableTestEnvironment.inMemory().workflows(SupportTicket.class);
        env.llm("triage").respond("{\"team\": \"human\", \"reason\": \"no team fits\", \"urgent\": false}");
        env.human("support_lead").answer(new Answer("Mia", "Yes, in Lisbon."));
        env.llm("reply").respond("Dear Kim, yes, we have a store in Lisbon.");

        Resolution r = env.start(SupportTicket::handle, TICKET).result();

        assertEquals("human", r.team());
        assertEquals("Mia: Yes, in Lisbon.", r.work());
    }
}
