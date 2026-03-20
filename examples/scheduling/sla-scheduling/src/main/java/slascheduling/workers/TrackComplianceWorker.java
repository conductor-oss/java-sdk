package slascheduling.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TrackComplianceWorker implements Worker {
    @Override public String getTaskDefName() { return "sla_track_compliance"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [compliance] Tracking SLA compliance");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("completedOnTime", 3);
        r.getOutputData().put("breached", 0);
        r.getOutputData().put("complianceRate", "100%");
        r.getOutputData().put("averageResolutionMin", 275);
        return r;
    }
}
