package documentreview;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import documentreview.workers.*;
import java.util.List;
import java.util.Map;
public class DocumentReviewExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 692: Document Review ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("drv_ingest","drv_classify","drv_review","drv_privilege","drv_produce"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new IngestWorker(),new ClassifyWorker(),new ReviewWorker(),new PrivilegeWorker(),new ProduceWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("drv_document_review",1,Map.of("matterId","MAT-200","documentBatch","batch-Q1-2024"));
        Workflow wf = client.waitForWorkflow(wfId,"COMPLETED",60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: "+status+"\n  Produced: "+wf.getOutput().get("produced")+"\n  Withheld: "+wf.getOutput().get("withheld"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(status)?0:1);
    }
}
