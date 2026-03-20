package resourceallocation;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import resourceallocation.workers.*;
import java.util.List; import java.util.Map;
public class ResourceAllocationExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 625: Resource Allocation ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("ral_assess_demand","ral_check_capacity","ral_allocate","ral_confirm"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new AssessDemandWorker(),new CheckCapacityWorker(),new AllocateWorker(),new ConfirmWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("resource_allocation_resource-allocation", 1, Map.of("projectId","PROJ-42","resourceType","developer","hoursNeeded",40));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status); System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
