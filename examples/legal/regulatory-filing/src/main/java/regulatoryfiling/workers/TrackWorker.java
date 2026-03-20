package regulatoryfiling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TrackWorker implements Worker {
    @Override public String getTaskDefName() { return "rgf_track"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [track] Tracking submission " + task.getInputData().get("submissionId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("trackingStatus", "received");
        result.getOutputData().put("estimatedProcessingDays", 15);
        return result;
    }
}
