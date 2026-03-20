package publicrecords;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import publicrecords.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 528: Public Records — Request, Search, Verify, Redact, Release
 *
 * Performs a public records request workflow with document redaction.
 */
public class PublicRecordsExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 528: Public Records ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of("pbr_request", "pbr_search", "pbr_verify", "pbr_redact", "pbr_release"));
        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new RequestWorker(),
                new SearchWorker(),
                new VerifyWorker(),
                new RedactWorker(),
                new ReleaseWorker()
        );
        helper.startWorkers(workers);

        try {
            String workflowId = helper.startWorkflow("pbr_public_records", 1, Map.of(
                    "requesterId", "MEDIA-01",
                    "recordType", "budget",
                    "query", "2024 municipal spending"
            ));

            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("Documents released: %s%n", workflow.getOutput().get("documentsReleased"));

            System.out.println("\nResult: PASSED");
        } finally {
            helper.stopWorkers();
        }
    }
}
