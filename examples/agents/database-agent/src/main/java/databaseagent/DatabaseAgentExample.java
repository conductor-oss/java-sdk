package databaseagent;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import databaseagent.workers.ParseQuestionWorker;
import databaseagent.workers.GenerateQueryWorker;
import databaseagent.workers.ValidateQueryWorker;
import databaseagent.workers.ExecuteQueryWorker;
import databaseagent.workers.FormatWorker;

import java.util.List;
import java.util.Map;

/**
 * Database Agent Demo
 *
 * Demonstrates a sequential NL-to-SQL pipeline of five workers that collaborate
 * to parse a question, generate SQL, validate it, execute it, and format results:
 *   parse_question -> generate_query -> validate_query -> execute_query -> format
 *
 * Run:
 *   java -jar target/database-agent-1.0.0.jar
 */
public class DatabaseAgentExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Database Agent Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 — Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "db_parse_question", "db_generate_query", "db_validate_query",
                "db_execute_query", "db_format"));
        System.out.println("  Registered: db_parse_question, db_generate_query, db_validate_query, db_execute_query, db_format\n");

        // Step 2 — Register workflow
        System.out.println("Step 2: Registering workflow 'database_agent'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 — Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ParseQuestionWorker(),
                new GenerateQueryWorker(),
                new ValidateQueryWorker(),
                new ExecuteQueryWorker(),
                new FormatWorker()
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

        // Step 4 — Start the workflow
        System.out.println("Step 4: Starting workflow...\n");

        List<Map<String, Object>> tableDefs = List.of(
                Map.of("name", "employees",
                        "columns", List.of("id", "name", "department_id", "hire_date", "salary")),
                Map.of("name", "departments",
                        "columns", List.of("id", "name", "location", "budget")),
                Map.of("name", "sales",
                        "columns", List.of("id", "employee_id", "amount", "sale_date", "product_id")),
                Map.of("name", "products",
                        "columns", List.of("id", "name", "category", "price"))
        );

        Map<String, Object> databaseSchema = Map.of(
                "database", "company_analytics",
                "tables", tableDefs
        );

        String workflowId = client.startWorkflow("database_agent", 1,
                Map.of("question", "What are the top 5 departments by total revenue, including employee count and average sale?",
                        "databaseSchema", databaseSchema));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 — Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

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
