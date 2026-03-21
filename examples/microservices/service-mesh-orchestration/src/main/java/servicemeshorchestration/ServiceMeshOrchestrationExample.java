package servicemeshorchestration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import servicemeshorchestration.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Service Mesh Orchestration Demo
 *
 * Deploys sidecar proxies, configures mTLS, sets traffic policies, and validates connectivity.
 */
public class ServiceMeshOrchestrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Service Mesh Orchestration Demo ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("mesh_deploy_sidecar", "mesh_configure_mtls", "mesh_set_traffic_policy", "mesh_validate"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new DeploySidecarWorker(), new ConfigureMtlsWorker(),
                new SetTrafficPolicyWorker(), new ValidateWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String workflowId = client.startWorkflow("service_mesh_orchestration", 1,
                Map.of("serviceName", "payment-service", "namespace", "production", "meshType", "istio"));

        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        client.stopWorkers();
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
