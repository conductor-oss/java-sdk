package backendforfrontend;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import backendforfrontend.workers.*;
import java.util.List;
import java.util.Map;

public class BackendForFrontendExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 306: Backend for Frontend ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("bff_fetch_data", "bff_transform_web", "bff_transform_mobile"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new FetchDataWorker(), new TransformWebWorker(), new TransformMobileWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("bff_workflow", 1, Map.of("userId", "user-1", "platform", "mobile"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
