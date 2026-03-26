package gdprconsent;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import gdprconsent.workers.PresentOptionsWorker;
import gdprconsent.workers.RecordConsentWorker;
import gdprconsent.workers.UpdateSystemsWorker;
import gdprconsent.workers.AuditWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 607: GDPR Consent — Present Options, Record Consent, Update Systems, Audit
 */
public class GdprConsentExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 607: GDPR Consent ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("gdc_present_options", "gdc_record_consent", "gdc_update_systems", "gdc_audit"));
        System.out.println("  Registered.\n");

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new PresentOptionsWorker(), new RecordConsentWorker(),
                new UpdateSystemsWorker(), new AuditWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) { System.out.println("Worker-only mode. Press Ctrl+C to stop.\n"); Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("gdc_gdpr_consent", 1,
                Map.of("userId", "USR-GDPR01",
                       "consents", Map.of("analytics", true, "marketing", false, "thirdParty", false, "cookies", true)));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();
        System.out.println("\nResult: " + ("COMPLETED".equals(status) ? "PASSED" : "FAILED"));
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
