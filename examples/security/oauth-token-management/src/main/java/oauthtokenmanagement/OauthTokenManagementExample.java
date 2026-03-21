package oauthtokenmanagement;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import oauthtokenmanagement.workers.*;

import java.util.List;
import java.util.Map;

/**
 * OAuth Token Management Demo
 *
 * Token lifecycle orchestration:
 *   otm_validate_grant -> otm_issue_tokens -> otm_store_token -> otm_audit_log
 */
public class OauthTokenManagementExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== OAuth Token Management Demo ===\n");
        var client = new ConductorClientHelper();

        client.registerTaskDefs(List.of(
                "otm_validate_grant", "otm_issue_tokens",
                "otm_store_token", "otm_audit_log"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new ValidateGrantWorker(), new IssueTokensWorker(),
                new StoreTokenWorker(), new AuditLogWorker());
        client.startWorkers(workers);

        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String workflowId = client.startWorkflow("oauth_token_management_workflow", 1,
                Map.of("clientId", "app-dashboard", "grantType", "authorization_code", "scope", "read:users write:reports"));
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        client.stopWorkers();
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
