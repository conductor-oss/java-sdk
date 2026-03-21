package leadnurturing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import leadnurturing.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 623: Lead Nurturing
 *
 * Demonstrates a lead nurturing workflow:
 *   nur_segment -> nur_personalize -> nur_send -> nur_track
 */
public class LeadNurturingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 623: Lead Nurturing ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("nur_segment", "nur_personalize", "nur_send", "nur_track"));
        System.out.println("  Registered 4 task definitions.\n");

        System.out.println("Step 2: Registering workflow 'nur_lead_nurturing'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new SegmentWorker(), new PersonalizeWorker(),
                new SendWorker(), new TrackWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("nur_lead_nurturing", 1,
                Map.of("leadId", "LEAD-NUR01", "leadStage", "consideration",
                        "interests", List.of("automation", "analytics")));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
