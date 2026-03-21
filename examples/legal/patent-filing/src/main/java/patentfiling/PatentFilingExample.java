package patentfiling;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import patentfiling.workers.*;
import java.util.List;
import java.util.Map;

public class PatentFilingExample {
    private static final String WORKFLOW_NAME = "ptf_patent_filing";

    public static void main(String[] args) throws Exception {
        ConductorClientHelper helper = new ConductorClientHelper();
        List<Worker> workers = List.of(
            new DraftWorker(), new ReviewWorker(), new PriorArtWorker(),
            new FileWorker(), new TrackWorker()
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
        Map<String, Object> input = Map.of("inventionTitle", "Novel Widget Mechanism", "inventors", List.of("Alice Engineer"), "description", "A new mechanism for widget assembly");
        String workflowId = helper.startWorkflow(WORKFLOW_NAME, 1, input);
        System.out.println("Started workflow: " + workflowId);
        Workflow result = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("Status: " + result.getStatus());
        System.out.println("Output: " + result.getOutput());
        helper.stopWorkers();
    }
}
