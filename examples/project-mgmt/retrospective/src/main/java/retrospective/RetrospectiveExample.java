package retrospective;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import retrospective.workers.*;

import java.util.List;
import java.util.Map;

public class RetrospectiveExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example retrospective: Retrospective ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("rsp_collect_feedback", "rsp_categorize", "rsp_prioritize", "rsp_action_items"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CollectFeedbackWorker(), new CategorizeWorker(), new PrioritizeWorker(), new ActionItemsWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("retrospective_retrospective", 1, Map.of("sprintId", "SPR-42", "teamName", "Platform Engineering", "facilitator", "Scrum Master"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
