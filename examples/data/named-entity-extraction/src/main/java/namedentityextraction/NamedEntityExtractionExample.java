package namedentityextraction;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import namedentityextraction.workers.*;
import java.util.List;
import java.util.Map;
public class NamedEntityExtractionExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 635: Named Entity Extraction ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("ner_tokenize", "ner_tag", "ner_extract_entities", "ner_link"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new TokenizeWorker(), new TagWorker(), new ExtractEntitiesWorker(), new LinkWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("ner_named_entity_extraction", 1,
                Map.of("text", "Apple Inc. announced that Tim Cook will speak in Cupertino, California next week."));
        System.out.println("  Workflow ID: " + wfId);
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
