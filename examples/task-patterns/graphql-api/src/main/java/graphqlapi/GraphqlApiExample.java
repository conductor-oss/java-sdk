package graphqlapi;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import graphqlapi.workers.GraphqlTaskWorker;

import java.util.List;
import java.util.Map;

/**
 * GraphQL API — Query Conductor data using GraphQL (Orkes Conductor feature)
 *
 * Demonstrates REST vs GraphQL query patterns for Conductor.
 * Registers a simple workflow with a single task, runs it, then
 * shows how the same data can be queried via REST and GraphQL.
 *
 * Run:
 *   java -jar target/graphql-api-1.0.0.jar
 *   java -jar target/graphql-api-1.0.0.jar --workers
 */
public class GraphqlApiExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== GraphQL API: REST vs GraphQL Query Patterns ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of("gql_task"));
        System.out.println("  Registered: gql_task\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'graphql_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(new GraphqlTaskWorker());
        client.startWorkers(workers);
        System.out.println("  1 worker polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("graphql_demo", 1,
                Map.of("project", "conductor-app", "env", "staging"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        // Step 6 — Show REST vs GraphQL comparison
        System.out.println("\n--- REST vs GraphQL Query Comparison ---\n");

        System.out.println("REST approach (multiple requests):");
        System.out.println("  GET /api/workflow/" + workflowId + "           -> full workflow object");
        System.out.println("  GET /api/workflow/" + workflowId + "/tasks     -> all task details");
        System.out.println("  (returns all fields, even ones you don't need)\n");

        System.out.println("GraphQL approach (single request, exact fields):");
        System.out.println("  POST /api/graphql");
        System.out.println("  query {");
        System.out.println("    getWorkflow(id: \"" + workflowId + "\") {");
        System.out.println("      status");
        System.out.println("      output {");
        System.out.println("        project");
        System.out.println("        result");
        System.out.println("      }");
        System.out.println("      tasks {");
        System.out.println("        referenceTaskName");
        System.out.println("        status");
        System.out.println("      }");
        System.out.println("    }");
        System.out.println("  }");
        System.out.println("  (returns only the fields you request)\n");

        System.out.println("Benefits of GraphQL:");
        System.out.println("  - Single request instead of multiple REST calls");
        System.out.println("  - Request only the fields you need (no over-fetching)");
        System.out.println("  - Strongly typed schema with introspection");
        System.out.println("  - Combine multiple queries in one request");

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
