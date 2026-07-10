/*
 * Copyright 2025 Conductor Authors.
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
package org.conductoross.conductor.ai.model;

/**
 * Result of deploying an agent to the server.
 *
 * <pre>{@code
 * List<DeploymentInfo> deployments = runtime.deploy(agent1, agent2);
 * for (DeploymentInfo d : deployments) {
 *     System.out.println("Deployed: " + d.getAgentName() + " -> " + d.getRegisteredName());
 * }
 * }</pre>
 */
public class DeploymentInfo {
    private final String registeredName;
    private final String agentName;

    public DeploymentInfo(String registeredName, String agentName) {
        this.registeredName = registeredName;
        this.agentName = agentName;
    }

    /** The name under which this agent is registered on the server. */
    public String getRegisteredName() {
        return registeredName;
    }

    /** The original agent name. */
    public String getAgentName() {
        return agentName;
    }

    @Override
    public String toString() {
        return "DeploymentInfo{agentName=" + agentName + ", registeredName=" + registeredName + "}";
    }
}
