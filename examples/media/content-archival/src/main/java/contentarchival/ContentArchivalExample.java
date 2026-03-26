package contentarchival;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import contentarchival.workers.IdentifyContentWorker;
import contentarchival.workers.CompressWorker;
import contentarchival.workers.StoreColdWorker;
import contentarchival.workers.IndexArchiveWorker;
import contentarchival.workers.VerifyIntegrityWorker;
import java.util.List;
import java.util.Map;
public class ContentArchivalExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 528: Content Archival ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("car_identify_content", "car_compress", "car_store_cold", "car_index_archive", "car_verify_integrity"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new IdentifyContentWorker(), new CompressWorker(), new StoreColdWorker(), new IndexArchiveWorker(), new VerifyIntegrityWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("content_archival_workflow", 1, Map.of("archiveJobId", "ARC-528-2026-03", "ageThresholdDays", 180, "contentCategory", "blog_posts"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
