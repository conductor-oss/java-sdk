package textclassification;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import textclassification.workers.*;
import java.util.List;
import java.util.Map;
public class TextClassificationExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 634: Text Classification ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("txc_preprocess", "txc_extract_features", "txc_classify", "txc_confidence"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new PreprocessWorker(), new ExtractFeaturesWorker(), new ClassifyWorker(), new ConfidenceWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("txc_text_classification", 1,
                Map.of("text", "Researchers developed a new deep neural network for NLU", "categories", List.of("technology", "science", "business", "sports")));
        System.out.println("  Workflow ID: " + wfId);
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
