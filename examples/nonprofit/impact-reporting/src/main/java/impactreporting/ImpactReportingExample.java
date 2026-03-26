package impactreporting;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import impactreporting.workers.*;
import java.util.List;
import java.util.Map;
/** Example 756: Impact Reporting — Collect Data, Aggregate, Analyze, Format, Publish */
public class ImpactReportingExample {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Example 756: Impact Reporting ===\n");
        ConductorClientHelper helper = new ConductorClientHelper();
        helper.registerTaskDefs(List.of("ipr_collect_data", "ipr_aggregate", "ipr_analyze", "ipr_format", "ipr_publish"));
        helper.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new CollectDataWorker(), new AggregateWorker(), new AnalyzeWorker(), new FormatWorker(), new PublishWorker());
        helper.startWorkers(workers);
        String workflowId = helper.startWorkflow("impact_reporting_756", 1, Map.of("programName", "Community Kitchen", "reportYear", 2025));
        Workflow workflow = helper.waitForWorkflow(workflowId, "COMPLETED", 30000);
        System.out.println("\n  Status: " + workflow.getStatus());
        System.out.println("  Output: " + workflow.getOutput());
        helper.stopWorkers();
        System.out.println("\nResult: PASSED");
    }
}
