package canarydeployment;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import canarydeployment.workers.*;

import java.util.List;
import java.util.Map;

public class CanaryDeploymentExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Canary Deployment Demo ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cd_deploy_canary", "cd_shift_traffic", "cd_analyze_metrics", "cd_promote_or_rollback"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new DeployCanaryWorker(), new ShiftTrafficWorker(), new AnalyzeMetricsWorker(), new PromoteOrRollbackWorker());
        client.startWorkers(workers);
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("canary_deployment_workflow", 1, Map.of("serviceName", "user-service", "newVersion", "4.1.0", "canaryPercentage", 10));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        client.stopWorkers();
    }
}
