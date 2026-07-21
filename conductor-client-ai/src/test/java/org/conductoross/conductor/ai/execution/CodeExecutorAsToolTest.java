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
package org.conductoross.conductor.ai.execution;

import org.conductoross.conductor.ai.model.ToolDef;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CodeExecutor#asTool()} must propagate the executor's timeout onto the
 * generated {@link ToolDef}, so worker registration sizes the Conductor task
 * def's responseTimeout to the handler's blocking duration. Building the tool
 * does not execute anything — no Docker required.
 */
class CodeExecutorAsToolTest {

    @Test
    void asTool_propagates_executor_timeout_to_toolDef() {
        DockerCodeExecutor executor = new DockerCodeExecutor("python:3.12-slim", "python", 420);
        ToolDef tool = executor.asTool("py_docker_exec", "run python in docker");

        assertEquals(
                420,
                tool.getTimeoutSeconds(),
                "asTool() must carry the executor's 420s timeout onto the ToolDef so a "
                        + "long-running container exec isn't reclaimed at the 300s default. "
                        + "COUNTERFACTUAL: if timeout isn't propagated, getTimeoutSeconds()==0.");
        assertEquals("worker", tool.getToolType());
    }
}
