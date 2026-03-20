package dependencymapping;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import dependencymapping.workers.*;
import java.util.List;
import java.util.Map;

public class DependencyMappingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 426: Dependency Mapping ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("dep_discover_services", "dep_trace_calls", "dep_build_graph", "dep_visualize"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new DiscoverServicesWorker(), new TraceCallsWorker(), new BuildGraphWorker(), new VisualizeWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("dependency_mapping_426", 1, Map.of("environment","production","namespace","ecommerce"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
