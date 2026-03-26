package predictivemaintenance;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import predictivemaintenance.workers.CollectDataWorker;
import predictivemaintenance.workers.AnalyzeTrendsWorker;
import predictivemaintenance.workers.PredictFailureWorker;
import predictivemaintenance.workers.ScheduleMaintenanceWorker;
import java.util.List;
import java.util.Map;
public class PredictiveMaintenanceExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 538: Predictive Maintenance ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("pmn_collect_data", "pmn_analyze_trends", "pmn_predict_failure", "pmn_schedule_maintenance"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectDataWorker(), new AnalyzeTrendsWorker(), new PredictFailureWorker(), new ScheduleMaintenanceWorker());
        client.startWorkers(workers);
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("predictive_maintenance_workflow", 1, Map.of("assetId", "PUMP-538-A", "assetType", "hydraulic_pump", "siteId", "SITE-FACTORY-02"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String s = wf.getStatus().name();
        System.out.println("  Status: " + s + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(s)?"\nResult: PASSED":"\nResult: FAILED");
        System.exit("COMPLETED".equals(s)?0:1);
    }
}
