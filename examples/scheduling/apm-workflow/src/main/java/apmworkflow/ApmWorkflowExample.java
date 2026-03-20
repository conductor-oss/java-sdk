package apmworkflow;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import apmworkflow.workers.*;
import java.util.List;
import java.util.Map;

public class ApmWorkflowExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 421: APM Workflow ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("apm_collect_traces", "apm_analyze_latency", "apm_detect_bottlenecks", "apm_report"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CollectTracesWorker(), new AnalyzeLatencyWorker(), new DetectBottlenecksWorker(), new ApmReportWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("apm_workflow_421", 1, Map.of("serviceName","checkout-service","timeRange","last-24h","percentile","p99"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
