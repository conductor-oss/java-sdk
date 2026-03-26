package disasterrecovery;

import com.netflix.conductor.client.worker.Worker;
import disasterrecovery.workers.DetectWorker;
import disasterrecovery.workers.FailoverDbWorker;
import disasterrecovery.workers.UpdateDnsWorker;
import disasterrecovery.workers.VerifyWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 465: Disaster Recovery — Automated DR Failover
 *
 * Pattern: detect -> failoverdb -> updatedns -> verify
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 465: Disaster Recovery ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "dr_detect",
                "dr_failover_db",
                "dr_update_dns",
                "dr_verify"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new DetectWorker(),
                new FailoverDbWorker(),
                new UpdateDnsWorker(),
                new VerifyWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("disaster_recovery_workflow", 1, Map.of(
                "primaryRegion", "us-east-1",
                "drRegion", "us-west-2"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  recovered: " + execution.getOutput().get("recovered"));
        System.out.println("  rtoMinutes: " + execution.getOutput().get("rtoMinutes"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
