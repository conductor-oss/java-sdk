package labresults;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import labresults.workers.*;
import java.util.List;
import java.util.Map;

/** Example 474: Lab Results */
public class LabResultsExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 474: Lab Results ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("lab_collect_sample", "lab_process", "lab_analyze", "lab_report", "lab_notify"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectSampleWorker(), new ProcessSampleWorker(), new AnalyzeSampleWorker(), new LabReportWorker(), new LabNotifyWorker());
        client.startWorkers(workers);
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("lab_results_workflow", 1, Map.of("orderId", "LAB-ORD-5501", "patientId", "PAT-10234", "testType", "comprehensive_metabolic_panel"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 30000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
