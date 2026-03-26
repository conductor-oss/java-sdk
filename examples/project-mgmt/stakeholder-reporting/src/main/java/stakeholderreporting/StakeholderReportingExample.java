package stakeholderreporting;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import stakeholderreporting.workers.*;
import java.util.List; import java.util.Map;
public class StakeholderReportingExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example stakeholder-reporting: Stakeholder Reporting ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("shr_collect_updates","shr_aggregate","shr_format","shr_distribute"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new CollectUpdatesWorker(),new AggregateWorker(),new FormatWorker(),new DistributeWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("stakeholder_reporting_727", 1, Map.of("projectId","PROJ-42","reportPeriod","2026-W10"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status); System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
