package releasemanagement;

import com.netflix.conductor.client.worker.Worker;
import releasemanagement.workers.PrepareWorker;
import releasemanagement.workers.ApproveWorker;
import releasemanagement.workers.DeployWorker;
import releasemanagement.workers.AnnounceWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 500: Release Management — Coordinated Software Release Pipeline
 *
 * Pattern: prepare -> approve -> deploy -> announce
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 500: Release Management ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "rm_prepare",
                "rm_approve",
                "rm_deploy",
                "rm_announce"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new PrepareWorker(),
                new ApproveWorker(),
                new DeployWorker(),
                new AnnounceWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("release_management_workflow", 1, Map.of(
                "version", "3.2.0",
                "product", "platform"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  prepareResult: " + execution.getOutput().get("prepareResult"));
        System.out.println("  announceResult: " + execution.getOutput().get("announceResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
