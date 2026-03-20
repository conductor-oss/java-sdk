package servicemigration;

import com.netflix.conductor.client.worker.Worker;
import servicemigration.workers.AssessWorker;
import servicemigration.workers.ReplicateWorker;
import servicemigration.workers.CutoverWorker;
import servicemigration.workers.ValidateWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 338: Service Migration — Orchestrated Service Relocation
 *
 * Pattern: assess -> replicate -> cutover -> validate
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 338: Service Migration ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "sm_assess",
                "sm_replicate",
                "sm_cutover",
                "sm_validate"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new AssessWorker(),
                new ReplicateWorker(),
                new CutoverWorker(),
                new ValidateWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("service_migration_workflow", 1, Map.of(
                "service", "payment-service",
                "sourceEnv", "aws-east",
                "targetEnv", "aws-west"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  assessResult: " + execution.getOutput().get("assessResult"));
        System.out.println("  validateResult: " + execution.getOutput().get("validateResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
