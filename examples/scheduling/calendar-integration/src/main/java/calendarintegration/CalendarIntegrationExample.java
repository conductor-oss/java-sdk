package calendarintegration;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import calendarintegration.workers.*;
import java.util.List;
import java.util.Map;

public class CalendarIntegrationExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 406: Calendar Integration ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("cal_fetch_events", "cal_compare_schedules", "cal_sync_changes", "cal_notify_stakeholders"));
        client.registerWorkflow("workflow.json");
        List<Worker> workers = List.of(new FetchEventsWorker(), new CompareSchedulesWorker(), new SyncChangesWorker(), new NotifyStakeholdersWorker());
        client.startWorkers(workers);
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("calendar_integration_406", 1, Map.of("calendarId", "team-engineering@example.com", "syncWindow", "2026-03-08/2026-03-15", "direction", "bidirectional"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
