package npsscoring;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import npsscoring.workers.ActWorker;
import npsscoring.workers.CalculateNpsWorker;
import npsscoring.workers.CollectResponsesWorker;
import npsscoring.workers.SegmentWorker;

import java.util.List;
import java.util.Map;

public class NpsScoringExample {

    public static void main(String[] args) throws Exception {
        ConductorClientHelper conductor = new ConductorClientHelper();

        // 1. Register task definitions
        conductor.registerTaskDefs(List.of(
                "nps_collect_responses", "nps_calculate", "nps_segment", "nps_act"
        ));
        System.out.println("Task definitions registered.");

        // 2. Register workflow
        conductor.registerWorkflow("workflow.json");
        System.out.println("Workflow 'nps_scoring' registered.");

        // 3. Start workers
        List<Worker> workers = List.of(
                new CollectResponsesWorker(),
                new CalculateNpsWorker(),
                new SegmentWorker(),
                new ActWorker()
        );
        conductor.startWorkers(workers);
        System.out.println("Workers started.");

        // 4. Start workflow
        String workflowId = conductor.startWorkflow("nps_scoring", 1, Map.of(
                "campaignId", "NPS-2024-Q4",
                "period", "2024-Q4"
        ));
        System.out.println("Workflow started: " + workflowId);

        // 5. Wait for completion
        Workflow workflow = conductor.waitForWorkflow(workflowId, "COMPLETED", 30_000);
        System.out.println("Workflow status: " + workflow.getStatus());
        System.out.println("NPS score: " + workflow.getOutput().get("npsScore"));
        System.out.println("Total responses: " + workflow.getOutput().get("totalResponses"));
        System.out.println("Actions triggered: " + workflow.getOutput().get("actionsTriggered"));

        // 6. Cleanup
        conductor.stopWorkers();
        System.out.println("Done.");
    }
}
