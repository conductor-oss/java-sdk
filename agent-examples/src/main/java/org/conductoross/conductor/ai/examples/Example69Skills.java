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
package org.conductoross.conductor.ai.examples;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.conductoross.conductor.ai.Agent;
import org.conductoross.conductor.ai.AgentRuntime;
import org.conductoross.conductor.ai.model.AgentResult;
import org.conductoross.conductor.ai.skill.Skill;
import org.conductoross.conductor.ai.tools.AgentTool;

/**
 * Example 69 — Skills
 *
 * <p>Loads an agentskills.io skill directory as an Conductor Agent. Skill scripts
 * become worker tools, and resource files are available through the generated
 * read_skill_file tool.
 *
 * <p>Usage:
 * <pre>
 *   CONDUCTOR_SERVER_URL=http://localhost:8080/api \
 *   CONDUCTOR_AGENT_LLM_MODEL=openai/gpt-4o-mini \
 *   ./gradlew :examples:run -PmainClass=org.conductoross.conductor.ai.examples.Example69Skills \
 *     --args="/path/to/skill 'Review this repository'"
 * </pre>
 */
public class Example69Skills {

    public static void main(String[] args) {
        AgentRuntime runtime = new AgentRuntime();
        Path skillPath = args.length > 0
            ? Paths.get(args[0])
            : Paths.get(System.getProperty("user.home"), ".claude", "skills", "dg");
        String prompt = args.length > 1
            ? args[1]
            : "Run this skill against the current request and return a concise result.";

        if (!Files.exists(skillPath.resolve("SKILL.md"))) {
            throw new IllegalArgumentException(
                "Expected a skill directory containing SKILL.md: " + skillPath.toAbsolutePath());
        }

        Agent skillAgent = Skill.skill(
            skillPath,
            Settings.LLM_MODEL,
            null,
            null,
            List.of(Paths.get(System.getProperty("user.home"), ".agents", "skills")));

        AgentResult direct = runtime.run(skillAgent, prompt);
        direct.printResult();

        Agent parent = Agent.builder()
            .name("skill_tool_manager_69")
            .model(Settings.LLM_MODEL)
            .instructions(
                "Use the wrapped skill tool for the user request, then return the skill result.")
            .tools(List.of(AgentTool.from(skillAgent, "Run the loaded skill")))
            .maxTurns(4)
            .build();

        AgentResult viaTool = runtime.run(parent, prompt);
        viaTool.printResult();

        runtime.shutdown();
    }
}
