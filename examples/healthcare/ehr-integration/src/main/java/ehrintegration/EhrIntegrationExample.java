package ehrintegration;

import com.netflix.conductor.client.worker.Worker;
import ehrintegration.workers.*;

import java.util.List;
import java.util.Map;

public class EhrIntegrationExample {

    private static final List<Worker> WORKERS = List.of(
            new QueryPatientWorker(),
            new MergeRecordsWorker(),
            new ValidateRecordWorker(),
            new UpdateRecordWorker()
    );

    public static void main(String[] args) throws Exception {
        System.out.println("=== EHR Integration Workflow ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();

        if (args.length > 0 && "--workers".equals(args[0])) {
            System.out.println("Starting workers only…");
            helper.startWorkers(WORKERS);
            Thread.currentThread().join();
            return;
        }

        helper.registerTaskDefs(List.of("ehr_query_patient", "ehr_merge_records", "ehr_validate", "ehr_update"));
        helper.registerWorkflow("workflow.json");
        helper.startWorkers(WORKERS);

        String workflowId = helper.startWorkflow("ehr_integration_workflow", 1, Map.of(
                "patientId", "PAT-10234",
                "sourceSystem", "all"
        ));

        var result = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\nWorkflow status: " + result.getStatus());
        System.out.println("Records merged: " + result.getOutput().get("recordsMerged"));
        System.out.println("Validation: " + result.getOutput().get("validationPassed"));
        System.out.println("Updated: " + result.getOutput().get("updated"));
        System.out.println("\nResult: PASSED");
        helper.stopWorkers();
    }
}
