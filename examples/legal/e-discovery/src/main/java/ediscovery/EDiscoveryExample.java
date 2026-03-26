package ediscovery;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import ediscovery.workers.*;
import java.util.List;
import java.util.Map;
public class EDiscoveryExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 693: E-Discovery ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("edc_identify","edc_collect","edc_process","edc_review","edc_produce"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new IdentifyWorker(),new CollectWorker(),new ProcessWorker(),new ReviewWorker(),new ProduceWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("edc_e_discovery",1,Map.of("matterId","MAT-300","custodians",8,"dateRange","2023-01-01 to 2024-01-01"));
        Workflow wf = client.waitForWorkflow(wfId,"COMPLETED",60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: "+status+"\n  Total collected: "+wf.getOutput().get("totalCollected")+"\n  Total produced: "+wf.getOutput().get("totalProduced"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(status)?0:1);
    }
}
