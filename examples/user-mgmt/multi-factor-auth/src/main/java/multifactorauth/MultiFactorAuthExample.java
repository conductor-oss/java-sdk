package multifactorauth;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import multifactorauth.workers.*;
import java.util.List;
import java.util.Map;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MultiFactorAuthExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 611: Multi-Factor Authentication ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("mfa_primary_auth", "mfa_select_method", "mfa_verify_factor", "mfa_grant"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new PrimaryAuthWorker(), new SelectMethodWorker(), new VerifyFactorWorker(), new GrantAccessWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        
        // Store secret via Conductor Secrets API
        String conductorUrl = System.getenv().getOrDefault("CONDUCTOR_BASE_URL", "http://localhost:8080/api");
        HttpClient http = HttpClient.newHttpClient();
        http.send(HttpRequest.newBuilder()
                .uri(URI.create(conductorUrl + "/secrets/mfa_password"))
                .PUT(HttpRequest.BodyPublishers.ofString("\"Mfa!Pass#2026\""))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        System.out.println("  Secret \'mfa_password\' stored via Conductor Secrets API");

        String workflowId = client.startWorkflow("mfa_multi_factor_auth", 1,
                Map.of("username", "frank_secure", "preferredMethod", "totp"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + workflow.getOutput());

        client.stopWorkers();
        System.out.println("\nResult: " + ("COMPLETED".equals(status) ? "PASSED" : "FAILED"));
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
