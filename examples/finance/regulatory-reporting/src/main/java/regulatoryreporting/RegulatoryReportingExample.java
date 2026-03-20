package regulatoryreporting;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import regulatoryreporting.workers.*;
import java.util.List;
import java.util.Map;

public class RegulatoryReportingExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 497: Regulatory Reporting ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("reg_collect_data", "reg_validate", "reg_format", "reg_submit", "reg_confirm"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectDataWorker(), new ValidateWorker(), new FormatWorker(), new SubmitWorker(), new ConfirmWorker());
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("regulatory_reporting_workflow", 1, Map.of("reportId", "REG-2024-Q1-CALL", "reportType", "CALL", "reportingPeriod", "2024-Q1", "entity", "First National Bank"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
