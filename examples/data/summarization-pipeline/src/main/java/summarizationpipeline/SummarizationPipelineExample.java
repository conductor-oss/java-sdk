package summarizationpipeline;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import summarizationpipeline.workers.*;
import java.util.List;
import java.util.Map;
public class SummarizationPipelineExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 636: Summarization Pipeline ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("sum_extract_sections", "sum_compress", "sum_generate_summary"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ExtractSectionsWorker(), new CompressWorker(), new GenerateSummaryWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("sum_summarization_pipeline", 1, Map.of("document", "Full research paper text...", "maxLength", 100));
        System.out.println("  Workflow ID: " + wfId);
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
