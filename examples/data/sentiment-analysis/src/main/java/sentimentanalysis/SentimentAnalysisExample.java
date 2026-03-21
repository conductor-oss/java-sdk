package sentimentanalysis;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import sentimentanalysis.workers.*;
import java.util.List;
import java.util.Map;
public class SentimentAnalysisExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 633: Sentiment Analysis ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("snt_preprocess", "snt_analyze", "snt_classify", "snt_aggregate"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new PreprocessWorker(), new AnalyzeWorker(), new ClassifyWorker(), new AggregateWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("snt_sentiment_analysis", 1,
                Map.of("texts", List.of("Great product!", "Terrible support", "It works"), "source", "app_reviews"));
        System.out.println("  Workflow ID: " + wfId);
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
