package productcatalog;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import productcatalog.workers.*;

import java.util.List;
import java.util.Map;

/**
 * Example 451: Product Catalog
 *
 * Performs a product catalog management workflow:
 * add product -> validate -> enrich metadata -> publish -> index for search.
 *
 * Run:
 *   java -jar target/product-catalog-1.0.0.jar
 */
public class ProductCatalogExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Example 451: Product Catalog ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "prd_add_product", "prd_validate", "prd_enrich", "prd_publish", "prd_index"));
        System.out.println("  Registered 5 tasks.\n");

        System.out.println("Step 2: Registering workflow 'product_catalog'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new AddProductWorker(),
                new ValidateProductWorker(),
                new EnrichProductWorker(),
                new PublishProductWorker(),
                new IndexProductWorker());
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("product_catalog", 1, Map.of(
                "sku", "WH-1000XM5",
                "name", "Wireless Noise Cancelling Headphones",
                "price", 349.99,
                "category", "Electronics",
                "description", "Premium over-ear headphones with industry-leading noise cancellation."));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 30000);
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
