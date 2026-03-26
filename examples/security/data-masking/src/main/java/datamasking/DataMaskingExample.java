package datamasking;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import datamasking.workers.DmIdentifyFieldsWorker;
import datamasking.workers.DmSelectStrategyWorker;
import datamasking.workers.DmApplyMaskingWorker;
import datamasking.workers.DmValidateOutputWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 392: Data Masking -- Dynamic Data Masking Orchestration
 *
 * Pattern:
 *   identify-fields -> select-strategy -> apply-masking -> validate-output
 */
public class DataMaskingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 392: Data Masking ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "dm_identify_fields", "dm_select_strategy", "dm_apply_masking", "dm_validate_output"));

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new DmIdentifyFieldsWorker(),
                new DmSelectStrategyWorker(),
                new DmApplyMaskingWorker(),
                new DmValidateOutputWorker()
        );
        client.startWorkers(workers);

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("data_masking_workflow", 1,
                Map.of("dataSource", "customer-database", "purpose", "analytics"));

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
