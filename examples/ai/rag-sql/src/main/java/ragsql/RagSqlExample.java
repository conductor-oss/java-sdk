package ragsql;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ragsql.workers.ParseNlWorker;
import ragsql.workers.GenerateSqlWorker;
import ragsql.workers.ValidateSqlWorker;
import ragsql.workers.ExecuteSqlWorker;
import ragsql.workers.FormatResultsWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 154: SQL RAG — Natural Language to SQL Query
 *
 * Parses natural-language questions, generates SQL, validates for safety,
 * executes the query, and formats results into a human-readable answer.
 *
 * Run:
 *   java -jar target/rag-sql-1.0.0.jar
 *   java -jar target/rag-sql-1.0.0.jar --workers
 */
public class RagSqlExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 154: SQL RAG ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "sq_parse_nl", "sq_generate_sql",
                "sq_validate_sql", "sq_execute_sql", "sq_format_results"));
        System.out.println("  Registered: sq_parse_nl, sq_generate_sql, sq_validate_sql, sq_execute_sql, sq_format_results\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'sql_rag_workflow'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ParseNlWorker(),
                new GenerateSqlWorker(),
                new ValidateSqlWorker(),
                new ExecuteSqlWorker(),
                new FormatResultsWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 — Run SQL RAG query
        System.out.println("Step 4: Starting workflow — SQL RAG query...\n");
        String workflowId = client.startWorkflow("sql_rag_workflow", 1,
                Map.of("question", "What are the most executed workflows in the last 7 days?",
                        "dbSchema", "workflow_executions(id, workflow_name, status, duration_sec, created_at)"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("  Waiting for completion...");
        Workflow wf = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Answer: " + wf.getOutput().get("answer") + "\n");

        System.out.println("--- SQL RAG Pattern ---");
        System.out.println("  - Parse NL: Extract intent and entities from natural language");
        System.out.println("  - Generate SQL: Build a SQL query from parsed entities");
        System.out.println("  - Validate SQL: Check for dangerous operations");
        System.out.println("  - Execute SQL: Run the query and fetch results");
        System.out.println("  - Format Results: Present results in natural language");

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
