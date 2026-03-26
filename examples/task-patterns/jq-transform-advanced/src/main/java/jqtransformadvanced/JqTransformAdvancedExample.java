package jqtransformadvanced;

import com.netflix.conductor.common.run.Workflow;

import java.util.List;
import java.util.Map;

/**
 * Advanced JQ Transform: Complex Data Transformations Using System Tasks
 *
 * Demonstrates chaining three JSON_JQ_TRANSFORM system tasks to perform
 * complex data transformations — no workers needed. The Conductor server
 * executes all JQ expressions directly.
 *
 * Pipeline:
 *   1. jq_flatten — Flatten nested order objects to flat records with line totals
 *   2. jq_aggregate — Group by customer, aggregate totals and order counts
 *   3. jq_classify — Classify customers into tiers (gold/silver/bronze)
 *
 * Run:
 *   java -jar target/jq-transform-advanced-1.0.0.jar
 */
public class JqTransformAdvancedExample {

    private static final String WORKFLOW_NAME = "jq_advanced_demo";
    private static final int WORKFLOW_VERSION = 1;

    /**
     * Builds sample order data for the workflow input.
     */
    static List<Map<String, Object>> buildSampleOrders() {
        return List.of(
                Map.of(
                        "id", "ORD-001",
                        "customer", Map.of("name", "Alice", "email", "alice@example.com"),
                        "items", List.of(
                                Map.of("name", "Widget", "price", 25, "qty", 4),
                                Map.of("name", "Gadget", "price", 75, "qty", 2)
                        )
                ),
                Map.of(
                        "id", "ORD-002",
                        "customer", Map.of("name", "Bob", "email", "bob@example.com"),
                        "items", List.of(
                                Map.of("name", "Widget", "price", 25, "qty", 1),
                                Map.of("name", "Sprocket", "price", 10, "qty", 3)
                        )
                ),
                Map.of(
                        "id", "ORD-003",
                        "customer", Map.of("name", "Alice", "email", "alice@example.com"),
                        "items", List.of(
                                Map.of("name", "Gadget", "price", 75, "qty", 3),
                                Map.of("name", "Doohickey", "price", 50, "qty", 1)
                        )
                ),
                Map.of(
                        "id", "ORD-004",
                        "customer", Map.of("name", "Carol", "email", "carol@example.com"),
                        "items", List.of(
                                Map.of("name", "Widget", "price", 25, "qty", 10),
                                Map.of("name", "Gadget", "price", 75, "qty", 4),
                                Map.of("name", "Sprocket", "price", 10, "qty", 20)
                        )
                )
        );
    }

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Advanced JQ Transform: Complex Data Transformations ===\n");

        System.out.println("KEY CONCEPT: Chain multiple JSON_JQ_TRANSFORM system tasks to build");
        System.out.println("a data transformation pipeline. No workers needed — the Conductor");
        System.out.println("server executes all JQ expressions directly.\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register workflow (no task defs needed for system tasks)
        System.out.println("Step 1: Registering workflow '" + WORKFLOW_NAME + "'...");
        System.out.println("  (No task definitions needed — JSON_JQ_TRANSFORM is a system task)");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        if (workersOnly) {
            System.out.println("Worker-only mode: No workers to start!");
            System.out.println("JSON_JQ_TRANSFORM tasks don't use workers — they execute on the Conductor server.");
            System.out.println("Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        // Step 2 — Build sample input and start workflow
        List<Map<String, Object>> orders = buildSampleOrders();
        System.out.println("Step 2: Starting workflow with " + orders.size() + " orders...\n");

        System.out.println("  Sample orders:");
        for (Map<String, Object> order : orders) {
            @SuppressWarnings("unchecked")
            Map<String, Object> customer = (Map<String, Object>) order.get("customer");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) order.get("items");
            System.out.println("    " + order.get("id") + " - " + customer.get("name")
                    + " (" + items.size() + " items)");
        }
        System.out.println();

        String workflowId = client.startWorkflow(WORKFLOW_NAME, WORKFLOW_VERSION,
                Map.of("orders", orders));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 3 — Wait for completion (should be near-instant for system tasks)
        System.out.println("Step 3: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n");

        if ("COMPLETED".equals(status)) {
            Map<String, Object> output = workflow.getOutput();

            System.out.println("Step 4: Results\n");
            System.out.println("  Flattened Orders: " + output.get("flattenedOrders"));
            System.out.println();
            System.out.println("  Customer Summary: " + output.get("customerSummary"));
            System.out.println();
            System.out.println("  Tiered Customers: " + output.get("tieredCustomers"));
            System.out.println();
        } else {
            System.out.println("  FAILED — check Conductor UI for details\n");
        }

        // Summary
        System.out.println("=== Summary ===");
        System.out.println("Pipeline: jq_flatten -> jq_aggregate -> jq_classify");
        System.out.println("Tasks executed: 3 (all JSON_JQ_TRANSFORM system tasks)");
        System.out.println("Workers used: 0");
        System.out.println("Orders processed: " + orders.size());

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
