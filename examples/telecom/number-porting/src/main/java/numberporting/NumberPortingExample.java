package numberporting;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import numberporting.workers.RequestWorker;
import numberporting.workers.ValidateWorker;
import numberporting.workers.CoordinateWorker;
import numberporting.workers.PortWorker;
import numberporting.workers.VerifyWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 813: Number Porting
 */
public class NumberPortingExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 813: Number Porting ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("npt_request", "npt_validate", "npt_coordinate", "npt_port", "npt_verify"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new RequestWorker(), new ValidateWorker(), new CoordinateWorker(), new PortWorker(), new VerifyWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("npt_number_porting", 1, Map.of("phoneNumber", "+1-555-0172", "fromCarrier", "CarrierA", "toCarrier", "CarrierB"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("ported: %s%n", workflow.getOutput().get("ported"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
