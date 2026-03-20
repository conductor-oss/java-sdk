package documentqa;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import documentqa.workers.*;
import java.util.List;
import java.util.Map;
public class DocumentQaExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 638: Document QA ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("dqa_ingest", "dqa_chunk", "dqa_index", "dqa_query", "dqa_answer"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new IngestWorker(), new ChunkWorker(), new IndexWorker(), new QueryWorker(), new AnswerWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("dqa_document_qa", 1, Map.of("documentUrl", "https://docs.example.com/earnings-q4.pdf", "question", "What was the total revenue in Q4?"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
