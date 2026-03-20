package healthdashboard;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import healthdashboard.workers.*;
import java.util.List;
import java.util.Map;

public class HealthDashboardExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 417: Health Dashboard ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("hd_check_api","hd_check_db","hd_check_cache","hd_render_dashboard"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CheckApiWorker(), new CheckDbWorker(), new CheckCacheWorker(), new RenderDashboardWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("health_dashboard_417", 1, Map.of("environment","production"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
