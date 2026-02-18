/*
 * Copyright 2024 Conductor Authors.
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
package io.orkes.conductor.sdk.examples.validation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.netflix.conductor.client.automator.TaskRunnerConfigurer;
import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.MetadataClient;
import com.netflix.conductor.client.http.TaskClient;
import com.netflix.conductor.client.http.WorkflowClient;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.sdk.workflow.def.ConductorWorkflow;
import com.netflix.conductor.sdk.workflow.def.tasks.DoWhile;
import com.netflix.conductor.sdk.workflow.def.tasks.ForkJoin;
import com.netflix.conductor.sdk.workflow.def.tasks.Http;
import com.netflix.conductor.sdk.workflow.def.tasks.LlmChatComplete;
import com.netflix.conductor.sdk.workflow.def.tasks.LlmGenerateEmbeddings;
import com.netflix.conductor.sdk.workflow.def.tasks.LlmIndexDocument;
import com.netflix.conductor.sdk.workflow.def.tasks.LlmSearchIndex;
import com.netflix.conductor.sdk.workflow.def.tasks.LlmTextComplete;
import com.netflix.conductor.sdk.workflow.def.tasks.SetVariable;
import com.netflix.conductor.sdk.workflow.def.tasks.SimpleTask;
import com.netflix.conductor.sdk.workflow.def.tasks.Switch;
import com.netflix.conductor.sdk.workflow.executor.WorkflowExecutor;

/**
 * End-to-end validation script for the Conductor Java SDK.
 * <p>
 * This script validates:
 * <ul>
 *   <li>SDK connectivity to Conductor server</li>
 *   <li>Workflow registration</li>
 *   <li>Worker task execution</li>
 *   <li>Workflow execution and completion</li>
 *   <li>LLM task class instantiation (compilation test)</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>
 * export CONDUCTOR_SERVER_URL=http://localhost:7001/api
 * ./gradlew :examples:run -PmainClass=io.orkes.conductor.sdk.examples.validation.SdkValidationTest
 * </pre>
 */
public class SdkValidationTest {

    private static final String VALIDATION_WORKFLOW = "sdk_validation_workflow";
    private static int passedTests = 0;
    private static int failedTests = 0;

