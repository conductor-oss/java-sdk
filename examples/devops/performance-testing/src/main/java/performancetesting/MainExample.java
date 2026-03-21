package performancetesting;

import com.netflix.conductor.client.worker.Worker;
import performancetesting.workers.PrepareEnvWorker;
import performancetesting.workers.RunLoadTestWorker;
import performancetesting.workers.AnalyzeResultsWorker;
import performancetesting.workers.GenerateReportWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 430: Performance Testing — Automated Load Testing Pipeline
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 430: Performance Testing ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "pt_prepare_env",
                "pt_run_load_test",
                "pt_analyze_results",
                "pt_generate_report"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new PrepareEnvWorker(),
                new RunLoadTestWorker(),
                new AnalyzeResultsWorker(),
                new GenerateReportWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("performance_testing_workflow", 1, Map.of(
                "service", "api-gateway",
                "targetRps", 1000,
                "duration", "5m"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  prepare_envResult: " + execution.getOutput().get("prepare_envResult"));
        System.out.println("  generate_reportResult: " + execution.getOutput().get("generate_reportResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
