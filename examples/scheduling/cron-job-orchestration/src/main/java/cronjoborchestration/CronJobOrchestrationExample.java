package cronjoborchestration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import cronjoborchestration.workers.ScheduleJobWorker;
import cronjoborchestration.workers.ExecuteJobWorker;
import cronjoborchestration.workers.LogResultWorker;
import cronjoborchestration.workers.CleanupWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 401: Cron Job Orchestration
 *
 * Orchestrate cron jobs: schedule the job, execute it, log the result,
 * and clean up temporary resources.
 *
 * Pattern:
 *   schedule -> execute -> log -> cleanup
 *
 * Run:
 *   java -jar target/cron-job-orchestration-1.0.0.jar
 */
public class CronJobOrchestrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 401: Cron Job Orchestration ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "cj_schedule_job", "cj_execute_job", "cj_log_result", "cj_cleanup"));
        System.out.println("  Registered: cj_schedule_job, cj_execute_job, cj_log_result, cj_cleanup\n");

        System.out.println("Step 2: Registering workflow 'cron_job_orchestration_401'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ScheduleJobWorker(),
                new ExecuteJobWorker(),
                new LogResultWorker(),
                new CleanupWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("cron_job_orchestration_401", 1,
                Map.of("jobName", "nightly-data-export",
                        "cronExpression", "0 2 * * *",
                        "command", "python export_data.py --format=csv"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

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
