package dependencyupdate;

import com.netflix.conductor.client.worker.Worker;
import dependencyupdate.workers.ScanOutdatedWorker;
import dependencyupdate.workers.UpdateDepsWorker;
import dependencyupdate.workers.RunTestsWorker;
import dependencyupdate.workers.CreatePrWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 334: Dependency Update — Automated Dependency Upgrade Pipeline
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 334: Dependency Update ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "du_scan_outdated",
                "du_update_deps",
                "du_run_tests",
                "du_create_pr"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new ScanOutdatedWorker(),
                new UpdateDepsWorker(),
                new RunTestsWorker(),
                new CreatePrWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("dependency_update_workflow", 1, Map.of(
                "repository", "auth-service",
                "updateType", "all"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  scan_outdatedResult: " + execution.getOutput().get("scan_outdatedResult"));
        System.out.println("  create_prResult: " + execution.getOutput().get("create_prResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
