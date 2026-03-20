package inlinetasks;

import com.netflix.conductor.common.run.Workflow;

import java.util.List;
import java.util.Map;

/**
 * INLINE Tasks — JavaScript That Runs on the Conductor Server
 *
 * Demonstrates 4 INLINE task patterns — all logic executes as JavaScript
 * on the Conductor server, requiring zero workers:
 *   1. Math/aggregation: sum, average, min, max of a number array
 *   2. String manipulation: uppercase, word count, slug, reversed
 *   3. Conditional logic: tiering based on average, flags
 *   4. Response building: combine all results into a single output
 *
 * Run:
 *   java -jar target/inline-tasks-1.0.0.jar
 */
public class InlineTasksExample {

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== INLINE Tasks: Server-Side JavaScript Without Workers ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register workflow (no task defs needed for INLINE tasks)
        System.out.println("Step 1: Registering workflow 'inline_tasks_demo'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered (4 INLINE tasks, 0 workers needed).\n");

        if (workersOnly) {
            System.out.println("Running in --workers mode.");
            System.out.println("No workers to start — all tasks are INLINE (server-side JavaScript).");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        // Step 2 — Start the workflow
        System.out.println("Step 2: Starting workflow with sample data...");
        Map<String, Object> input = Map.of(
                "numbers", List.of(85, 92, 78, 95, 88, 72, 91),
                "text", "Conductor makes workflow orchestration simple",
                "config", Map.of("enabled", true)
        );
        System.out.println("  Numbers: " + input.get("numbers"));
        System.out.println("  Text:    \"" + input.get("text") + "\"");
        System.out.println("  Config:  " + input.get("config") + "\n");

        String workflowId = client.startWorkflow("inline_tasks_demo", 1, input);
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 3 — Wait for completion
        System.out.println("Step 3: Waiting for completion (no workers polling — server executes JS)...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status + "\n");

        if ("COMPLETED".equals(status)) {
            Map<String, Object> output = workflow.getOutput();

            // Math results
            System.out.println("--- Math Aggregation ---");
            Map<String, Object> math = (Map<String, Object>) output.get("math");
            if (math != null) {
                System.out.println("  Sum:     " + math.get("sum"));
                System.out.println("  Average: " + math.get("average"));
                System.out.println("  Min:     " + math.get("min"));
                System.out.println("  Max:     " + math.get("max"));
                System.out.println("  Count:   " + math.get("count"));
            }

            // Text results
            System.out.println("\n--- String Manipulation ---");
            Map<String, Object> text = (Map<String, Object>) output.get("text");
            if (text != null) {
                System.out.println("  Uppercase:  " + text.get("uppercase"));
                System.out.println("  Word Count: " + text.get("wordCount"));
                System.out.println("  Slug:       " + text.get("slug"));
                System.out.println("  Reversed:   " + text.get("reversed"));
            }

            // Classification results
            System.out.println("\n--- Classification ---");
            Map<String, Object> classification = (Map<String, Object>) output.get("classification");
            if (classification != null) {
                System.out.println("  Tier:            " + classification.get("tier"));
                System.out.println("  High Performer:  " + classification.get("isHighPerformer"));
                System.out.println("  Enabled:         " + classification.get("isEnabled"));
            }

            // Summary
            System.out.println("\n--- Summary ---");
            System.out.println("  " + output.get("summary"));

            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("Workflow did not complete successfully.");
            System.out.println("Output: " + workflow.getOutput());
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
