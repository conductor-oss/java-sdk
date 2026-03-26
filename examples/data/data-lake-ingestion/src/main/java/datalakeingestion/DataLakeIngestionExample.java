package datalakeingestion;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import datalakeingestion.workers.*;

import java.util.List;
import java.util.Map;

public class DataLakeIngestionExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Data Lake Ingestion Demo ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("li_validate_schema", "li_partition_by_date", "li_convert_format", "li_write_to_lake", "li_update_catalog"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ValidateSchemaWorker(), new PartitionByDateWorker(), new ConvertFormatWorker(), new WriteToLakeWorker(), new UpdateCatalogWorker());
        client.startWorkers(workers);
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("data_lake_ingestion", 1, Map.of(
                "records", List.of(
                        Map.of("user_id", "U-1", "event_type", "click", "event_date", "2025-01-15T10:30:00Z", "page", "/home"),
                        Map.of("user_id", "U-2", "event_type", "purchase", "event_date", "2025-01-15T11:00:00Z", "page", "/checkout"),
                        Map.of("user_id", "U-1", "event_type", "click", "event_date", "2025-01-16T09:15:00Z", "page", "/products")),
                "lakePath", "s3://data-lake/events", "format", "parquet"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
    }
}
