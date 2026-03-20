package capacitymgmttelecom;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import capacitymgmttelecom.workers.MonitorWorker;
import capacitymgmttelecom.workers.ForecastWorker;
import capacitymgmttelecom.workers.PlanWorker;
import capacitymgmttelecom.workers.ProvisionWorker;
import capacitymgmttelecom.workers.VerifyWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 817: Capacity Management (Telecom)
 */
public class CapacityMgmtTelecomExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 817: Capacity Management (Telecom) ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("cmt_monitor", "cmt_forecast", "cmt_plan", "cmt_provision", "cmt_verify"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new MonitorWorker(), new ForecastWorker(), new PlanWorker(), new ProvisionWorker(), new VerifyWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("cmt_capacity_mgmt_telecom", 1, Map.of("region", "METRO-NE-3", "networkType", "5G"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("newCapacity: %s%n", workflow.getOutput().get("newCapacity"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
