package portfoliorebalancing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import portfoliorebalancing.workers.*;

import java.util.List;
import java.util.Map;

public class PortfolioRebalancingExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 496: Portfolio Rebalancing ===\n");
        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("prt_analyze_drift", "prt_determine_trades", "prt_execute_trades", "prt_verify", "prt_report"));
        System.out.println("  Registered 5 task definitions.\n");

        System.out.println("Step 2: Registering workflow...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new AnalyzeDriftWorker(), new DetermineTradesWorker(), new ExecuteTradesWorker(), new VerifyWorker(), new ReportWorker());
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) { System.out.println("Worker-only mode. Ctrl+C to stop."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String wfId = client.startWorkflow("portfolio_rebalancing_workflow", 1, Map.of("portfolioId", "PORT-INST-001", "accountId", "ACCT-FIN-5501", "strategy", "60_20_15_5"));
        System.out.println("  Workflow ID: " + wfId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
