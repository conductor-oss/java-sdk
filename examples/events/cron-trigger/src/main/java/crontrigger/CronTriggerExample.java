package crontrigger;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import crontrigger.workers.CheckScheduleWorker;
import crontrigger.workers.ExecuteTasksWorker;
import crontrigger.workers.RecordRunWorker;
import crontrigger.workers.LogSkipWorker;

import java.util.List;
import java.util.Map;

/**
 * Cron Trigger Demo
 *
 * Demonstrates a SWITCH-based cron trigger workflow:
 *   cn_check_schedule -> SWITCH(shouldRun:
 *       yes -> cn_execute_tasks -> cn_record_run,
 *       no  -> cn_log_skip)
 *
 * Run:
 *   java -jar target/cron-trigger-1.0.0.jar
 */
public class CronTriggerExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Cron Trigger Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "cn_check_schedule", "cn_execute_tasks",
                "cn_record_run", "cn_log_skip"));
        System.out.println("  Registered: cn_check_schedule, cn_execute_tasks, cn_record_run, cn_log_skip\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'cron_trigger'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new CheckScheduleWorker(),
                new ExecuteTasksWorker(),
                new RecordRunWorker(),
                new LogSkipWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("cron_trigger", 1,
                Map.of("cronExpression", "0 */5 * * *",
                        "jobName", "daily-report-generator"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
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
