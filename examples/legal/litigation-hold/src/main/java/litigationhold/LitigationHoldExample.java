package litigationhold;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import litigationhold.workers.*;
import java.util.List;
import java.util.Map;

public class LitigationHoldExample {
    private static final String WORKFLOW_NAME = "lth_litigation_hold";

    public static void main(String[] args) throws Exception {
        ConductorClientHelper helper = new ConductorClientHelper();
        List<Worker> workers = List.of(
            new IdentifyWorker(), new NotifyWorker(), new CollectWorker(),
            new PreserveWorker(), new TrackWorker()
        );
        if (args.length > 0 && "--workers".equals(args[0])) {
            helper.startWorkers(workers);
            System.out.println("Workers running. Press Enter to quit.");
            System.in.read();
            helper.stopWorkers();
            return;
        }
        helper.registerWorkflow("workflow.json");
        helper.startWorkers(workers);
        Map<String, Object> input = Map.of("caseId", "CASE-2024-001", "custodians", List.of("John Doe", "Jane Smith"));
        String workflowId = helper.startWorkflow(WORKFLOW_NAME, 1, input);
        System.out.println("Started workflow: " + workflowId);
        Workflow result = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("Status: " + result.getStatus());
        System.out.println("Output: " + result.getOutput());
        helper.stopWorkers();
    }
}
