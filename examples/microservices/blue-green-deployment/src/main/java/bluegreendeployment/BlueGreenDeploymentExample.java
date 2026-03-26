package bluegreendeployment;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import bluegreendeployment.workers.DeployGreenWorker;
import bluegreendeployment.workers.ValidateGreenWorker;
import bluegreendeployment.workers.SwitchTrafficWorker;
import bluegreendeployment.workers.MonitorGreenWorker;

import java.util.List;
import java.util.Map;

public class BlueGreenDeploymentExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Blue-Green Deployment Demo ===\n");
        var client = new ConductorClientHelper();

        client.registerTaskDefs(List.of("bg_deploy_green", "bg_validate_green", "bg_switch_traffic", "bg_monitor_green"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new DeployGreenWorker(), new ValidateGreenWorker(),
                new SwitchTrafficWorker(), new MonitorGreenWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("blue_green_deployment", 1,
                Map.of("serviceName", "api-gateway", "newVersion", "3.2.0", "imageTag", "api-gateway:3.2.0"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        client.stopWorkers();
    }
}
