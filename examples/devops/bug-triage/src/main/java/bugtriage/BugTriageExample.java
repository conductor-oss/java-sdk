package bugtriage;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import bugtriage.workers.*;
import java.util.List;
import java.util.Map;
public class BugTriageExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 642: Bug Triage ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("btg_parse_report", "btg_classify_severity", "btg_handle_critical", "btg_handle_high", "btg_handle_low", "btg_assign"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new ParseReportWorker(), new ClassifySeverityWorker(), new HandleCriticalWorker(), new HandleHighWorker(), new HandleLowWorker(), new AssignWorker());
        client.startWorkers(workers);
        if (workersOnly) { System.out.println("Worker-only mode."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("btg_bug_triage", 1,
                Map.of("bugReport", Map.of("title", "App crash on login with SSO", "description", "Application crash when users attempt SSO login with expired tokens", "component", "authentication")));
        System.out.println("  Workflow ID: " + wfId);
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status + "\n  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
