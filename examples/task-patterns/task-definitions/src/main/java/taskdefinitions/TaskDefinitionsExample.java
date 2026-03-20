package taskdefinitions;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.run.Workflow;
import taskdefinitions.workers.FastTaskWorker;

import java.util.List;
import java.util.Map;

/**
 * Task Definitions — Configure retries, timeouts, concurrency
 *
 * Demonstrates how to register task definitions with different configurations:
 * - td_critical_task: 5 retries with EXPONENTIAL_BACKOFF, 2s base delay, 300s timeout
 * - td_fast_task: 1 retry with FIXED delay, 1s delay, 10s timeout
 * - td_limited_task: 3 retries, concurrency limit of 3, rate limit 10 per 60s
 *
 * Then runs td_fast_task in a simple workflow to verify.
 *
 * Run:
 *   java -jar target/task-definitions-1.0.0.jar
 *   java -jar target/task-definitions-1.0.0.jar --workers
 */
public class TaskDefinitionsExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Task Definitions Demo: Configure Retries, Timeouts, Concurrency ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register 3 task definitions with different configs
        System.out.println("Step 1: Registering task definitions...");

        // td_critical_task — high retry count with exponential backoff
        TaskDef criticalTask = new TaskDef();
        criticalTask.setName("td_critical_task");
        criticalTask.setRetryCount(5);
        criticalTask.setRetryLogic(TaskDef.RetryLogic.EXPONENTIAL_BACKOFF);
        criticalTask.setRetryDelaySeconds(2);
        criticalTask.setTimeoutSeconds(300);
        criticalTask.setResponseTimeoutSeconds(60);
        criticalTask.setOwnerEmail("examples@orkes.io");

        // td_fast_task — minimal retries, tight timeouts
        TaskDef fastTask = new TaskDef();
        fastTask.setName("td_fast_task");
        fastTask.setRetryCount(1);
        fastTask.setRetryLogic(TaskDef.RetryLogic.FIXED);
        fastTask.setRetryDelaySeconds(1);
        fastTask.setTimeoutSeconds(10);
        fastTask.setResponseTimeoutSeconds(5);
        fastTask.setOwnerEmail("examples@orkes.io");

        // td_limited_task — concurrency and rate limiting
        TaskDef limitedTask = new TaskDef();
        limitedTask.setName("td_limited_task");
        limitedTask.setRetryCount(3);
        limitedTask.setTimeoutSeconds(60);
        limitedTask.setResponseTimeoutSeconds(30);
        limitedTask.setConcurrentExecLimit(3);
        limitedTask.setRateLimitPerFrequency(10);
        limitedTask.setRateLimitFrequencyInSeconds(60);
        limitedTask.setOwnerEmail("examples@orkes.io");

        client.registerTaskDefs(List.of(criticalTask, fastTask, limitedTask));

        System.out.println("\n  Registered 3 task definitions:\n");
        System.out.println("  td_critical_task:");
        System.out.println("    Retries: 5 (EXPONENTIAL_BACKOFF, 2s base delay)");
        System.out.println("    Timeout: 300s total, 60s response");
        System.out.println("");
        System.out.println("  td_fast_task:");
        System.out.println("    Retries: 1 (FIXED, 1s delay)");
        System.out.println("    Timeout: 10s total, 5s response");
        System.out.println("");
        System.out.println("  td_limited_task:");
        System.out.println("    Retries: 3");
        System.out.println("    Timeout: 60s total, 30s response");
        System.out.println("    Concurrent limit: 3");
        System.out.println("    Rate limit: 10 per 60s");
        System.out.println("");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'task_def_test'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new FastTaskWorker());
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("task_def_test", 1, Map.of());
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
