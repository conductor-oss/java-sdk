package timebasedtriggers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import timebasedtriggers.workers.*;

import java.util.List;
import java.util.Map;

public class TimeBasedTriggersExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 405: Time-Based Triggers ===\n");
        var client = new ConductorClientHelper();

        client.registerTaskDefs(List.of("tb_check_time", "tb_morning_job", "tb_afternoon_job", "tb_evening_job"));
        client.registerWorkflow("workflow.json");

        List<Worker> workers = List.of(new CheckTimeWorker(), new MorningJobWorker(), new AfternoonJobWorker(), new EveningJobWorker());
        client.startWorkers(workers);
        System.out.println("  4 workers polling.\n");

        if (workersOnly) { System.out.println("Worker-only mode. Ctrl+C to stop."); Thread.currentThread().join(); return; }
        Thread.sleep(2000);

        String wfId = client.startWorkflow("time_based_triggers_405", 1, Map.of("timezone", "America/New_York", "currentHour", 9));
        System.out.println("  Workflow ID: " + wfId + "\n");
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        System.out.println("  Status: " + wf.getStatus().name());
        System.out.println("  Output: " + wf.getOutput());
        client.stopWorkers();
        System.out.println("COMPLETED".equals(wf.getStatus().name()) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(wf.getStatus().name()) ? 0 : 1);
    }
}
