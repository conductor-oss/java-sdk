package secretsmanagement;

import com.netflix.conductor.client.worker.Worker;
import secretsmanagement.workers.CreateSecretWorker;
import secretsmanagement.workers.DistributeWorker;
import secretsmanagement.workers.VerifyAccessWorker;
import secretsmanagement.workers.ScheduleRotationWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 300: Secrets Management — Secure Secret Lifecycle Orchestration
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 300: Secrets Management ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "sm_create_secret",
                "sm_distribute",
                "sm_verify_access",
                "sm_schedule_rotation"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new CreateSecretWorker(),
                new DistributeWorker(),
                new VerifyAccessWorker(),
                new ScheduleRotationWorker()
        );
        helper.startWorkers(workers);


        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        String workflowId = helper.startWorkflow("secrets_management_workflow", 1, Map.of(
                "secretName", "db-prod-password",
                "secretType", "database"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  create_secretResult: " + execution.getOutput().get("create_secretResult"));
        System.out.println("  schedule_rotationResult: " + execution.getOutput().get("schedule_rotationResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
