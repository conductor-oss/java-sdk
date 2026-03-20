package agencymanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import agencymanagement.workers.OnboardWorker;
import agencymanagement.workers.LicenseWorker;
import agencymanagement.workers.AssignTerritoryWorker;
import agencymanagement.workers.TrackWorker;
import agencymanagement.workers.ReviewWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 702: Agency Management
 */
public class AgencyManagementExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 702: Agency Management ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("agm_onboard", "agm_license", "agm_assign_territory", "agm_track", "agm_review"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new OnboardWorker(), new LicenseWorker(), new AssignTerritoryWorker(), new TrackWorker(), new ReviewWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("agm_agency_management", 1, Map.of("agentId", "AGT-702", "agentName", "Sarah Chen", "state", "CA"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("rating: %s%n", workflow.getOutput().get("rating"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
