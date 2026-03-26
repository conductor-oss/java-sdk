package smoketesting;

import com.netflix.conductor.client.worker.Worker;
import smoketesting.workers.CheckEndpointsWorker;
import smoketesting.workers.VerifyDataWorker;
import smoketesting.workers.TestIntegrationsWorker;
import smoketesting.workers.ReportStatusWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 341: Smoke Testing — Post-Deployment Validation Pipeline
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 341: Smoke Testing ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "st_check_endpoints",
                "st_verify_data",
                "st_test_integrations",
                "st_report_status"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new CheckEndpointsWorker(),
                new VerifyDataWorker(),
                new TestIntegrationsWorker(),
                new ReportStatusWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("smoke_testing_workflow", 1, Map.of(
                "service", "order-service",
                "environment", "production"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  check_endpointsResult: " + execution.getOutput().get("check_endpointsResult"));
        System.out.println("  report_statusResult: " + execution.getOutput().get("report_statusResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
