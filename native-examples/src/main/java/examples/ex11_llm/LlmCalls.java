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
package examples.ex11_llm;

import java.time.Duration;

import org.conductoross.conductor.sdk.Conductor;
import org.conductoross.conductor.sdk.Workflow;
import org.conductoross.conductor.sdk.WorkflowHandle;

import com.netflix.conductor.client.http.ConductorClient;

import static org.conductoross.conductor.sdk.Conductor.llm;

/**
 * 11 LLM: calling a model from a workflow.
 *
 * <p>{@code llm(ref)} is Conductor's LLM_CHAT_COMPLETE task: the server calls the model through the
 * providers it is configured with, so no API key lives in your code, and the prompt and the answer
 * are recorded on the execution.
 *
 * <pre>
 *   llm(ref).model("provider/model")   a model of a provider configured on the server, such as
 *                                      openai/gpt-4o-mini or anthropic/claude-sonnet-4-5
 *     .system(instructions)            the system message
 *     .prompt(text)                    the user message
 *     .retries(count, delay)           retries when the call fails
 *     .as(String.class)                the answer as text
 *     .as(Record.class)                the answer read as JSON into a record: structured output
 * </pre>
 *
 * <p>Runs against the stand-in model in {@code native-examples/stubs/ai_stub.py}, through the
 * server's LiteLLM provider. With a real provider, set {@link #CLASSIFIER} and {@link #WRITER} to its
 * models.
 *
 * <pre>
 *   python3 native-examples/stubs/ai_stub.py 8765 &amp;
 *   # the server started with --conductor.ai.litellm.baseURL=http://localhost:8765
 *   CONDUCTOR_SERVER_URL=http://localhost:8080/api ./gradlew :native-examples:ex11_llm
 * </pre>
 */
public class LlmCalls {

    static final String CLASSIFIER = "litellm/support-router";
    static final String WRITER = "litellm/support-writer";

    public record Message(String customer, String text) {}

    /** The model's answer, read from its JSON. */
    public record Triage(String team, String reason, boolean urgent) {}

    public record Handled(Triage triage, String reply) {}

    @Workflow(name = "llm_calls")
    public Handled handle(Message message) {
        // structured output: the model answers JSON, read into a record
        Triage triage = llm("classify").model(CLASSIFIER)
                .system("""
                        Decide which team should handle the customer's message: billing, technical,
                        shipping, or human when none fits. Answer only with JSON:
                          {"team": "...", "reason": "<why>", "urgent": true | false}""")
                .prompt(message.text())
                .as(Triage.class);

        // text
        String reply = llm("draft_reply").model(WRITER)
                .system("Write a short, friendly reply to the customer.")
                .prompt(message.text())
                .retries(2, Duration.ofSeconds(2))
                .as(String.class);

        return new Handled(triage, reply);
    }

    public static void main(String[] args) {
        ConductorClient client = ConductorClient.fromEnvironment();
        try (Conductor conductor = Conductor.of(client)) {
            conductor.workflows(LlmCalls.class).start();

            Message message = new Message("ana", "I was charged twice for order 1042, please refund one.");
            WorkflowHandle<Handled> run = conductor.start(LlmCalls::handle, message);
            System.out.println("execution: " + run.workflowId());
            System.out.println("result:    " + run.result(Duration.ofSeconds(60)));
        }
        client.shutdown();
    }
}
