package foodsafety;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import foodsafety.workers.*;
import java.util.List;
import java.util.Map;

/**
 * Example 738: Food Safety — Inspect, Check Temps, Verify Hygiene, Certify, Record
 */
public class FoodSafetyExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 738: Food Safety ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("fsf_inspect", "fsf_check_temps", "fsf_verify_hygiene", "fsf_certify", "fsf_record"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new InspectWorker(), new CheckTempsWorker(), new VerifyHygieneWorker(), new CertifyWorker(), new RecordWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("food_safety_738", 1, Map.of("restaurantId", "REST-10", "inspectorId", "INS-007"));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
