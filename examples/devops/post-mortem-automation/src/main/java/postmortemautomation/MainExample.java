package postmortemautomation;

import com.netflix.conductor.client.worker.Worker;
import postmortemautomation.workers.GatherTimelineWorker;
import postmortemautomation.workers.CollectMetricsWorker;
import postmortemautomation.workers.DraftDocumentWorker;
import postmortemautomation.workers.ScheduleReviewWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 337: Post-Mortem Automation — Incident Review Orchestration
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 337: Post-Mortem Automation ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "pm_gather_timeline",
                "pm_collect_metrics",
                "pm_draft_document",
                "pm_schedule_review"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new GatherTimelineWorker(),
                new CollectMetricsWorker(),
                new DraftDocumentWorker(),
                new ScheduleReviewWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("post_mortem_workflow", 1, Map.of(
                "incidentId", "INC-2024-042",
                "severity", "P1"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  gather_timelineResult: " + execution.getOutput().get("gather_timelineResult"));
        System.out.println("  schedule_reviewResult: " + execution.getOutput().get("schedule_reviewResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
