package auditlogging;

import com.netflix.conductor.client.worker.Worker;
import auditlogging.workers.CaptureEventWorker;
import auditlogging.workers.EnrichContextWorker;
import auditlogging.workers.StoreImmutableWorker;
import auditlogging.workers.VerifyIntegrityWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 481: Audit Logging — Tamper-Proof Audit Trail Orchestration
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 481: Audit Logging ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "al_capture_event",
                "al_enrich_context",
                "al_store_immutable",
                "al_verify_integrity"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new CaptureEventWorker(),
                new EnrichContextWorker(),
                new StoreImmutableWorker(),
                new VerifyIntegrityWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("audit_logging_workflow", 1, Map.of(
                "actor", "admin@example.com",
                "action", "delete",
                "resource", "user/12345"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  capture_eventResult: " + execution.getOutput().get("capture_eventResult"));
        System.out.println("  verify_integrityResult: " + execution.getOutput().get("verify_integrityResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
