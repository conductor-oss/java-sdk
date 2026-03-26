package deploymentai;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import deploymentai.workers.*;
import java.util.List;
import java.util.Map;
public class DeploymentAiExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 648: Deployment AI ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("dai_analyze_changes", "dai_predict_risk", "dai_recommend_strategy", "dai_execute_deploy"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new AnalyzeChangesWorker(), new PredictRiskWorker(), new RecommendStrategyWorker(), new ExecuteDeployWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("dai_deployment_ai", 1,
                Map.of("serviceName", "payment-service", "version", "3.2.1", "environment", "production"));
        System.out.println("  Workflow ID: " + wfId);
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
