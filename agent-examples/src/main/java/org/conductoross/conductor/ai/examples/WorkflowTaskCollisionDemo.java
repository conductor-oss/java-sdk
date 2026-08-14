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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.MetadataClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.common.run.Workflow;

/**
 * Task name collision without agents — plain workflows and plain workers.
 *
 * <p>Two workflows, unique names. One shared task name, two worker implementations.
 *
 * <p>Part 1 — no domain: both workers poll the bare queue, wf_alpha gets either answer.
 * <p>Part 2 — taskToDomain: the same two workers route correctly, every run.
 *
 * <p>Queue name is domain:taskType. Workflow name never reaches it.
 */
public class WorkflowTaskCollisionDemo {

    private static final String SHARED_TASK = "wfdemo_shared_task";
    private static final String WF_ALPHA = "wfdemo_alpha";
    private static final String WF_BRAVO = "wfdemo_bravo";
    private static final int RUNS = 6;

    static class LabelledWorker implements Worker {
        private final String label;

        LabelledWorker(String label) {
            this.label = label;
        }

        @Override
        public String getTaskDefName() {
            return SHARED_TASK;
        }

        @Override
        public TaskResult execute(Task task) {
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("answer", label);
            return result;
        }

        @Override
        public int getPollingInterval() {
            return 100;
        }
    }

    private static WorkflowDef workflowDef(String name) {
        WorkflowTask step = new WorkflowTask();
        step.setName(SHARED_TASK);
        step.setTaskReferenceName("step");
        step.setType("SIMPLE");

        WorkflowDef def = new WorkflowDef();
        def.setName(name);
        def.setVersion(1);
        def.setSchemaVersion(2);
        def.setOwnerEmail("demo@example.com");
        def.setTimeoutSeconds(120);
        def.setTasks(List.of(step));
        def.setOutputParameters(Map.of("answer", "${step.output.answer}"));
        return def;
    }

    private static String runOnce(WorkflowClient workflows, Map<String, String> taskToDomain) throws Exception {
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(WF_ALPHA);
        request.setVersion(1);
        if (taskToDomain != null) {
            request.setTaskToDomain(taskToDomain);
        }
        String id = workflows.startWorkflow(request);

        for (int i = 0; i < 100; i++) {
            Workflow wf = workflows.getWorkflow(id, false);
            if (wf.getStatus() != null && wf.getStatus().isTerminal()) {
                Object answer = wf.getOutput().get("answer");
                return answer != null ? String.valueOf(answer) : "<no answer: " + wf.getStatus() + ">";
            }
            Thread.sleep(200);
        }
        return "<timeout>";
    }

    private static void tally(String heading, WorkflowClient workflows, Map<String, String> taskToDomain)
            throws Exception {
        Map<String, Integer> counts = new TreeMap<>();
        for (int i = 0; i < RUNS; i++) {
            String answer = runOnce(workflows, taskToDomain);
            counts.merge(answer, 1, Integer::sum);
        }
        System.out.println(heading);
        counts.forEach((k, v) -> System.out.println("    " + k + " x" + v));
    }

    public static void main(String[] args) throws Exception {
        String serverUrl = System.getenv().getOrDefault("CONDUCTOR_SERVER_URL", "http://localhost:8080/api");
        ConductorClient client = new ConductorClient(serverUrl);
        MetadataClient metadata = new MetadataClient(client);
        WorkflowClient workflows = new WorkflowClient(client);
        TaskClient tasks = new TaskClient(client);

        TaskDef taskDef = new TaskDef(SHARED_TASK);
        taskDef.setOwnerEmail("demo@example.com");
        taskDef.setRetryCount(0);
        taskDef.setTimeoutSeconds(60);
        taskDef.setResponseTimeoutSeconds(30);
        metadata.registerTaskDefs(List.of(taskDef));

        for (String name : List.of(WF_ALPHA, WF_BRAVO)) {
            try {
                metadata.registerWorkflowDef(workflowDef(name));
                System.out.println("registered " + name + " v1");
            } catch (Exception e) {
                System.out.println("register " + name + " v1 rejected: " + e.getClass().getSimpleName());
                metadata.updateWorkflowDefs(List.of(workflowDef(name)));
                System.out.println("    updateWorkflowDefs overwrote it in place");
            }
        }

        Worker alpha = new LabelledWorker("ALPHA-111");
        Worker bravo = new LabelledWorker("BRAVO-222");

        System.out.println();
        System.out.println("shared task name: " + SHARED_TASK);
        System.out.println("wf_alpha owns ALPHA-111, wf_bravo owns BRAVO-222; only wf_alpha is started");
        System.out.println();

        TaskRunnerConfigurer noDomain = new TaskRunnerConfigurer.Builder(tasks, List.of(alpha, bravo))
                .withThreadCount(2)
                .build();
        noDomain.init();
        tally("PART 1 — no domain, queue is the bare task name:", workflows, null);
        noDomain.shutdown();

        Map<String, String> alphaDomain = new LinkedHashMap<>();
        alphaDomain.put(SHARED_TASK, "alpha");
        Map<String, String> bravoDomain = new LinkedHashMap<>();
        bravoDomain.put(SHARED_TASK, "bravo");

        TaskRunnerConfigurer alphaRunner = new TaskRunnerConfigurer.Builder(tasks, List.of(alpha))
                .withTaskToDomain(alphaDomain)
                .withThreadCount(1)
                .build();
        TaskRunnerConfigurer bravoRunner = new TaskRunnerConfigurer.Builder(tasks, List.of(bravo))
                .withTaskToDomain(bravoDomain)
                .withThreadCount(1)
                .build();
        alphaRunner.init();
        bravoRunner.init();
        Thread.sleep(1000);

        System.out.println();
        tally("PART 2 — taskToDomain=alpha, queue is alpha:" + SHARED_TASK + ":", workflows, alphaDomain);
        alphaRunner.shutdown();
        bravoRunner.shutdown();

        System.out.println();
        System.out.println("part 1 mixes because workflow name never reaches the queue name");
        System.out.println("part 2 is clean because domain does");
        System.exit(0);
    }
}
