package taxassessment;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import taxassessment.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 901: Tax Assessment — Collect Data, Assess Property, Calculate, Notify, Appeal
 *
 * Performs a property tax assessment workflow from data collection to appeal.
 */
public class TaxAssessmentExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 901: Tax Assessment ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of("txa_collect_data", "txa_assess_property", "txa_calculate", "txa_notify", "txa_appeal"));
        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new CollectDataWorker(),
                new AssessPropertyWorker(),
                new CalculateWorker(),
                new NotifyWorker(),
                new AppealWorker()
        );
        helper.startWorkers(workers);

        try {
            String workflowId = helper.startWorkflow("txa_tax_assessment", 1, Map.of(
                    "propertyId", "PROP-901",
                    "taxYear", "2024",
                    "ownerId", "OWN-55"
            ));

            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("Tax amount: %s%n", workflow.getOutput().get("taxAmount"));

            System.out.println("\nResult: PASSED");
        } finally {
            helper.stopWorkers();
        }
    }
}
