package citizenrequest;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import citizenrequest.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 523: Citizen Request — Submit, Classify, Route, Resolve, Notify
 *
 * Performs a citizen service request workflow from submission to resolution.
 */
public class CitizenRequestExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 523: Citizen Request ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of("ctz_submit", "ctz_classify", "ctz_route", "ctz_resolve", "ctz_notify"));
        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new SubmitWorker(),
                new ClassifyWorker(),
                new RouteWorker(),
                new ResolveWorker(),
                new NotifyWorker()
        );
        helper.startWorkers(workers);

        try {
            String workflowId = helper.startWorkflow("ctz_citizen_request", 1, Map.of(
                    "citizenId", "CIT-200",
                    "requestType", "pothole",
                    "description", "Large pothole on Main Street"
            ));

            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("Resolution: %s%n", workflow.getOutput().get("resolution"));

            System.out.println("\nResult: PASSED");
        } finally {
            helper.stopWorkers();
        }
    }
}
