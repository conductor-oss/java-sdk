package lastmiledelivery;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import lastmiledelivery.workers.*;
import java.util.*;
public class LastMileDeliveryExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 668: Last Mile Delivery ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("lmd_assign_driver","lmd_optimize_route","lmd_deliver","lmd_confirm"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new AssignDriverWorker(), new OptimizeRouteWorker(), new DeliverWorker(), new ConfirmWorker()));
        if (args.length > 0 && "--workers".equals(args[0])) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("lmd_last_mile_delivery", 1,
                Map.of("orderId","ORD-2024-668","address","742 Evergreen Terrace, Springfield","timeWindow","2pm-4pm"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name() + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
