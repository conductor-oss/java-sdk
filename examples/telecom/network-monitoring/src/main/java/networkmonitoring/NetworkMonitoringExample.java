package networkmonitoring;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import networkmonitoring.workers.MonitorWorker;
import networkmonitoring.workers.DetectIssuesWorker;
import networkmonitoring.workers.DiagnoseWorker;
import networkmonitoring.workers.RepairWorker;
import networkmonitoring.workers.VerifyWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 789: Network Monitoring
 */
public class NetworkMonitoringExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 789: Network Monitoring ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("nmn_monitor", "nmn_detect_issues", "nmn_diagnose", "nmn_repair", "nmn_verify"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new MonitorWorker(), new DetectIssuesWorker(), new DiagnoseWorker(), new RepairWorker(), new VerifyWorker());
        helper.startWorkers(workers);
        try {
            String workflowId = helper.startWorkflow("nmn_network_monitoring", 1, Map.of("networkSegment", "SEG-NE-07", "checkType", "performance"));
            Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
            System.out.printf("%nWorkflow status: %s%n", workflow.getStatus());
            System.out.printf("repaired: %s%n", workflow.getOutput().get("repaired"));
            System.out.println("\nResult: PASSED");
        } finally { helper.stopWorkers(); }
    }
}
