package datacatalog;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import datacatalog.workers.DiscoverAssetsWorker;
import datacatalog.workers.ClassifyDataWorker;
import datacatalog.workers.TagMetadataWorker;
import datacatalog.workers.IndexCatalogWorker;

import java.util.List;
import java.util.Map;

/**
 * Data Catalog Demo
 *
 * Sequential catalog pipeline:
 *   cg_discover_assets -> cg_classify_data -> cg_tag_metadata -> cg_index_catalog
 *
 * Run:
 *   java -jar target/data-catalog-1.0.0.jar
 */
public class DataCatalogExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== Data Catalog Demo ===\n");

        var client = new ConductorClientHelper();

        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "cg_discover_assets", "cg_classify_data",
                "cg_tag_metadata", "cg_index_catalog"));
        System.out.println("  Registered: cg_discover_assets, cg_classify_data, cg_tag_metadata, cg_index_catalog\n");

        System.out.println("Step 2: Registering workflow 'data_catalog'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new DiscoverAssetsWorker(),
                new ClassifyDataWorker(),
                new TagMetadataWorker(),
                new IndexCatalogWorker()
        );
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("data_catalog", 1,
                Map.of("dataSource", Map.of("host", "db.internal", "database", "production"),
                        "scanDepth", "full",
                        "classificationRules", List.of("pii_detection", "sensitivity_scoring")));
        System.out.println("  Workflow ID: " + workflowId + "\n");

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
