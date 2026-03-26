package regulatoryfiling;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import regulatoryfiling.workers.*;
import java.util.List;
import java.util.Map;

public class RegulatoryFilingExample {
    private static final String WORKFLOW_NAME = "rgf_regulatory_filing";

    public static void main(String[] args) throws Exception {
        ConductorClientHelper helper = new ConductorClientHelper();
        List<Worker> workers = List.of(
            new PrepareWorker(), new ValidateWorker(), new SubmitWorker(),
            new TrackWorker(), new ConfirmWorker()
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
        Map<String, Object> input = Map.of("filingType", "SEC-10K", "entityName", "Acme Corp", "jurisdiction", "US-Federal");
        String workflowId = helper.startWorkflow(WORKFLOW_NAME, 1, input);
        System.out.println("Started workflow: " + workflowId);
        Workflow result = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("Status: " + result.getStatus());
        System.out.println("Output: " + result.getOutput());
        helper.stopWorkers();
    }
}
