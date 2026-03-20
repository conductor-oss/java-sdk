package inspectionworkflow;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import inspectionworkflow.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 526: Inspection Workflow — Schedule, Inspect, Document, Pass/Fail, Record
 *
 * Performs a government inspection with conditional pass/fail via SWITCH.
 */
public class InspectionWorkflowExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 526: Inspection Workflow ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of("inw_schedule", "inw_inspect", "inw_document", "inw_record_pass", "inw_record_fail"));
        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new ScheduleWorker(),
                new InspectWorker(),
                new DocumentWorker(),
                new RecordPassWorker(),
                new RecordFailWorker()
        );
        helper.startWorkers(workers);

        try {
            String workflowId = helper.startWorkflow("inw_inspection_workflow", 1, Map.of(
                    "propertyId", "PROP-500",
                    "inspectionType", "building-safety"
            ));

            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("Result: %s%n", workflow.getOutput().get("result"));

            System.out.println("\nResult: PASSED");
        } finally {
            helper.stopWorkers();
        }
    }
}
