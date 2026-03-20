package tradeexecution;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import tradeexecution.workers.ValidateOrderWorker;
import tradeexecution.workers.CheckComplianceWorker;
import tradeexecution.workers.RouteWorker;
import tradeexecution.workers.ExecuteWorker;
import tradeexecution.workers.ConfirmWorker;

import java.util.List;
import java.util.Map;

/**
 * Trade Execution Workflow Demo
 *
 * Demonstrates a sequential trade pipeline:
 *   trd_validate_order -> trd_check_compliance -> trd_route -> trd_execute -> trd_confirm
 *
 * Run:
 *   java -jar target/trade-execution-1.0.0.jar
 */
public class TradeExecutionExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 494: Trade Execution ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "trd_validate_order", "trd_check_compliance",
                "trd_route", "trd_execute", "trd_confirm"));
        System.out.println("  Registered 5 task definitions.\n");

        System.out.println("Step 2: Registering workflow 'trade_execution_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ValidateOrderWorker(),
                new CheckComplianceWorker(),
                new RouteWorker(),
                new ExecuteWorker(),
                new ConfirmWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("trade_execution_workflow", 1,
                Map.of("orderId", "ORD-TRD-2024-001",
                       "accountId", "ACCT-FIN-5501",
                       "symbol", "AAPL",
                       "side", "BUY",
                       "quantity", 100,
                       "orderType", "market"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
