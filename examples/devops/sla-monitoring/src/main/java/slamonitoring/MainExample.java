package slamonitoring;

import com.netflix.conductor.client.worker.Worker;
import slamonitoring.workers.CollectSlisWorker;
import slamonitoring.workers.CalculateBudgetWorker;
import slamonitoring.workers.EvaluateComplianceWorker;
import slamonitoring.workers.ReportWorker;

import java.util.List;
import java.util.Map;

/**
 * Example 339: SLA Monitoring — Service Level Agreement Tracking
 */
public class MainExample {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 339: SLA Monitoring ===\n");

        ConductorClientHelper helper = new ConductorClientHelper();

        helper.registerTaskDefs(List.of(
                "sla_collect_slis",
                "sla_calculate_budget",
                "sla_evaluate_compliance",
                "sla_report"
        ));

        helper.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(
                new CollectSlisWorker(),
                new CalculateBudgetWorker(),
                new EvaluateComplianceWorker(),
                new ReportWorker()
        );
        helper.startWorkers(workers);

        String workflowId = helper.startWorkflow("sla_monitoring_workflow", 1, Map.of(
                "service", "payment-api",
                "sloTarget", 99.9
        ));

        var execution = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);

        System.out.println("  collect_slisResult: " + execution.getOutput().get("collect_slisResult"));
        System.out.println("  reportResult: " + execution.getOutput().get("reportResult"));

        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
