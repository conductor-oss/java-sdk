package patentfiling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TrackWorker implements Worker {
    @Override public String getTaskDefName() { return "ptf_track"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [track] Tracking application " + task.getInputData().get("applicationNumber"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("trackingId", "TRK-" + System.currentTimeMillis());
        result.getOutputData().put("status", "pending-examination");
        return result;
    }
}
