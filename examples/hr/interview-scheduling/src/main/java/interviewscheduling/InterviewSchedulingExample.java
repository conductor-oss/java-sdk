package interviewscheduling;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import interviewscheduling.workers.*;
import java.util.List; import java.util.Map;
public class InterviewSchedulingExample {
    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);
        System.out.println("=== Example 603: Interview Scheduling ===\n");
        var client = new ConductorClientHelper();
        client.registerTaskDefs(List.of("ivs_availability","ivs_schedule","ivs_invite","ivs_confirm","ivs_remind"));
        client.registerWorkflow("workflow.json");
        client.startWorkers(List.of(new AvailabilityWorker(),new ScheduleWorker(),new InviteWorker(),new ConfirmWorker(),new RemindWorker()));
        if (workersOnly) { Thread.currentThread().join(); return; }
        Thread.sleep(2000);
        String wfId = client.startWorkflow("ivs_interview_scheduling", 1,
                Map.of("candidateName","Alex Rivera","interviewers",3,"role","Senior Engineer"));
        Workflow wf = client.waitForWorkflow(wfId, "COMPLETED", 60000);
        String status = wf.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Interview ID: " + wf.getOutput().get("interviewId"));
        System.out.println("  Scheduled: " + wf.getOutput().get("scheduledTime"));
        client.stopWorkers();
        System.out.println("COMPLETED".equals(status) ? "\nResult: PASSED" : "\nResult: FAILED");
        System.exit("COMPLETED".equals(status) ? 0 : 1);
    }
}
