package logisticsoptimization;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import logisticsoptimization.workers.*;
import java.util.*;
import java.util.stream.*;

public class LogisticsOptimizationExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 658: Logistics Optimization ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("lo_analyze_demand","lo_optimize_routes","lo_schedule","lo_dispatch"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new AnalyzeDemandWorker(), new OptimizeRoutesWorker(), new ScheduleWorker(), new DispatchWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        List<Map<String, Object>> orders = IntStream.rangeClosed(1, 40)
                .mapToObj(i -> Map.<String, Object>of("id", "ORD-" + i, "zip", "600" + (i % 10) + "0"))
                .collect(Collectors.toList());
        String wfId = client.startWorkflow("lo_logistics_optimization", 1,
                Map.of("region", "midwest", "date", "2024-03-15", "orders", orders));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
