package chaosengineering;

import com.netflix.conductor.client.worker.Worker;
import chaosengineering.workers.DefineExperimentWorker;
import chaosengineering.workers.InjectFailureWorker;
import chaosengineering.workers.ObserveWorker;
import chaosengineering.workers.RecoverWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 332: Chaos Engineering — Controlled Failure Injection Orchestration
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 332: Chaos Engineering ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "ce_define_experiment",
                "ce_inject_failure",
                "ce_observe",
                "ce_recover"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new DefineExperimentWorker(),
                new InjectFailureWorker(),
                new ObserveWorker(),
                new RecoverWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("chaos_engineering_workflow", 1, Map.of(
                "service", "payment-service",
                "faultType", "latency-injection"
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  define_experimentResult: " + execution.getOutput().get("define_experimentResult"));
        System.out.println("  recoverResult: " + execution.getOutput().get("recoverResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
