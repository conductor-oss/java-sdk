package redisintegration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import redisintegration.workers.RedisConnectWorker;
import redisintegration.workers.GetSetWorker;
import redisintegration.workers.PubSubWorker;
import redisintegration.workers.CacheMgmtWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 445: Redis Integration
 *
 * Performs a Redis integration workflow:
 * connect -> get/set operations -> pub/sub -> cache management.
 *
 * Run:
 *   java -jar target/redis-integration-1.0.0.jar
 */
public class RedisIntegrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 445: Redis Integration ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "red_connect", "red_get_set", "red_pub_sub", "red_cache_mgmt"));
        System.out.println("  Registered: red_connect, red_get_set, red_pub_sub, red_cache_mgmt\n");

        System.out.println("Step 2: Registering workflow \'redis_integration_445\'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new RedisConnectWorker(),
                new GetSetWorker(),
                new PubSubWorker(),
                new CacheMgmtWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("redis_integration_445", 1,
                Map.of("redisHost", "redis://localhost:6481",
                        "cacheKey", "user:session:abc123",
                        "cacheValue", "session-data",
                        "channel", "session-updates"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Connected: " + workflow.getOutput().get("connected"));
        System.out.println("  Cached Value: " + workflow.getOutput().get("cachedValue"));
        System.out.println("  Published: " + workflow.getOutput().get("published"));
        System.out.println("  TTL: " + workflow.getOutput().get("ttl") + "s");

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
