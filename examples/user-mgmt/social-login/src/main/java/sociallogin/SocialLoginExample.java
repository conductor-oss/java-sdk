package sociallogin;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import sociallogin.workers.*;
import java.util.List;
import java.util.Map;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class SocialLoginExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 612: Social Login ===\n");

        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("slo_detect_provider", "slo_auth", "slo_link_account", "slo_session"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new DetectProviderWorker(), new OAuthWorker(), new LinkAccountWorker(), new CreateSessionWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        
        // Store secret via Conductor Secrets API
        String conductorUrl = System.getenv().getOrDefault("CONDUCTOR_BASE_URL", "http://localhost:8080/api");
        HttpClient http = HttpClient.newHttpClient();
        http.send(HttpRequest.newBuilder()
                .uri(URI.create(conductorUrl + "/secrets/oauth_token"))
                .PUT(HttpRequest.BodyPublishers.ofString("\"oauth-test-token-2026\""))
                .header("Content-Type", "application/json")
                .build(), HttpResponse.BodyHandlers.ofString());
        System.out.println("  Secret \'oauth_token\' stored via Conductor Secrets API");

        String workflowId = client.startWorkflow("slo_social_login", 1,
                Map.of("provider", "google", "email", "grace@example.com"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + workflow.getOutput());

        client.stopWorkers();
        System.out.println("\nResult: " + ("COMPLETED".equals(status) ? "PASSED" : "FAILED"));
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
