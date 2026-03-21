package configmanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import configmanagement.workers.*;
import java.util.List;
import java.util.Map;

public class ConfigManagementExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 299: Config Management ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cf_load_config", "cf_validate_config", "cf_deploy_config", "cf_verify_config"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new LoadConfigWorker(), new ValidateConfigWorker(), new DeployConfigWorker(), new VerifyConfigWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("config_management_299", 1,
                Map.of("configSource", "consul", "environment", "production"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
