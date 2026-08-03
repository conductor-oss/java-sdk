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
package org.conductoross.conductor.ai.examples;

import io.orkes.conductor.client.AgentClient;
import io.orkes.conductor.client.ApiClient;
import io.orkes.conductor.client.OrkesClients;
import io.orkes.conductor.client.model.agent.AgentRequest;
import io.orkes.conductor.client.model.agent.AgentStatusResponse;
import io.orkes.conductor.client.model.agent.StartResponse;

/**
 * Example 71 — start and control an already-deployed agent through {@link AgentClient}.
 *
 * <p>Deploy the agent definition separately, then run:
 *
 * <pre>{@code
 * ./gradlew :agent-examples:run \
 *   -PmainClass=org.conductoross.conductor.ai.examples.Example71AgentControlPlane \
 *   --args="researcher 3 'Summarize the latest release' status"
 * }</pre>
 *
 * <p>The final argument is {@code status}, {@code stop}, or {@code cancel}. Use {@code -} for the
 * version to select the server's deployed default.
 */
public final class Example71AgentControlPlane {

    private Example71AgentControlPlane() {}

    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException(
                    "Usage: <agent-name> [version|-] [prompt] [status|stop|cancel]");
        }

        String name = args[0];
        Integer version = args.length > 1 && !"-".equals(args[1])
                ? Integer.valueOf(args[1])
                : null;
        String prompt = args.length > 2 ? args[2] : "Summarize the latest release.";
        String action = args.length > 3 ? args[3] : "status";

        ApiClient transport = createTransport();
        try (AgentClient agents = new OrkesClients(transport).getAgentClient()) {
            AgentRequest request = AgentRequest.deployedAgent(name, version)
                    .prompt(prompt)
                    .build();
            StartResponse started = agents.startAgent(request);
            String executionId = started.getExecutionId();
            System.out.println("Started " + name + " as " + executionId);

            switch (action) {
                case "stop" -> {
                    agents.stopAgent(executionId);
                    System.out.println("Graceful stop requested after the current iteration.");
                }
                case "cancel" -> {
                    agents.cancelAgent(executionId, "Cancelled from Example71AgentControlPlane");
                    System.out.println("Execution cancelled immediately.");
                }
                case "status" -> printStatus(agents.getAgentStatus(executionId));
                default -> throw new IllegalArgumentException(
                        "Action must be status, stop, or cancel: " + action);
            }
        }
    }

    private static ApiClient createTransport() {
        if (Settings.AUTH_KEY != null && Settings.AUTH_SECRET != null) {
            return new ApiClient(Settings.SERVER_URL, Settings.AUTH_KEY, Settings.AUTH_SECRET);
        }
        return new ApiClient(Settings.SERVER_URL);
    }

    private static void printStatus(AgentStatusResponse status) {
        System.out.println("Status: " + status.getStatus());
        System.out.println("Started: " + status.getStartTime());
        System.out.println("Ended: " + status.getEndTime());
    }
}