    public static void main(String[] args) {
        String serverUrl = System.getenv().getOrDefault("CONDUCTOR_SERVER_URL", "http://localhost:7001/api");
        
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║       Conductor Java SDK End-to-End Validation               ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Server URL: " + serverUrl);
        System.out.println();

        ConductorClient client = null;
        WorkflowExecutor executor = null;
        TaskRunnerConfigurer taskRunner = null;

        try {
            // Test 1: Client connectivity
            printTest("Test 1: Client Connectivity");
            client = ConductorClient.builder()
                .basePath(serverUrl)
                .build();
            
            // Verify connectivity by getting server health
            MetadataClient metadataClient = new MetadataClient(client);
            try {
                metadataClient.getAllWorkflowsWithLatestVersions();
                pass("Connected to Conductor server");
            } catch (Exception e) {
                fail("Cannot connect to Conductor server: " + e.getMessage());
                throw e;
            }

            // Test 2: LLM Task Classes Instantiation
            printTest("Test 2: LLM Task Classes Instantiation");
            try {
                // Test all new LLM task classes can be instantiated
                LlmTextComplete textTask = new LlmTextComplete("test_text", "text_ref")
                    .llmProvider("openai")
                    .model("gpt-4")
                    .promptName("test-prompt")
                    .temperature(0.7);
                pass("LlmTextComplete instantiated");

                LlmChatComplete chatTask = new LlmChatComplete("test_chat", "chat_ref")
                    .llmProvider("openai")
                    .model("gpt-4")
                    .messages(List.of(Map.of("role", "user", "content", "test")))
                    .temperature(0.7);
                pass("LlmChatComplete instantiated");

                LlmIndexDocument indexTask = new LlmIndexDocument("test_index", "index_ref")
                    .vectorDb("pinecone")
                    .namespace("test")
                    .index("test-index")
                    .embeddingModel("text-embedding-ada-002")
                    .text("test document");
                pass("LlmIndexDocument instantiated");

                LlmSearchIndex searchTask = new LlmSearchIndex("test_search", "search_ref")
                    .vectorDb("pinecone")
                    .namespace("test")
                    .index("test-index")
                    .query("test query")
                    .topK(5);
                pass("LlmSearchIndex instantiated");

                LlmGenerateEmbeddings embedTask = new LlmGenerateEmbeddings("test_embed", "embed_ref")
                    .llmProvider("openai")
                    .model("text-embedding-ada-002")
                    .text("test text");
                pass("LlmGenerateEmbeddings instantiated");
            } catch (Exception e) {
                fail("LLM task class instantiation failed: " + e.getMessage());
            }

            // Test 3: Workflow Definition
            printTest("Test 3: Workflow Definition with SDK");
            executor = new WorkflowExecutor(client, 100);
            ConductorWorkflow<Map<String, Object>> workflow = createValidationWorkflow(executor);
            pass("Workflow definition created with tasks: " + 
                workflow.toWorkflowDef().getTasks().size() + " tasks");

            // Test 4: Workflow Registration
            printTest("Test 4: Workflow Registration");
            try {
                boolean registered = workflow.registerWorkflow(true, true);
                if (registered) {
                    pass("Workflow registered: " + VALIDATION_WORKFLOW);
                } else {
                    fail("Workflow registration returned false");
                }
            } catch (Exception e) {
                fail("Workflow registration failed: " + e.getMessage());
            }

            // Test 5: Start Workers
            printTest("Test 5: Worker Initialization");
            TaskClient taskClient = new TaskClient(client);
            List<Worker> workers = List.of(
                new ValidationWorker("validation_task_1"),
                new ValidationWorker("validation_task_2"),
                new ValidationWorker("validation_task_3")
            );
            
            taskRunner = new TaskRunnerConfigurer.Builder(taskClient, workers)
                .withThreadCount(3)
                .build();
            taskRunner.init();
            pass("Workers started for tasks: validation_task_1, validation_task_2, validation_task_3");

            // Give workers time to start polling
            Thread.sleep(2000);

            // Test 6: Workflow Execution
            printTest("Test 6: Workflow Execution");
            Map<String, Object> input = new HashMap<>();
            input.put("name", "Conductor SDK Validation");
            input.put("testValue", 42);

            try {
                var workflowRun = workflow.execute(input);
                Workflow result = workflowRun.get(30, TimeUnit.SECONDS);
                
                if (result.getStatus() == Workflow.WorkflowStatus.COMPLETED) {
                    pass("Workflow completed successfully");
                    pass("Workflow ID: " + result.getWorkflowId());
                    pass("Output: " + result.getOutput());
                } else {
                    fail("Workflow ended with status: " + result.getStatus());
                    if (result.getReasonForIncompletion() != null) {
                        fail("Reason: " + result.getReasonForIncompletion());
                    }
                }
            } catch (Exception e) {
                fail("Workflow execution failed: " + e.getMessage());
            }

            // Test 7: Workflow Client Operations
            printTest("Test 7: Workflow Client Operations");
            WorkflowClient workflowClient = new WorkflowClient(client);
            try {
                // Start another workflow using StartWorkflowRequest
                StartWorkflowRequest startRequest = new StartWorkflowRequest();
                startRequest.setName(VALIDATION_WORKFLOW);
                startRequest.setVersion(1);
                startRequest.setInput(input);
                String workflowId = workflowClient.startWorkflow(startRequest);
                pass("Started workflow via WorkflowClient: " + workflowId);

                // Wait a bit for it to start
                Thread.sleep(1000);

                // Get workflow status
                Workflow wf = workflowClient.getWorkflow(workflowId, true);
                pass("Retrieved workflow status: " + wf.getStatus());

                // Wait for completion
                int attempts = 0;
                while (wf.getStatus() == Workflow.WorkflowStatus.RUNNING && attempts < 30) {
                    Thread.sleep(1000);
                    wf = workflowClient.getWorkflow(workflowId, false);
                    attempts++;
                }
                
                if (wf.getStatus() == Workflow.WorkflowStatus.COMPLETED) {
                    pass("Second workflow completed: " + workflowId);
                } else {
                    fail("Second workflow status: " + wf.getStatus());
                }
            } catch (Exception e) {
                fail("Workflow client operations failed: " + e.getMessage());
            }

            // Test 8: Complex Task Types
            printTest("Test 8: Complex Task Types (Switch, DoWhile, ForkJoin)");
            try {
                // Test Switch task
                Switch switchTask = new Switch("switch_ref", "${workflow.input.condition}");
                switchTask.switchCase("case1", new SimpleTask("task1", "task1_ref"));
                switchTask.switchCase("case2", new SimpleTask("task2", "task2_ref"));
                switchTask.defaultCase(new SimpleTask("default", "default_ref"));
                pass("Switch task created with 2 cases + default");

                // Test DoWhile task
                DoWhile doWhile = new DoWhile("loop_ref", 3, 
                    new SimpleTask("loop_task", "loop_task_ref"));
                pass("DoWhile task created with 3 iterations");

                // Test ForkJoin task - each array is a parallel branch
                SimpleTask fork1 = new SimpleTask("fork1", "fork1_ref");
                SimpleTask fork2 = new SimpleTask("fork2", "fork2_ref");
                @SuppressWarnings("unchecked")
                ForkJoin forkJoin = new ForkJoin("fork_ref", 
                    new com.netflix.conductor.sdk.workflow.def.tasks.Task<?>[]{fork1}, 
                    new com.netflix.conductor.sdk.workflow.def.tasks.Task<?>[]{fork2});
                pass("ForkJoin task created with 2 parallel branches");

                // Test Http task
                Http httpTask = new Http("http_ref");
                httpTask.url("https://httpbin.org/get")
                    .method(Http.Input.HttpMethod.GET);
                pass("Http task created");

                // Test SetVariable task
                SetVariable setVar = new SetVariable("setvar_ref");
                setVar.input("myVar", "myValue");
                pass("SetVariable task created");
            } catch (Exception e) {
                fail("Complex task types failed: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("\n*** Fatal error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cleanup
            if (taskRunner != null) {
                taskRunner.shutdown();
            }
            if (executor != null) {
                executor.shutdown();
            }
        }

        // Print summary
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                      VALIDATION SUMMARY                       ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.printf("║  Passed: %-4d                                                ║%n", passedTests);
        System.out.printf("║  Failed: %-4d                                                ║%n", failedTests);
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        if (failedTests == 0) {
            System.out.println("║  Result: ALL TESTS PASSED                                    ║");
        } else {
            System.out.println("║  Result: SOME TESTS FAILED                                   ║");
        }
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        System.exit(failedTests > 0 ? 1 : 0);
    }

    /**
     * Creates a simple validation workflow with multiple task types.
     */
    private static ConductorWorkflow<Map<String, Object>> createValidationWorkflow(WorkflowExecutor executor) {
        ConductorWorkflow<Map<String, Object>> workflow = new ConductorWorkflow<>(executor);
        workflow.setName(VALIDATION_WORKFLOW);
        workflow.setVersion(1);
        workflow.setOwnerEmail("validation@conductor-oss.org");
        workflow.setDescription("SDK validation workflow");

        // Task 1: Simple task
        SimpleTask task1 = new SimpleTask("validation_task_1", "task1_ref");
        task1.input("input_name", "${workflow.input.name}");
        workflow.add(task1);

        // Task 2: Another simple task using output from task 1
        SimpleTask task2 = new SimpleTask("validation_task_2", "task2_ref");
        task2.input("previous_output", "${task1_ref.output.result}");
        task2.input("test_value", "${workflow.input.testValue}");
        workflow.add(task2);

        // Task 3: Final task
        SimpleTask task3 = new SimpleTask("validation_task_3", "task3_ref");
        task3.input("all_results", Map.of(
            "task1", "${task1_ref.output.result}",
            "task2", "${task2_ref.output.result}"
        ));
        workflow.add(task3);

        // Set workflow output
        Map<String, Object> output = new HashMap<>();
        output.put("finalResult", "${task3_ref.output.result}");
        output.put("task1Result", "${task1_ref.output.result}");
        output.put("task2Result", "${task2_ref.output.result}");
        workflow.setWorkflowOutput(output);

        return workflow;
    }

    private static void printTest(String testName) {
        System.out.println();
        System.out.println("─────────────────────────────────────────────────────────────────");
        System.out.println(testName);
        System.out.println("─────────────────────────────────────────────────────────────────");
    }

    private static void pass(String message) {
        System.out.println("  ✓ " + message);
        passedTests++;
    }

    private static void fail(String message) {
        System.out.println("  ✗ " + message);
        failedTests++;
    }

    /**
     * Simple worker for validation testing.
     */
    static class ValidationWorker implements Worker {
        private final String taskDefName;

        ValidationWorker(String taskDefName) {
            this.taskDefName = taskDefName;
        }

        @Override
        public String getTaskDefName() {
            return taskDefName;
        }

        @Override
        public TaskResult execute(Task task) {
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.COMPLETED);
            
            // Echo input back as output with some processing
            Map<String, Object> output = new HashMap<>();
            output.put("result", "Processed by " + taskDefName);
            output.put("inputReceived", task.getInputData());
            output.put("timestamp", System.currentTimeMillis());
            result.setOutputData(output);
            
            return result;
        }
    }
}
