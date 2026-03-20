package threatintelligence;

import com.netflix.conductor.client.worker.Worker;
import threatintelligence.workers.IngestFeedsWorker;
import threatintelligence.workers.CorrelateIocsWorker;
import threatintelligence.workers.EnrichContextWorker;
import threatintelligence.workers.DistributeIntelWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 351: Threat Intelligence — Automated Threat Feed Processing
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 351: Threat Intelligence ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "ti_ingest_feeds",
                "ti_correlate_iocs",
                "ti_enrich_context",
                "ti_distribute_intel"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new IngestFeedsWorker(),
                new CorrelateIocsWorker(),
                new EnrichContextWorker(),
                new DistributeIntelWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("threat_intelligence_workflow", 1, Map.of(
                "feedSources", "OSINT",
                "lookbackHours", 24
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  ingest_feedsResult: " + execution.getOutput().get("ingest_feedsResult"));
        System.out.println("  distribute_intelResult: " + execution.getOutput().get("distribute_intelResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
