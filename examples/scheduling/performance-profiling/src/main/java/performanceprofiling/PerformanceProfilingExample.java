package performanceprofiling;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import performanceprofiling.workers.*;
import java.util.List;
import java.util.Map;

public class PerformanceProfilingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 430: Performance Profiling ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("prf_instrument", "prf_collect_profile", "prf_analyze_hotspots", "prf_recommend"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new PrfInstrumentWorker(), new CollectProfileWorker(), new AnalyzeHotspotsWorker(), new RecommendWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("performance_profiling_430", 1, Map.of("serviceName","order-service","profilingDuration","60s","profileType","cpu"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
