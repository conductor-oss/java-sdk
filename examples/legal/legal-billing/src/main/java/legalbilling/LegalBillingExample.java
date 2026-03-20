package legalbilling;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import legalbilling.workers.*;
import java.util.List;
import java.util.Map;

public class LegalBillingExample {
    private static final String WORKFLOW_NAME = "lgb_legal_billing";

    public static void main(String[] args) throws Exception {
        ConductorClientHelper helper = new ConductorClientHelper();
        List<Worker> workers = List.of(
            new TrackTimeWorker(), new ReviewWorker(), new GenerateWorker(),
            new SendWorker(), new CollectWorker()
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
        Map<String, Object> input = Map.of("clientId", "CLT-100", "matterId", "MAT-2024-050", "billingPeriod", "2024-Q1");
        String workflowId = helper.startWorkflow(WORKFLOW_NAME, 1, input);
        System.out.println("Started workflow: " + workflowId);
        Workflow result = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("Status: " + result.getStatus());
        System.out.println("Output: " + result.getOutput());
        helper.stopWorkers();
    }
}
