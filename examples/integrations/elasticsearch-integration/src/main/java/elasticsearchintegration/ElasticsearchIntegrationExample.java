package elasticsearchintegration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import elasticsearchintegration.workers.IndexDocWorker;
import elasticsearchintegration.workers.SearchWorker;
import elasticsearchintegration.workers.AggregateWorker;
import elasticsearchintegration.workers.AnalyzeWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 446: Elasticsearch Integration
 *
 * Runs an Elasticsearch integration workflow:
 * index document -> search -> aggregate -> analyze results.
 *
 * Run:
 *   java -jar target/elasticsearch-integration-1.0.0.jar
 */
public class ElasticsearchIntegrationExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 446: Elasticsearch Integration ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "els_index_doc", "els_search", "els_aggregate", "els_analyze"));
        System.out.println("  Registered: els_index_doc, els_search, els_aggregate, els_analyze\n");

        System.out.println("Step 2: Registering workflow \'elasticsearch_integration_446\'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new IndexDocWorker(),
                new SearchWorker(),
                new AggregateWorker(),
                new AnalyzeWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("elasticsearch_integration_446", 1,
                Map.of("indexName", "reports-2024",
                        "document", Map.of("title", "Monthly Revenue Report", "category", "finance"),
                        "searchQuery", "revenue report"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Document ID: " + workflow.getOutput().get("documentId"));
        System.out.println("  Total Hits: " + workflow.getOutput().get("totalHits"));
        System.out.println("  Top Category: " + workflow.getOutput().get("topCategory"));
        System.out.println("  Insights: " + workflow.getOutput().get("insights"));

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
