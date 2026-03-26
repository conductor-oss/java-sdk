package authorizationrbac;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import authorizationrbac.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Authorization RBAC Demo
 *
 * Role-based access control orchestration:
 *   rbac_resolve_roles -> rbac_evaluate_permissions -> rbac_check_context -> rbac_enforce_decision
 */
public class AuthorizationRbacExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Authorization RBAC Demo ===\n");
        var client = new ConductorClientHelper();

        client.registerTaskDefs(List.of(
                "rbac_resolve_roles", "rbac_evaluate_permissions",
                "rbac_check_context", "rbac_enforce_decision"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new ResolveRolesWorker(), new EvaluatePermissionsWorker(),
                new CheckContextWorker(), new EnforceDecisionWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String workflowId = client.startWorkflow("authorization_rbac_workflow", 1,
                Map.of("userId", "user-001", "resource", "billing/invoices", "action", "delete"));
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        client.stopWorkers();
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
