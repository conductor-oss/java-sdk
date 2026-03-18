package runbookautomation;

import com.netflix.conductor.client.worker.Worker;
import runbookautomation.workers.LoadRunbookWorker;
import runbookautomation.workers.ExecuteStepWorker;
import runbookautomation.workers.VerifyStepWorker;
import runbookautomation.workers.LogOutcomeWorker;

import java.util.List;
import java.util.Map;

/**
 * Example runbook-automation: Runbook Automation — Codifying Operational Procedures
 *
 * Pattern: loadrunbook -> executestep -> verifystep -> logoutcome
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example runbook-automation: Runbook Automation ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "ra_load_runbook",
                "ra_execute_step",
                "ra_verify_step",
                "ra_log_outcome"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new LoadRunbookWorker(),
                new ExecuteStepWorker(),
                new VerifyStepWorker(),
                new LogOutcomeWorker()
        );
        helper.startWorkers(workers);


        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        String workflowId = helper.startWorkflow("runbook_automation_workflow", 1, Map.of(
                "runbookName", "database-failover",
                "trigger", "manual"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  runbookId: " + execution.getOutput().get("runbookId"));
        System.out.println("  outcome: " + execution.getOutput().get("outcome"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
