package webinarregistration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import webinarregistration.workers.*;
import java.util.List;
import java.util.Map;

public class WebinarRegistrationExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 626: Webinar Registration ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("wbr_register", "wbr_confirm", "wbr_remind", "wbr_followup"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new RegisterWorker(), new ConfirmWorker(), new RemindWorker(), new FollowupWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("wbr_webinar_registration", 1,
                Map.of("attendeeName", "Jack Torres", "email", "jack@company.com", "webinarId", "WEB-AI-2024"));
        System.out.println("  Workflow ID: " + wfId);
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
