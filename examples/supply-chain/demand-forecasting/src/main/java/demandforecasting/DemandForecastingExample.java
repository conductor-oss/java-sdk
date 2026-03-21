package demandforecasting;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import demandforecasting.workers.*;
import java.util.*;

public class DemandForecastingExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 659: Demand Forecasting ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("df_collect_data","df_analyze_trends","df_forecast","df_plan"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CollectDataWorker(), new AnalyzeTrendsWorker(), new ForecastWorker(), new PlanWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("df_demand_forecasting", 1,
                Map.of("productCategory","consumer-electronics","horizon","6-month","region","North America"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
